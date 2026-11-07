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
 * 자동이체 스케줄러 (10시, 14시, 18시)
 * 매 실행마다 미확정 이체 정산을 먼저 하고, 미납 회차를 건별로 이체한다.
 * 한 건의 실패가 나머지를 롤백하지 않도록 여기에는 @Transactional을 붙이지 않는다.
 * 자동이체는 기한 당일만 시도하고, 다음 날 연체로 바뀌면 수동 상환으로 처리한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AutoTransferScheduler {

    private final RepaymentScheduleRepository repaymentScheduleRepository;

    private final AutoTransferService autoTransferService;
    private final TransferReconciliationService transferReconciliationService;
    private final TransferMonitor transferMonitor;

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
                // 한 건의 실패가 다음 건으로 번지지 않게 건별로 잡는다.
                AutoTransferService.Result result = autoTransferService.execute(scheduleSeq);
                transferMonitor.recordTransfer(result);
                switch (result) {
                    case SUCCEEDED -> succeeded++;
                    case SKIPPED -> skipped++;
                    case BLOCKED -> blocked++;
                    case UNRESOLVED -> unresolved++;
                }
            } catch (TransferFailException e) {
                // 잔액 부족 등 확실한 거절. 미납으로 돌아가 다음 실행에서 재시도
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
            log.warn("[자동이체] 결과 미확정 - 이번 실행에서 결과 불명={}, 미해결로 진입 차단={}", unresolved, blocked);
        }
        transferMonitor.alertIfTransferUnresolved(unresolved, blocked, failed);
    }

    // REQUESTED로 남은 회차를 거래내역과 대조해 확정
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
