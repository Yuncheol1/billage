package com.ssafy.a305.be.domain.repayment.service;

import com.ssafy.a305.be.domain.contract.entity.Contract;
import com.ssafy.a305.be.domain.notification.service.NotificationCommandService;
import com.ssafy.a305.be.domain.repayment.entity.AutoTransferRegistration;
import com.ssafy.a305.be.domain.repayment.entity.RepaymentEvent;
import com.ssafy.a305.be.domain.repayment.entity.RepaymentSchedule;
import com.ssafy.a305.be.domain.repayment.entity.TransferAttempt;
import com.ssafy.a305.be.domain.repayment.repository.AutoTransferRegistrationRepository;
import com.ssafy.a305.be.domain.repayment.repository.RepaymentEventRepository;
import com.ssafy.a305.be.domain.repayment.repository.RepaymentScheduleRepository;
import com.ssafy.a305.be.domain.repayment.repository.TransferAttemptRepository;
import com.ssafy.a305.be.global.exception.TransferFailException;
import com.ssafy.a305.be.global.exception.TransferOutcomeUnknownException;
import com.ssafy.a305.be.global.external.ssafy.SsafyTransferApiClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
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
 *  Tx1  시도 기록(REQUESTED) 커밋     ← 이후 죽어도 "시도했다"는 흔적이 남는다
 *       ────────────────────────
 *       은행 API 호출 (트랜잭션 밖)   ← 커넥션·row 락을 응답 대기 동안 붙잡지 않는다
 *       ────────────────────────
 *  Tx2  결과 반영 커밋               ← 확정된 사실만 기록한다
 * </pre>
 *
 * <p>Tx1을 호출자 트랜잭션에 얹지 않고 먼저 커밋하는 이유: 함께 묶으면 이후 롤백 때 시도 기록도
 * 사라져, 막으려던 바로 그 상황(이체는 나갔는데 흔적이 없음)에서 무력해진다.</p>
 *
 * <p><b>결과를 알 수 없는 시도는 REQUESTED로 남고, 그 회차의 새 이체는 차단된다.</b>
 * 은행이 멱등성을 보장하지 않으므로, 이미 나갔을지도 모르는 이체를 다시 보내지 않는 것이
 * 유일한 안전장치이기 때문이다. 확정은 거래내역 대조(정산)의 몫이며 아직 구현하지 않았다.</p>
 *
 * <p>프로그래매틱 트랜잭션({@link TransactionTemplate})을 쓰므로 이 클래스의 메서드는 프록시
 * 경계에 의존하지 않는다. 락은 마지막 커밋이 끝난 뒤 {@code finally}에서 해제된다.</p>
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
        /** 처리 대상이 아니어서 건너뜀(락 점유 중, 등록 없음, 이미 납부됨 등). */
        SKIPPED,
        /** 이전 시도의 결과가 확정되지 않아 진입을 막음. */
        BLOCKED,
        /** 이체를 보냈지만 결과를 알 수 없음. 시도가 REQUESTED로 남았다. */
        UNRESOLVED
    }

    private final RepaymentScheduleRepository repaymentScheduleRepository;
    private final AutoTransferRegistrationRepository registrationRepository;
    private final RepaymentEventRepository repaymentEventRepository;
    private final TransferAttemptRepository transferAttemptRepository;
    private final NotificationCommandService notificationCommandService;
    private final SsafyTransferApiClient ssafyTransferApiClient;
    private final RepaymentTransferRequestGuard repaymentTransferRequestGuard;
    private final TransactionTemplate transactionTemplate;

    /**
     * 상환 회차 1건에 대해 자동이체를 실행한다.
     *
     * @throws TransferFailException 은행이 거절했음이 확실할 때. 호출자가 건별로 잡아 격리한다.
     */
    public Result execute(Integer scheduleSeq) {
        String lockToken = repaymentTransferRequestGuard.tryAcquire(scheduleSeq);
        if (lockToken == null) {
            log.info("[자동이체] scheduleSeq={}는 이미 처리 중이어서 Skip.", scheduleSeq);
            return Result.SKIPPED;
        }

        try {
            return process(scheduleSeq);
        } finally {
            // 모든 트랜잭션이 끝난 뒤이므로 미확정 상태가 다른 요청에 노출되지 않는다.
            repaymentTransferRequestGuard.release(scheduleSeq, lockToken);
        }
    }

    private Result process(Integer scheduleSeq) {
        // ── Tx1: 검증 + 시도 기록 ──
        Prepared prepared;
        try {
            prepared = transactionTemplate.execute(status -> prepare(scheduleSeq));
        } catch (DataIntegrityViolationException e) {
            // (schedule_seq, attempt_no) 유니크 제약이 동시 선점을 막은 경우. 락이 뚫렸을 때의 최후 방어선.
            log.warn("[자동이체] scheduleSeq={} 시도 번호가 이미 선점되어 Skip.", scheduleSeq);
            return Result.BLOCKED;
        }
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
            log.error("[자동이체] 결과 불명 - scheduleSeq={}, attemptNo={}. 시도를 미해결로 남긴다. 사유={}",
                    scheduleSeq, plan.attemptNo(), e.getMessage());
            return Result.UNRESOLVED;
        } catch (TransferFailException e) {
            // 은행이 거절했음이 확실하다. 시도를 확정 실패로 닫아 다음 슬롯의 재시도를 허용한다.
            transactionTemplate.executeWithoutResult(status ->
                    markAttemptFailed(plan.attemptSeq(), e.getResponseCode()));
            throw e;
        }

        // ── Tx2: 결과 반영 ──
        try {
            transactionTemplate.executeWithoutResult(status -> complete(plan));
        } catch (RuntimeException e) {
            // 이체는 나갔는데 반영에 실패했다. 시도는 REQUESTED로 남아 재이체를 막고 있다.
            log.error("[자동이체] 이체 성공 후 결과 반영 실패 - scheduleSeq={}, attemptNo={}. 수동 확인 필요",
                    scheduleSeq, plan.attemptNo(), e);
            throw e;
        }

        log.info("[자동이체] 성공 - scheduleSeq={}, attemptNo={}, amount={}",
                scheduleSeq, plan.attemptNo(), plan.amount());
        return Result.SUCCEEDED;
    }

    /** Tx1. 검증하고 시도를 REQUESTED로 열어 커밋될 계획을 돌려준다. */
    private Prepared prepare(Integer scheduleSeq) {
        RepaymentSchedule schedule = repaymentScheduleRepository
                .findByScheduleSeqWithContractUsers(scheduleSeq)
                .orElse(null);
        if (schedule == null) {
            log.warn("[자동이체] scheduleSeq={} 회차를 찾을 수 없습니다. Skip.", scheduleSeq);
            return Prepared.skip(Result.SKIPPED);
        }

        // 대상 목록을 뽑은 뒤 수동 이체 등으로 이미 처리됐을 수 있으므로 다시 확인한다.
        if (!"NONPAID".equals(schedule.getStatus())) {
            log.info("[자동이체] scheduleSeq={} 상태가 {}(으)로 바뀌어 Skip.", scheduleSeq, schedule.getStatus());
            return Prepared.skip(Result.SKIPPED);
        }

        // 이전 시도의 결과가 확정되지 않았다면 새 이체를 시작하면 안 된다. 이미 나갔을 수 있다.
        if (transferAttemptRepository.existsByScheduleSeqAndStatus(scheduleSeq, TransferAttempt.Status.REQUESTED)) {
            log.warn("[자동이체] scheduleSeq={} 이전 시도의 결과가 확정되지 않아 진입 차단. 확인 필요", scheduleSeq);
            return Prepared.skip(Result.BLOCKED);
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

        BigDecimal amount = schedule.getTotalDue();
        int attemptNo = transferAttemptRepository.findLastAttemptNo(scheduleSeq) + 1;
        String memo = RepaymentTransferMemo.of(schedule, attemptNo);

        TransferAttempt attempt = transferAttemptRepository.save(
                TransferAttempt.open(scheduleSeq, attemptNo, amount, LocalDateTime.now()));

        return Prepared.ready(new TransferPlan(
                attempt.getAttemptSeq(),
                scheduleSeq,
                attemptNo,
                registration.getDebtorAccount().getAccountNo(),
                registration.getCreditorAccount().getAccountNo(),
                amount,
                memo,
                // userKey는 채무자(출금자)의 키를 사용
                contract.getDebtor().getSsafyUserKey()
        ));
    }

    /** Tx2. 은행이 성공을 확인해 준 뒤에만 호출된다. */
    private void complete(TransferPlan plan) {
        RepaymentSchedule schedule = repaymentScheduleRepository
                .findByScheduleSeqWithContractUsers(plan.scheduleSeq())
                .orElseThrow(() -> new IllegalStateException(
                        "이체 후 회차를 찾을 수 없습니다. scheduleSeq=" + plan.scheduleSeq()));
        TransferAttempt attempt = transferAttemptRepository.findById(plan.attemptSeq())
                .orElseThrow(() -> new IllegalStateException(
                        "시도 기록을 찾을 수 없습니다. attemptSeq=" + plan.attemptSeq()));
        Contract contract = schedule.getContract();

        LocalDateTime now = LocalDateTime.now();
        attempt.markSucceeded(now);
        schedule.markPaid(now);

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

    private void markAttemptFailed(Long attemptSeq, String responseCode) {
        TransferAttempt attempt = transferAttemptRepository.findById(attemptSeq)
                .orElseThrow(() -> new IllegalStateException("시도 기록을 찾을 수 없습니다. attemptSeq=" + attemptSeq));
        attempt.markFailed(responseCode, LocalDateTime.now());
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
            Long attemptSeq,
            Integer scheduleSeq,
            int attemptNo,
            String withdrawalAccountNo,
            String depositAccountNo,
            BigDecimal amount,
            String memo,
            String debtorUserKey
    ) {
    }
}
