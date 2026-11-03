package com.ssafy.a305.be.domain.repayment.service;

import com.ssafy.a305.be.domain.contract.entity.Contract;
import com.ssafy.a305.be.domain.notification.service.NotificationCommandService;
import com.ssafy.a305.be.domain.repayment.entity.AutoTransferRegistration;
import com.ssafy.a305.be.domain.repayment.entity.RepaymentEvent;
import com.ssafy.a305.be.domain.repayment.entity.RepaymentSchedule;
import com.ssafy.a305.be.domain.repayment.entity.RepaymentScheduleChangeReason;
import com.ssafy.a305.be.domain.repayment.repository.AutoTransferRegistrationRepository;
import com.ssafy.a305.be.domain.repayment.repository.RepaymentEventRepository;
import com.ssafy.a305.be.domain.repayment.repository.RepaymentScheduleRepository;
import com.ssafy.a305.be.global.exception.TransferFailException;
import com.ssafy.a305.be.global.exception.TransferOutcomeUnknownException;
import com.ssafy.a305.be.global.external.ssafy.SsafyTransferApiClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 자동이체 1건 처리.
 * Tx1(REQUESTED 선점 커밋) → 은행 API 호출(트랜잭션 밖) → Tx2(결과 반영) 순서로 나눈다.
 * 결과를 모르면 REQUESTED로 남겨 재이체를 막고, 확정은 TransferReconciliationService가 한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AutoTransferService {

    public enum Result {
        SUCCEEDED,
        SKIPPED,    // 등록 없음, 이미 납부됨 등
        BLOCKED,    // 이전 이체가 미확정이거나 다른 요청이 먼저 선점
        UNRESOLVED  // 이체 결과 불명, REQUESTED 유지
    }

    private final RepaymentScheduleRepository repaymentScheduleRepository;
    private final AutoTransferRegistrationRepository registrationRepository;
    private final RepaymentEventRepository repaymentEventRepository;
    private final NotificationCommandService notificationCommandService;
    private final SsafyTransferApiClient ssafyTransferApiClient;
    private final TransactionTemplate transactionTemplate;
    private final RepaymentScheduleHistoryRecorder historyRecorder;

    // 은행이 확실히 거절하면 TransferFailException을 던진다.
    public Result execute(Integer scheduleSeq) {
        // 1. 검증 + 선점 (Tx1)
        Prepared prepared = transactionTemplate.execute(status -> prepare(scheduleSeq));
        if (prepared.plan() == null) {
            return prepared.skipResult();
        }
        TransferPlan plan = prepared.plan();

        // 2. 은행 API 호출 (트랜잭션 밖)
        try {
            ssafyTransferApiClient.transfer(
                    plan.withdrawalAccountNo(),
                    plan.depositAccountNo(),
                    plan.amount(),
                    plan.memo(),
                    plan.debtorUserKey()
            );
        } catch (TransferOutcomeUnknownException e) {
            // 돈이 나갔을 수 있으므로 실패로 처리하지 않는다.
            log.error("[자동이체] 결과 불명 - scheduleSeq={}. 회차를 REQUESTED로 남긴다. 사유={}",
                    scheduleSeq, e.getMessage());
            recordOutcomeUnknownSafely(scheduleSeq, e);
            return Result.UNRESOLVED;
        } catch (TransferFailException e) {
            // 확실한 거절 → 되돌려서 다음 실행에서 재시도
            transactionTemplate.executeWithoutResult(status -> {
                if (repaymentScheduleRepository.releaseTransferClaim(scheduleSeq, plan.previousStatus()) == 1) {
                    historyRecorder.record(scheduleSeq, "REQUESTED", plan.previousStatus(),
                            RepaymentScheduleChangeReason.TRANSFER_REJECTED, null, e.getMessage());
                }
            });
            throw e;
        }

        // 3. 결과 반영 (Tx2)
        try {
            transactionTemplate.executeWithoutResult(status -> complete(plan));
        } catch (RuntimeException e) {
            // 이체는 됐는데 반영 실패. REQUESTED로 남아 정산이 바로잡는다.
            log.error("[자동이체] 이체 성공 후 결과 반영 실패 - scheduleSeq={}. 정산 대상으로 남긴다.",
                    scheduleSeq, e);
            throw e;
        }

        log.info("[자동이체] 성공 - scheduleSeq={}, amount={}", scheduleSeq, plan.amount());
        return Result.SUCCEEDED;
    }

    private Prepared prepare(Integer scheduleSeq) {
        RepaymentSchedule schedule = repaymentScheduleRepository
                .findByScheduleSeqWithContractUsers(scheduleSeq)
                .orElse(null);
        if (schedule == null) {
            log.warn("[자동이체] scheduleSeq={} 회차를 찾을 수 없습니다. Skip.", scheduleSeq);
            return Prepared.skip(Result.SKIPPED);
        }

        // 이전 이체가 미확정이면 이미 나갔을 수 있다.
        if ("REQUESTED".equals(schedule.getStatus())) {
            log.warn("[자동이체] scheduleSeq={} 이전 이체의 결과가 확정되지 않아 진입 차단.", scheduleSeq);
            return Prepared.skip(Result.BLOCKED);
        }

        // 목록 조회 후 수동 이체 등으로 바뀌었을 수 있다.
        if (!"NONPAID".equals(schedule.getStatus())) {
            log.info("[자동이체] scheduleSeq={} 상태가 {}(으)로 바뀌어 Skip.", scheduleSeq, schedule.getStatus());
            return Prepared.skip(Result.SKIPPED);
        }

        Contract contract = schedule.getContract();
        Integer contractSeq = contract.getContractSeq();

        AutoTransferRegistration registration = registrationRepository
                .findActiveWithAccounts(contractSeq)
                .orElse(null);
        if (registration == null) {
            log.warn("[자동이체] contractSeq={} 에 대한 활성 자동이체 등록이 없습니다. Skip.", contractSeq);
            return Prepared.skip(Result.SKIPPED);
        }

        // 선점 UPDATE가 영속성 컨텍스트를 비우므로 값은 미리 꺼내 둔다.
        TransferPlan plan = new TransferPlan(
                scheduleSeq,
                schedule.getStatus(),
                registration.getDebtorAccount().getAccountNo(),
                registration.getCreditorAccount().getAccountNo(),
                schedule.getTotalDue(),
                schedule.transferMemo(),
                // userKey는 채무자(출금자)의 키를 사용
                contract.getDebtor().getSsafyUserKey()
        );

        int claimed = repaymentScheduleRepository.claimForTransfer(
                scheduleSeq, plan.previousStatus(), LocalDateTime.now());
        if (claimed == 0) {
            log.warn("[자동이체] scheduleSeq={} 다른 요청이 먼저 선점해 진입 차단.", scheduleSeq);
            return Prepared.skip(Result.BLOCKED);
        }
        historyRecorder.record(scheduleSeq, plan.previousStatus(), "REQUESTED",
                RepaymentScheduleChangeReason.AUTO_TRANSFER_REQUESTED, null, null);
        return Prepared.ready(plan);
    }

    private void complete(TransferPlan plan) {
        // 정산이 먼저 확정했다면 0건 → 이벤트·알림을 다시 만들지 않는다.
        LocalDateTime now = LocalDateTime.now();
        int updated = repaymentScheduleRepository.markPaidIfRequested(plan.scheduleSeq(), now);

        RepaymentSchedule schedule = repaymentScheduleRepository
                .findByScheduleSeqWithContractUsers(plan.scheduleSeq())
                .orElseThrow(() -> new IllegalStateException(
                        "이체 후 회차를 찾을 수 없습니다. scheduleSeq=" + plan.scheduleSeq()));
        if (updated == 0) {
            if ("PAID".equals(schedule.getStatus())) {
                log.info("[자동이체] scheduleSeq={} 정산이 먼저 확정해 결과 반영을 건너뜀", plan.scheduleSeq());
                return;
            }
            throw new IllegalStateException(
                    "이체 후 회차가 선점 상태가 아닙니다. scheduleSeq=%d, status=%s"
                            .formatted(plan.scheduleSeq(), schedule.getStatus()));
        }
        historyRecorder.record(plan.scheduleSeq(), "REQUESTED", "PAID",
                RepaymentScheduleChangeReason.TRANSFER_SUCCEEDED, null, null);
        Contract contract = schedule.getContract();

        // 상환 이벤트 기록 (evidenceGrade=GOLD, status=AUTO_CONFIRMED)
        RepaymentEvent event = RepaymentEvent.builder()
                .repaymentSchedule(schedule)
                .contract(contract)
                .evidenceGrade("GOLD")
                .amountPaid(plan.amount())
                .reportedByUser(contract.getDebtor())
                .confirmedByUser(contract.getCreditor())
                .status("AUTO_CONFIRMED")
                .note("자동이체 처리")
                .requestedDttm(now)
                .confirmedDttm(now)
                .build();
        repaymentEventRepository.save(event);

        sendRepaymentPaidNotificationsSafely(schedule);

        boolean allSchedulesCompleted = contract.getRepaymentSchedules().stream()
                .allMatch(item -> "PAID".equals(item.getStatus())
                        || "MANUAL_PAID".equals(item.getStatus())
                        || "WAIVED".equals(item.getStatus()));
        if (allSchedulesCompleted) {
            contract.markClosed(now);
            sendContractClosedNotificationsSafely(contract);
        }
    }

    // 이력 기록이 실패해도 회차는 REQUESTED로 남아 있으므로 예외를 넘기지 않는다.
    private void recordOutcomeUnknownSafely(Integer scheduleSeq, TransferOutcomeUnknownException e) {
        try {
            transactionTemplate.executeWithoutResult(status -> historyRecorder.record(
                    scheduleSeq, "REQUESTED", "REQUESTED",
                    RepaymentScheduleChangeReason.TRANSFER_OUTCOME_UNKNOWN, null, e.getMessage()));
        } catch (RuntimeException recordFailure) {
            log.error("[자동이체] 결과 불명 이력 기록 실패 - scheduleSeq={}", scheduleSeq, recordFailure);
        }
    }

    // 알림 실패로 Tx2가 롤백되면 돈은 나갔는데 기록이 없어진다. 예외를 삼킨다.
    private void sendRepaymentPaidNotificationsSafely(RepaymentSchedule schedule) {
        try {
            notificationCommandService.sendRepaymentPaidNotifications(schedule);
        } catch (Exception e) {
            log.error("[자동이체] 상환 완료 알림 전송 실패 - scheduleSeq={}", schedule.getScheduleSeq(), e);
        }
    }

    private void sendContractClosedNotificationsSafely(Contract contract) {
        try {
            notificationCommandService.sendContractClosedNotifications(contract);
        } catch (Exception e) {
            log.error("[자동이체] 계약 종료 알림 전송 실패 - contractSeq={}", contract.getContractSeq(), e);
        }
    }

    private record Prepared(Result skipResult, TransferPlan plan) {
        static Prepared skip(Result result) {
            return new Prepared(result, null);
        }

        static Prepared ready(TransferPlan plan) {
            return new Prepared(null, plan);
        }
    }

    // 트랜잭션 밖으로 엔티티를 들고 나가지 않도록 필요한 값만 담는다.
    private record TransferPlan(
            Integer scheduleSeq,
            String previousStatus, // 거절 시 되돌릴 상태
            String withdrawalAccountNo,
            String depositAccountNo,
            BigDecimal amount,
            String memo,
            String debtorUserKey
    ) {
    }
}
