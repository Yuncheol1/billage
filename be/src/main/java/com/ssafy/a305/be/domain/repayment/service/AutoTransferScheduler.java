package com.ssafy.a305.be.domain.repayment.service;

import com.ssafy.a305.be.domain.repayment.repository.RepaymentScheduleRepository;
import com.ssafy.a305.be.global.exception.TransferFailException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 자동이체 스케줄러.
 *
 * <p>매일 10시·14시·18시에 미납 회차를 훑어 {@link AutoTransferService#execute(Integer)}를
 * 건별로 호출합니다. 성공한 회차는 PAID로 바뀌어 다음 슬롯의 대상에서 자동으로 빠지고,
 * 실패한 회차는 NONPAID로 남아 자동으로 다시 걸립니다. 즉 <b>도메인 상태가 곧 진행
 * 체크포인트</b>이므로 "어디까지 처리했는지"를 따로 저장할 필요가 없습니다.</p>
 *
 * <p>이 클래스에는 {@code @Transactional}을 붙이지 않습니다. 붙이면 전체 반복이 하나의
 * 트랜잭션이 되어 한 건의 실패가 나머지 전부를 롤백시킵니다. 트랜잭션 경계는 회차 1건을
 * 처리하는 {@link AutoTransferService} 안에서 구간별로 직접 관리합니다.</p>
 *
 * <p>매 실행은 <b>정산 → 이체</b> 순서입니다. 직전 실행 등에서 결과를 모른 채 REQUESTED로 남은
 * 회차를 먼저 거래내역과 대조해 확정하고, 그 결과 미납으로 돌아온 회차는 바로 이어지는 이체
 * 단계의 대상이 됩니다. 순서가 반대면 미납으로 돌아온 회차가 다음 슬롯까지 기다려야 합니다.</p>
 *
 * <p><b>자동이체는 납부 기한 당일에만 시도합니다.</b> 다음 날 09:30 연체 스케줄러가 NONPAID를
 * OVERDUE로 바꾸면 대상에서 빠지고, 이후에는 채무자의 수동 이체로 갚습니다. 정산에서 출금이 없다고
 * 판정된 회차도 같은 규칙을 따라, 기한이 지났으면 OVERDUE로 돌아가 자동 재시도되지 않습니다.
 * 연체 회차도 자동 재시도하려면 {@link RepaymentScheduleRepository#findDueScheduleSeqs}의 상태 조건을 함께
 * 바꿔야 합니다.</p>
 *
 * <p>정산으로도 판단하지 못한 회차는 자동으로 풀리지 않으므로, 로그에서 따로 세어 눈에 띄게 남기고
 * {@link TransferMonitor}로 지표를 기록해 운영 알림을 보냅니다.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AutoTransferScheduler {

    private final RepaymentScheduleRepository repaymentScheduleRepository;

    /** 회차 1건 처리. 트랜잭션 경계는 이 서비스가 구간별로 직접 관리한다. */
    private final AutoTransferService autoTransferService;

    /** 결과 불명 회차 1건 정산. */
    private final TransferReconciliationService transferReconciliationService;

    /** 처리 결과 지표와 운영 알림. */
    private final TransferMonitor transferMonitor;

    /** 1차 이체 10시, 2·3차 재시도 14시·18시. 남은 NONPAID만 다시 대상이 된다. */
    @Scheduled(cron = "0 0 10,14,18 * * *")
    public void runAutoTransfer() {
        reconcileUnresolvedTransfers();

        LocalDate today = LocalDate.now();
        List<Integer> targets = repaymentScheduleRepository.findDueScheduleSeqs(today);

        if (targets.isEmpty()) {
            log.info("[자동이체] 대상 없음 - date={}", today);
            return;
        }

        long startedAt = System.currentTimeMillis();
        int succeeded = 0;
        int skipped = 0;
        int blocked = 0;
        int unresolved = 0;
        int rejected = 0;
        int failed = 0;

        for (Integer scheduleSeq : targets) {
            try {
                // 한 건의 실패가 다음 건으로 번지지 않도록 여기서 잡는다.
                AutoTransferService.Result result = autoTransferService.execute(scheduleSeq);
                transferMonitor.recordTransfer(result);
                switch (result) {
                    case SUCCEEDED -> succeeded++;
                    case SKIPPED -> skipped++;
                    case BLOCKED -> blocked++;
                    case UNRESOLVED -> unresolved++;
                }
            } catch (TransferFailException e) {
                // 은행이 거절했음이 확실하다(잔액 부족 등). 회차는 미납으로 돌아가 다음 슬롯에서 재시도된다.
                rejected++;
                transferMonitor.recordTransferRejected();
                log.warn("[자동이체] 은행 거절 - scheduleSeq={}, 사유={}", scheduleSeq, e.getMessage());
            } catch (Exception e) {
                failed++;
                transferMonitor.recordTransferError();
                log.error("[자동이체] 처리 실패 - scheduleSeq={}, 사유={}",
                        scheduleSeq, e.getMessage(), e);
            }
        }

        log.info("[자동이체] 완료 - date={}, 대상={}, 성공={}, 스킵={}, 거절={}, 실패={}, 소요={}ms",
                today, targets.size(), succeeded, skipped, rejected, failed,
                System.currentTimeMillis() - startedAt);

        if (blocked > 0 || unresolved > 0) {
            // 결과가 확정되지 않은 이체가 있다. 자동으로 풀리지 않으므로 확인이 필요하다.
            log.warn("[자동이체] 결과 미확정 - 이번 실행에서 결과 불명={}, 미해결로 진입 차단={}", unresolved, blocked);
        }
        transferMonitor.alertIfTransferUnresolved(unresolved, blocked, failed);
    }

    /** REQUESTED로 남은 회차를 거래내역과 대조해 확정한다. 한 건의 실패가 다음 건으로 번지지 않게 건별로 잡는다. */
    void reconcileUnresolvedTransfers() {
        List<Integer> targets = transferReconciliationService.findTargets(LocalDateTime.now());
        if (targets.isEmpty()) {
            return;
        }

        int confirmedPaid = 0;
        int released = 0;
        int needsReview = 0;
        int skipped = 0;

        for (Integer scheduleSeq : targets) {
            try {
                TransferReconciliationService.Result result = transferReconciliationService.reconcile(scheduleSeq);
                transferMonitor.recordReconciliation(result);
                switch (result) {
                    case CONFIRMED_PAID -> confirmedPaid++;
                    case RELEASED -> released++;
                    case NEEDS_REVIEW -> needsReview++;
                    case SKIPPED -> skipped++;
                }
            } catch (Exception e) {
                needsReview++;
                transferMonitor.recordReconciliation(TransferReconciliationService.Result.NEEDS_REVIEW);
                log.error("[정산] 처리 실패 - scheduleSeq={}, 사유={}", scheduleSeq, e.getMessage(), e);
            }
        }

        log.info("[정산] 완료 - 대상={}, 출금 확인={}, 미납 복귀={}, 스킵={}, 수동 확인 필요={}",
                targets.size(), confirmedPaid, released, skipped, needsReview);
        if (needsReview > 0) {
            log.warn("[정산] 자동으로 판단하지 못한 회차 {}건. 거래내역 수동 확인 필요.", needsReview);
        }
        transferMonitor.alertIfReconciliationNeedsReview(needsReview);
    }
}
