package com.ssafy.a305.be.domain.repayment.service;

import com.ssafy.a305.be.domain.contract.entity.Contract;
import com.ssafy.a305.be.domain.repayment.dto.response.RepaymentSchedulePaidResponseDto;
import com.ssafy.a305.be.domain.repayment.dto.response.RepaymentScheduleTransferResponseDto;
import com.ssafy.a305.be.domain.repayment.entity.AutoTransferRegistration;
import com.ssafy.a305.be.domain.repayment.entity.RepaymentSchedule;
import com.ssafy.a305.be.domain.repayment.repository.AutoTransferRegistrationRepository;
import com.ssafy.a305.be.domain.repayment.repository.RepaymentScheduleRepository;
import com.ssafy.a305.be.global.exception.CustomBusinessException;
import com.ssafy.a305.be.global.exception.dto.ErrorCode;
import com.ssafy.a305.be.global.external.ssafy.SsafyTransferApiClient;
import com.ssafy.a305.be.global.external.ssafy.dto.SsafyTransferResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class RepaymentManualTransferService {
    private static final Set<String> TRANSFER_ALLOWED_STATUSES = Set.of("NONPAID", "OVERDUE");
    private static final Set<String> MANUAL_PAID_ALLOWED_STATUSES = Set.of("NONPAID", "OVERDUE");
    private static final Set<String> CONTRACT_ALLOWED_STATUSES = Set.of("SIGNED", "ACTIVE", "OVERDUE");

    private final RepaymentScheduleRepository repaymentScheduleRepository;
    private final AutoTransferRegistrationRepository autoTransferRegistrationRepository;
    private final SsafyTransferApiClient ssafyTransferApiClient;

    @Transactional
    public RepaymentScheduleTransferResponseDto transferRepaymentSchedule(
            Integer contractSeq,
            Integer scheduleSeq,
            Integer userSeq
    ) {
        RepaymentSchedule schedule = repaymentScheduleRepository
                .findByScheduleSeqAndContractSeqWithContractUsers(scheduleSeq, contractSeq)
                .orElseThrow(() -> new CustomBusinessException(ErrorCode.CONTRACT_NOT_FOUND, "해당 계약의 상환 회차를 찾을 수 없습니다."));

        Contract contract = schedule.getContract();
        validateContractStatus(contract);
        if (!contract.getDebtor().getUserSeq().equals(userSeq)) {
            throw new CustomBusinessException(ErrorCode.ACCESS_DENIED, "채무자만 수동 이체를 실행할 수 있습니다.");
        }

        if (!TRANSFER_ALLOWED_STATUSES.contains(schedule.getStatus())) {
            throw new CustomBusinessException(
                    ErrorCode.INVALID_CONTRACT_STATUS,
                    "미납(NONPAID) 또는 연체(OVERDUE) 상태의 회차만 수동 이체를 실행할 수 있습니다."
            );
        }

        BigDecimal amount = schedule.getTotalDue();
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new CustomBusinessException(ErrorCode.INVALID_INPUT_VALUE, "회차의 이체 금액이 유효하지 않습니다.");
        }

        AutoTransferRegistration registration = autoTransferRegistrationRepository
                .findActiveWithAccounts(contractSeq)
                .orElseThrow(() -> new CustomBusinessException(
                        ErrorCode.BANK_ACCOUNT_NOT_FOUND,
                        "자동이체 등록 정보가 없어 수동 이체를 실행할 수 없습니다."
                ));

        String withdrawalAccountNo = registration.getDebtorAccount().getAccountNo();
        String depositAccountNo = registration.getCreditorAccount().getAccountNo();
        String debtorUserKey = contract.getDebtor().getSsafyUserKey();

        if (debtorUserKey == null || debtorUserKey.isBlank()) {
            throw new CustomBusinessException(ErrorCode.INVALID_INPUT_VALUE, "채무자의 SSAFY userKey가 없어 수동 이체를 실행할 수 없습니다.");
        }
        if (withdrawalAccountNo == null || withdrawalAccountNo.isBlank()
                || depositAccountNo == null || depositAccountNo.isBlank()) {
            throw new CustomBusinessException(ErrorCode.BANK_ACCOUNT_NOT_FOUND, "수동 이체에 필요한 계좌 정보가 없습니다.");
        }

        SsafyTransferResponse transferResponse = ssafyTransferApiClient.transfer(
                withdrawalAccountNo,
                depositAccountNo,
                amount,
                contract.getContractId(),
                debtorUserKey
        );

        schedule.markPaid(LocalDateTime.now());
        closeContractIfAllSchedulesCompleted(contract);

        return new RepaymentScheduleTransferResponseDto(
                contractSeq,
                scheduleSeq,
                contract.getContractId(),
                schedule.getStatus(),
                extractTransactionDate(transferResponse),
                withdrawalAccountNo,
                depositAccountNo
        );
    }

    @Transactional
    public RepaymentSchedulePaidResponseDto markRepaymentScheduleManualPaid(
            Integer contractSeq,
            Integer scheduleSeq,
            Integer userSeq
    ) {
        RepaymentSchedule schedule = repaymentScheduleRepository
                .findByScheduleSeqAndContractSeqWithContractUsers(scheduleSeq, contractSeq)
                .orElseThrow(() -> new CustomBusinessException(ErrorCode.CONTRACT_NOT_FOUND, "해당 계약의 상환 회차를 찾을 수 없습니다."));

        Contract contract = schedule.getContract();
        validateContractStatus(contract);
        if (!contract.getCreditor().getUserSeq().equals(userSeq)) {
            throw new CustomBusinessException(ErrorCode.ACCESS_DENIED, "채권자만 상환 처리(수동 완료)를 할 수 있습니다.");
        }

        if (!MANUAL_PAID_ALLOWED_STATUSES.contains(schedule.getStatus())) {
            throw new CustomBusinessException(
                    ErrorCode.INVALID_CONTRACT_STATUS,
                    "미납(NONPAID) 또는 연체(OVERDUE) 상태의 회차만 상환 처리할 수 있습니다."
            );
        }

        schedule.markManualPaid(LocalDateTime.now());
        closeContractIfAllSchedulesCompleted(contract);

        return new RepaymentSchedulePaidResponseDto(
                contractSeq,
                scheduleSeq,
                contract.getContractId(),
                schedule.getStatus()
        );
    }

    private String extractTransactionDate(SsafyTransferResponse response) {
        if (response == null || response.getRec() == null || response.getRec().isEmpty()) {
            return null;
        }
        SsafyTransferResponse.TransactionRecord first = response.getRec().get(0);
        return first != null ? first.getTransactionDate() : null;
    }

    private void closeContractIfAllSchedulesCompleted(Contract contract) {
        boolean allSchedulesCompleted = contract.getRepaymentSchedules().stream()
                .allMatch(item -> "PAID".equals(item.getStatus())
                        || "MANUAL_PAID".equals(item.getStatus())
                        || "WAIVED".equals(item.getStatus()));

        if (allSchedulesCompleted) {
            contract.markClosed(LocalDateTime.now());
        }
    }

    private void validateContractStatus(Contract contract) {
        if (!CONTRACT_ALLOWED_STATUSES.contains(contract.getStatus())) {
            throw new CustomBusinessException(
                    ErrorCode.INVALID_CONTRACT_STATUS,
                    "이체/상환 처리 가능한 계약 상태가 아닙니다."
            );
        }
    }
}

