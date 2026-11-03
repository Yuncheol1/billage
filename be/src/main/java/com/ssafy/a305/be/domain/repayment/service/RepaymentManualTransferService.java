package com.ssafy.a305.be.domain.repayment.service;

import com.ssafy.a305.be.domain.contract.entity.Contract;
import com.ssafy.a305.be.domain.notification.service.NotificationCommandService;
import com.ssafy.a305.be.domain.repayment.dto.response.RepaymentSchedulePaidResponseDto;
import com.ssafy.a305.be.domain.repayment.dto.response.RepaymentScheduleTransferResponseDto;
import com.ssafy.a305.be.domain.repayment.entity.AutoTransferRegistration;
import com.ssafy.a305.be.domain.repayment.entity.RepaymentSchedule;
import com.ssafy.a305.be.domain.repayment.entity.RepaymentScheduleChangeReason;
import com.ssafy.a305.be.domain.repayment.repository.AutoTransferRegistrationRepository;
import com.ssafy.a305.be.domain.repayment.repository.RepaymentScheduleRepository;
import com.ssafy.a305.be.global.exception.CustomBusinessException;
import com.ssafy.a305.be.global.exception.TransferFailException;
import com.ssafy.a305.be.global.exception.TransferOutcomeUnknownException;
import com.ssafy.a305.be.global.exception.dto.ErrorCode;
import com.ssafy.a305.be.global.external.ssafy.SsafyTransferApiClient;
import com.ssafy.a305.be.global.external.ssafy.dto.SsafyTransferResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

@Service
@Slf4j
@RequiredArgsConstructor
public class RepaymentManualTransferService {
    private static final Set<String> TRANSFER_ALLOWED_STATUSES = Set.of("NONPAID", "OVERDUE");
    private static final Set<String> MANUAL_PAID_ALLOWED_STATUSES = Set.of("NONPAID", "OVERDUE");
    private static final Set<String> CONTRACT_ALLOWED_STATUSES = Set.of("SIGNED", "ACTIVE", "OVERDUE");

    private final RepaymentScheduleRepository repaymentScheduleRepository;
    private final AutoTransferRegistrationRepository autoTransferRegistrationRepository;
    private final SsafyTransferApiClient ssafyTransferApiClient;
    private final NotificationCommandService notificationCommandService;
    private final TransactionTemplate transactionTemplate;
    private final RepaymentScheduleHistoryRecorder historyRecorder;

    /**
     * 채무자가 회차를 직접 이체한다. 자동이체와 같은 3단계 구조
     * API 호출이 트랜잭션 안에 들어가지 않도록 @Transactional을 붙이지 않는다.
     */
    public RepaymentScheduleTransferResponseDto transferRepaymentSchedule(
            Integer contractSeq,
            Integer scheduleSeq,
            Integer userSeq
    ) {
        // 1. 검증 + 선점 (Tx1)
        ManualTransferPlan plan = transactionTemplate.execute(
                status -> prepareManualTransfer(contractSeq, scheduleSeq, userSeq));

        // 2. 은행 API 호출 (트랜잭션 밖)
        SsafyTransferResponse transferResponse;
        try {
            transferResponse = ssafyTransferApiClient.transfer(
                    plan.withdrawalAccountNo(),
                    plan.depositAccountNo(),
                    plan.amount(),
                    plan.memo(),
                    plan.debtorUserKey()
            );
        } catch (TransferOutcomeUnknownException e) {
            // 돈이 나갔을 수 있다. REQUESTED로 남기고 사용자에게는 확인 중이라고 알린다.
            log.error("[수동이체] 결과 불명 - scheduleSeq={}. 회차를 REQUESTED로 남긴다. 사유={}",
                    scheduleSeq, e.getMessage());
            recordOutcomeUnknownSafely(plan, e);
            throw new CustomBusinessException(ErrorCode.DUPLICATE_REQUEST,
                    "이체 결과를 확인하는 중입니다. 다시 이체하지 말고 잠시 후 상환 내역을 확인해주세요.");
        } catch (TransferFailException e) {
            transactionTemplate.executeWithoutResult(status -> {
                if (repaymentScheduleRepository.releaseTransferClaim(scheduleSeq, plan.previousStatus()) == 1) {
                    historyRecorder.record(scheduleSeq, "REQUESTED", plan.previousStatus(),
                            RepaymentScheduleChangeReason.TRANSFER_REJECTED, plan.userSeq(), e.getMessage());
                }
            });
            throw e;
        }

        // 3. 결과 반영 (Tx2)
        try {
            return transactionTemplate.execute(status -> completeManualTransfer(plan, transferResponse));
        } catch (RuntimeException e) {
            log.error("[수동이체] 이체 성공 후 결과 반영 실패 - scheduleSeq={}. 정산 대상으로 남긴다.",
                    scheduleSeq, e);
            throw e;
        }
    }

    private ManualTransferPlan prepareManualTransfer(Integer contractSeq, Integer scheduleSeq, Integer userSeq) {
        RepaymentSchedule schedule = repaymentScheduleRepository
                .findByScheduleSeqAndContractSeqWithContractUsers(scheduleSeq, contractSeq)
                .orElseThrow(() -> new CustomBusinessException(ErrorCode.CONTRACT_NOT_FOUND, "해당 계약의 상환 회차를 찾을 수 없습니다."));

        Contract contract = schedule.getContract();
        validateContractStatus(contract);
        if (!contract.getDebtor().getUserSeq().equals(userSeq)) {
            throw new CustomBusinessException(ErrorCode.ACCESS_DENIED, "채무자만 수동 이체를 실행할 수 있습니다.");
        }

        // 이전 이체가 미확정이면 이미 나갔을 수 있다.
        if ("REQUESTED".equals(schedule.getStatus())) {
            throw new CustomBusinessException(ErrorCode.DUPLICATE_REQUEST,
                    "이전 이체의 결과를 확인하는 중입니다. 다시 이체하지 말고 잠시 후 상환 내역을 확인해주세요.");
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

        // 선점 UPDATE가 영속성 컨텍스트를 비우므로 값은 미리 꺼내 둔다. 적요는 자동이체와 같다.
        ManualTransferPlan plan = new ManualTransferPlan(
                contractSeq, scheduleSeq, userSeq, schedule.getStatus(),
                withdrawalAccountNo, depositAccountNo, amount, schedule.transferMemo(), debtorUserKey);

        int claimed = repaymentScheduleRepository.claimForTransfer(
                scheduleSeq, plan.previousStatus(), LocalDateTime.now());
        if (claimed == 0) {
            throw new CustomBusinessException(ErrorCode.DUPLICATE_REQUEST, "이미 처리 중인 이체 요청입니다. 잠시 후 다시 시도해주세요.");
        }
        historyRecorder.record(scheduleSeq, plan.previousStatus(), "REQUESTED",
                RepaymentScheduleChangeReason.MANUAL_TRANSFER_REQUESTED, userSeq, null);
        return plan;
    }

    private RepaymentScheduleTransferResponseDto completeManualTransfer(
            ManualTransferPlan plan, SsafyTransferResponse transferResponse) {
        // 정산이 먼저 확정했다면 0건
        int updated = repaymentScheduleRepository.markPaidIfRequested(plan.scheduleSeq(), LocalDateTime.now());

        RepaymentSchedule schedule = repaymentScheduleRepository
                .findByScheduleSeqAndContractSeqWithContractUsers(plan.scheduleSeq(), plan.contractSeq())
                .orElseThrow(() -> new IllegalStateException(
                        "이체 후 회차를 찾을 수 없습니다. scheduleSeq=" + plan.scheduleSeq()));
        if (updated == 0 && !"PAID".equals(schedule.getStatus())) {
            throw new IllegalStateException(
                    "이체 후 회차가 선점 상태가 아닙니다. scheduleSeq=%d, status=%s"
                            .formatted(plan.scheduleSeq(), schedule.getStatus()));
        }
        Contract contract = schedule.getContract();

        // 정산이 먼저 확정했으면 알림·계약 종료는 이미 처리됨
        if (updated == 1) {
            historyRecorder.record(plan.scheduleSeq(), "REQUESTED", "PAID",
                    RepaymentScheduleChangeReason.TRANSFER_SUCCEEDED, plan.userSeq(), null);
            sendRepaymentNotificationsSafely(schedule);
            closeContractIfAllSchedulesCompleted(contract);
        }

        return new RepaymentScheduleTransferResponseDto(
                plan.contractSeq(),
                plan.scheduleSeq(),
                contract.getContractId(),
                schedule.getStatus(),
                extractTransactionDate(transferResponse),
                plan.withdrawalAccountNo(),
                plan.depositAccountNo()
        );
    }

    private record ManualTransferPlan(
            Integer contractSeq,
            Integer scheduleSeq,
            Integer userSeq,       // 이력에 남길 채무자
            String previousStatus, // 거절 시 되돌릴 상태
            String withdrawalAccountNo,
            String depositAccountNo,
            BigDecimal amount,
            String memo,
            String debtorUserKey
    ) {
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

        // 확인 직후 이체가 선점했을 수 있어 조건부로 바꾼다.
        LocalDateTime paidAt = LocalDateTime.now();
        String previousStatus = schedule.getStatus();
        if (repaymentScheduleRepository.markManualPaidIfUnpaid(scheduleSeq, paidAt) == 0) {
            throw new CustomBusinessException(ErrorCode.DUPLICATE_REQUEST,
                    "이 회차의 상태가 방금 바뀌었습니다. 상환 내역을 다시 확인해주세요.");
        }
        historyRecorder.record(scheduleSeq, previousStatus, "MANUAL_PAID",
                RepaymentScheduleChangeReason.CREDITOR_MARKED_PAID, userSeq, null);
        schedule.markManualPaid(paidAt);
        sendRepaymentNotificationsSafely(schedule);
        closeContractIfAllSchedulesCompleted(contract);

        return new RepaymentSchedulePaidResponseDto(
                contractSeq,
                scheduleSeq,
                contract.getContractId(),
                schedule.getStatus()
        );
    }

    // 이력 기록이 실패해도 회차는 REQUESTED로 남아 있으므로 예외를 넘기지 않는다.
    private void recordOutcomeUnknownSafely(ManualTransferPlan plan, TransferOutcomeUnknownException e) {
        try {
            transactionTemplate.executeWithoutResult(status -> historyRecorder.record(
                    plan.scheduleSeq(), "REQUESTED", "REQUESTED",
                    RepaymentScheduleChangeReason.TRANSFER_OUTCOME_UNKNOWN, plan.userSeq(), e.getMessage()));
        } catch (RuntimeException recordFailure) {
            log.error("[수동이체] 결과 불명 이력 기록 실패 - scheduleSeq={}", plan.scheduleSeq(), recordFailure);
        }
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
            try {
                notificationCommandService.sendContractClosedNotifications(contract);
            } catch (Exception e) {
                log.error("Failed to send contract closed notifications. contractSeq={}", contract.getContractSeq(), e);
            }
        }
    }

    private void sendRepaymentNotificationsSafely(RepaymentSchedule schedule) {
        try {
            notificationCommandService.sendRepaymentPaidNotifications(schedule);
        } catch (Exception e) {
            log.error("Failed to send repayment paid notifications. scheduleSeq={}", schedule.getScheduleSeq(), e);
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

