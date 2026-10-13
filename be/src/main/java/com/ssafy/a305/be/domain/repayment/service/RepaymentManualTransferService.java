package com.ssafy.a305.be.domain.repayment.service;

import com.ssafy.a305.be.domain.contract.entity.Contract;
import com.ssafy.a305.be.domain.notification.service.NotificationCommandService;
import com.ssafy.a305.be.domain.repayment.dto.response.RepaymentSchedulePaidResponseDto;
import com.ssafy.a305.be.domain.repayment.dto.response.RepaymentScheduleTransferResponseDto;
import com.ssafy.a305.be.domain.repayment.entity.AutoTransferRegistration;
import com.ssafy.a305.be.domain.repayment.entity.RepaymentSchedule;
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

    /**
     * 채무자가 회차를 직접 이체한다.
     *
     * <p>자동이체와 같은 3단 구조다. 회차 선점(Tx1) → 은행 API(트랜잭션 밖) → 결과 반영(Tx2).
     * 이 메서드에 {@code @Transactional}을 붙이지 않는다. 붙이면 API 호출이 트랜잭션 안으로 들어가
     * 커넥션을 응답 대기만큼 점유하고, 롤백 시 선점 기록까지 사라진다.</p>
     *
     * @see AutoTransferService
     */
    public RepaymentScheduleTransferResponseDto transferRepaymentSchedule(
            Integer contractSeq,
            Integer scheduleSeq,
            Integer userSeq
    ) {
        // ── Tx1: 검증 + 선점 ──
        ManualTransferPlan plan = transactionTemplate.execute(
                status -> prepareManualTransfer(contractSeq, scheduleSeq, userSeq));

        // ── 은행 API 호출: 트랜잭션 밖 ──
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
            // 돈이 나갔을 수 있다. 회차를 REQUESTED로 남겨 재이체를 막고, 사용자에게는 확인 중임을 알린다.
            log.error("[수동이체] 결과 불명 - scheduleSeq={}. 회차를 REQUESTED로 남긴다. 사유={}",
                    scheduleSeq, e.getMessage());
            throw new CustomBusinessException(ErrorCode.DUPLICATE_REQUEST,
                    "이체 결과를 확인하는 중입니다. 다시 이체하지 말고 잠시 후 상환 내역을 확인해주세요.");
        } catch (TransferFailException e) {
            transactionTemplate.executeWithoutResult(status ->
                    repaymentScheduleRepository.releaseTransferClaim(scheduleSeq, plan.previousStatus()));
            throw e;
        }

        // ── Tx2: 결과 반영 ──
        try {
            return transactionTemplate.execute(status -> completeManualTransfer(plan, transferResponse));
        } catch (RuntimeException e) {
            log.error("[수동이체] 이체 성공 후 결과 반영 실패 - scheduleSeq={}. 정산 대상으로 남긴다.",
                    scheduleSeq, e);
            throw e;
        }
    }

    /** Tx1. 검증을 통과하면 회차를 REQUESTED로 선점해 커밋될 계획을 돌려준다. */
    private ManualTransferPlan prepareManualTransfer(Integer contractSeq, Integer scheduleSeq, Integer userSeq) {
        RepaymentSchedule schedule = repaymentScheduleRepository
                .findByScheduleSeqAndContractSeqWithContractUsers(scheduleSeq, contractSeq)
                .orElseThrow(() -> new CustomBusinessException(ErrorCode.CONTRACT_NOT_FOUND, "해당 계약의 상환 회차를 찾을 수 없습니다."));

        Contract contract = schedule.getContract();
        validateContractStatus(contract);
        if (!contract.getDebtor().getUserSeq().equals(userSeq)) {
            throw new CustomBusinessException(ErrorCode.ACCESS_DENIED, "채무자만 수동 이체를 실행할 수 있습니다.");
        }

        // 이전 이체의 결과가 확정되지 않았다면 다시 이체하면 안 된다. 이미 나갔을 수 있다.
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

        // 선점 UPDATE가 영속성 컨텍스트를 비우므로, 엔티티에서 읽을 값은 선점 전에 모두 꺼낸다.
        // 적요는 자동이체와 같다. 정산은 두 경로를 구분하지 않고 "이 회차의 이체가 나갔는가"만 판정한다.
        ManualTransferPlan plan = new ManualTransferPlan(
                contractSeq, scheduleSeq, schedule.getStatus(),
                withdrawalAccountNo, depositAccountNo, amount, schedule.transferMemo(), debtorUserKey);

        int claimed = repaymentScheduleRepository.claimForTransfer(
                scheduleSeq, plan.previousStatus(), LocalDateTime.now());
        if (claimed == 0) {
            throw new CustomBusinessException(ErrorCode.DUPLICATE_REQUEST, "이미 처리 중인 이체 요청입니다. 잠시 후 다시 시도해주세요.");
        }
        return plan;
    }

    /** Tx2. 은행이 성공을 확인해 준 뒤에만 호출된다. */
    private RepaymentScheduleTransferResponseDto completeManualTransfer(
            ManualTransferPlan plan, SsafyTransferResponse transferResponse) {
        // 확인과 변경을 한 번에 한다. 정산이 먼저 확정했다면 0건이 된다.
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

        // 정산이 먼저 확정했다면(updated == 0) 알림·계약 종료는 그쪽에서 이미 했다. 응답만 돌려준다.
        if (updated == 1) {
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

    /** 트랜잭션 밖에서 API를 호출하는 데 필요한 값만 담는다. 엔티티를 들고 나가지 않는다. */
    private record ManualTransferPlan(
            Integer contractSeq,
            Integer scheduleSeq,
            /* 선점 직전 상태(NONPAID/OVERDUE). 확정 실패 시 이 상태로 되돌린다. */
            String previousStatus,
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

        // 위 확인과 이 UPDATE 사이에 이체가 선점했을 수 있다. 조건부로 바꿔 선점된 회차를 덮어쓰지 않는다.
        LocalDateTime paidAt = LocalDateTime.now();
        if (repaymentScheduleRepository.markManualPaidIfUnpaid(scheduleSeq, paidAt) == 0) {
            throw new CustomBusinessException(ErrorCode.DUPLICATE_REQUEST,
                    "이 회차의 상태가 방금 바뀌었습니다. 상환 내역을 다시 확인해주세요.");
        }
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

