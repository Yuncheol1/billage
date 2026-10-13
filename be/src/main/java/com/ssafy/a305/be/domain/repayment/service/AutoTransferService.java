package com.ssafy.a305.be.domain.repayment.service;

import com.ssafy.a305.be.domain.contract.entity.Contract;
import com.ssafy.a305.be.domain.notification.service.NotificationCommandService;
import com.ssafy.a305.be.domain.repayment.entity.AutoTransferRegistration;
import com.ssafy.a305.be.domain.repayment.entity.RepaymentEvent;
import com.ssafy.a305.be.domain.repayment.entity.RepaymentSchedule;
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
 * 자동이체 1건을 처리한다.
 *
 * <p>이체는 은행에서 일어나는 되돌릴 수 없는 부수 효과이므로, DB 트랜잭션으로 감싸는 대신
 * <b>세 구간으로 나눈다.</b></p>
 *
 * <pre>
 *  Tx1  회차를 REQUESTED로 선점·커밋   ← 이후 죽어도 "요청했다"는 흔적이 남는다
 *       ────────────────────────
 *       은행 API 호출 (트랜잭션 밖)   ← 커넥션·row 락을 응답 대기 동안 붙잡지 않는다
 *       ────────────────────────
 *  Tx2  결과 반영 커밋               ← 확정된 사실만 기록한다
 * </pre>
 *
 * <p>선점은 "조회한 상태 그대로일 때만 REQUESTED로 바꾸는" 조건부 UPDATE다. 같은 회차에
 * 자동이체와 수동 이체가 동시에 들어와도 DB가 한 건만 통과시키므로 별도의 락이 필요 없다.</p>
 *
 * <p><b>결과를 알 수 없는 회차는 REQUESTED로 남고, 새 이체가 차단된다.</b>
 * 은행이 멱등성을 보장하지 않으므로, 이미 나갔을지도 모르는 이체를 다시 보내지 않는 것이
 * 유일한 안전장치이기 때문이다. 확정은 {@link TransferReconciliationService}가 거래내역을
 * 대조해 맡는다.</p>
 *
 * @see AutoTransferScheduler
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AutoTransferService {

    /** 처리 1건의 결과. */
    public enum Result {
        /** 이체와 후처리까지 정상 완료. */
        SUCCEEDED,
        /** 처리 대상이 아니어서 건너뜀(등록 없음, 이미 납부됨 등). */
        SKIPPED,
        /** 이전 이체의 결과가 확정되지 않았거나, 다른 요청이 먼저 선점해 진입을 막음. */
        BLOCKED,
        /** 이체를 보냈지만 결과를 알 수 없음. 회차가 REQUESTED로 남았다. */
        UNRESOLVED
    }

    private final RepaymentScheduleRepository repaymentScheduleRepository;
    private final AutoTransferRegistrationRepository registrationRepository;
    private final RepaymentEventRepository repaymentEventRepository;
    private final NotificationCommandService notificationCommandService;
    private final SsafyTransferApiClient ssafyTransferApiClient;
    private final TransactionTemplate transactionTemplate;

    /**
     * 상환 회차 1건에 대해 자동이체를 실행한다.
     *
     * @throws TransferFailException 은행이 거절했음이 확실할 때. 호출자가 건별로 잡아 격리한다.
     */
    public Result execute(Integer scheduleSeq) {
        // ── Tx1: 검증 + 선점 ──
        Prepared prepared = transactionTemplate.execute(status -> prepare(scheduleSeq));
        if (prepared.plan() == null) {
            return prepared.skipResult();
        }
        TransferPlan plan = prepared.plan();

        // ── 은행 API 호출: 트랜잭션 밖 ──
        try {
            ssafyTransferApiClient.transfer(
                    plan.withdrawalAccountNo(),
                    plan.depositAccountNo(),
                    plan.amount(),
                    plan.memo(),
                    plan.debtorUserKey()
            );
        } catch (TransferOutcomeUnknownException e) {
            // 돈이 나갔을 수 있다. 실패로 단정하지 않고 REQUESTED로 남겨 새 이체를 막는다.
            log.error("[자동이체] 결과 불명 - scheduleSeq={}. 회차를 REQUESTED로 남긴다. 사유={}",
                    scheduleSeq, e.getMessage());
            return Result.UNRESOLVED;
        } catch (TransferFailException e) {
            // 은행이 거절했음이 확실하다. 이체 전 상태로 되돌려 다음 슬롯의 재시도를 허용한다.
            transactionTemplate.executeWithoutResult(status ->
                    repaymentScheduleRepository.releaseTransferClaim(scheduleSeq, plan.previousStatus()));
            throw e;
        }

        // ── Tx2: 결과 반영 ──
        try {
            transactionTemplate.executeWithoutResult(status -> complete(plan));
        } catch (RuntimeException e) {
            // 이체는 나갔는데 반영에 실패했다. 회차는 REQUESTED로 남아 재이체를 막고, 정산이 바로잡는다.
            log.error("[자동이체] 이체 성공 후 결과 반영 실패 - scheduleSeq={}. 정산 대상으로 남긴다.",
                    scheduleSeq, e);
            throw e;
        }

        log.info("[자동이체] 성공 - scheduleSeq={}, amount={}", scheduleSeq, plan.amount());
        return Result.SUCCEEDED;
    }

    /** Tx1. 검증하고 회차를 REQUESTED로 선점해 커밋될 계획을 돌려준다. */
    private Prepared prepare(Integer scheduleSeq) {
        RepaymentSchedule schedule = repaymentScheduleRepository
                .findByScheduleSeqWithContractUsers(scheduleSeq)
                .orElse(null);
        if (schedule == null) {
            log.warn("[자동이체] scheduleSeq={} 회차를 찾을 수 없습니다. Skip.", scheduleSeq);
            return Prepared.skip(Result.SKIPPED);
        }

        // 이전 이체의 결과가 확정되지 않았다면 새 이체를 시작하면 안 된다. 이미 나갔을 수 있다.
        if ("REQUESTED".equals(schedule.getStatus())) {
            log.warn("[자동이체] scheduleSeq={} 이전 이체의 결과가 확정되지 않아 진입 차단.", scheduleSeq);
            return Prepared.skip(Result.BLOCKED);
        }

        // 대상 목록을 뽑은 뒤 수동 이체 등으로 이미 처리됐을 수 있으므로 다시 확인한다.
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

        // 선점 UPDATE가 영속성 컨텍스트를 비우므로, 엔티티에서 읽을 값은 선점 전에 모두 꺼낸다.
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
            // 조회와 선점 사이에 다른 요청(수동 이체 등)이 먼저 상태를 바꿨다.
            log.warn("[자동이체] scheduleSeq={} 다른 요청이 먼저 선점해 진입 차단.", scheduleSeq);
            return Prepared.skip(Result.BLOCKED);
        }
        return Prepared.ready(plan);
    }

    /** Tx2. 은행이 성공을 확인해 준 뒤에만 호출된다. */
    private void complete(TransferPlan plan) {
        // 확인과 변경을 한 번에 한다. 정산이 먼저 확정했다면 0건이 되어 이벤트·알림을 다시 만들지 않는다.
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

    // ──────────────── 알림 (실패해도 이체 결과를 롤백하지 않는다) ────────────────

    /**
     * 알림 전송은 이체의 부수 효과일 뿐이므로 실패를 전파하지 않습니다.
     *
     * <p>여기서 예외가 새어나가면 결과 반영(Tx2)이 롤백되어, 돈은 빠져나갔는데 회차는
     * NONPAID로 남는 상태가 되므로 반드시 삼켜야 합니다.</p>
     */
    private void sendRepaymentPaidNotificationsSafely(RepaymentSchedule schedule) {
        try {
            notificationCommandService.sendRepaymentPaidNotifications(schedule);
        } catch (Exception e) {
            log.error("[자동이체] 상환 완료 알림 전송 실패 - scheduleSeq={}", schedule.getScheduleSeq(), e);
        }
    }

    /** @see #sendRepaymentPaidNotificationsSafely(RepaymentSchedule) */
    private void sendContractClosedNotificationsSafely(Contract contract) {
        try {
            notificationCommandService.sendContractClosedNotifications(contract);
        } catch (Exception e) {
            log.error("[자동이체] 계약 종료 알림 전송 실패 - contractSeq={}", contract.getContractSeq(), e);
        }
    }

    /** Tx1이 돌려주는 값. 진행할 계획이 있거나, 진행하지 않는 이유가 있다. */
    private record Prepared(Result skipResult, TransferPlan plan) {
        static Prepared skip(Result result) {
            return new Prepared(result, null);
        }

        static Prepared ready(TransferPlan plan) {
            return new Prepared(null, plan);
        }
    }

    /** 트랜잭션 밖에서 API를 호출하는 데 필요한 값만 담는다. 엔티티를 들고 나가지 않는다. */
    private record TransferPlan(
            Integer scheduleSeq,
            /* 선점 직전 상태. 확정 실패 시 이 상태로 되돌린다. */
            String previousStatus,
            String withdrawalAccountNo,
            String depositAccountNo,
            BigDecimal amount,
            String memo,
            String debtorUserKey
    ) {
    }
}
