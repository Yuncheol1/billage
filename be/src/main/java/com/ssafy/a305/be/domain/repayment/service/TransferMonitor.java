package com.ssafy.a305.be.domain.repayment.service;

import com.ssafy.a305.be.domain.repayment.repository.RepaymentScheduleRepository;
import com.ssafy.a305.be.global.external.alert.OpsAlertNotifier;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 자동이체·정산 지표와 운영 알림
 * attempts(이체 결과별), reconciliations(정산 결과별), unresolved(10분 넘게 남은 REQUESTED 수)
 */
@Component
public class TransferMonitor {

    private final MeterRegistry meterRegistry;
    private final RepaymentScheduleRepository repaymentScheduleRepository;
    private final OpsAlertNotifier opsAlertNotifier;

    public TransferMonitor(MeterRegistry meterRegistry,
                           RepaymentScheduleRepository repaymentScheduleRepository,
                           OpsAlertNotifier opsAlertNotifier) {
        this.meterRegistry = meterRegistry;
        this.repaymentScheduleRepository = repaymentScheduleRepository;
        this.opsAlertNotifier = opsAlertNotifier;

        Gauge.builder("billage.transfer.unresolved", this, TransferMonitor::countUnresolved)
                .description("정산 대기 시간이 지났는데도 결과가 확정되지 않은 회차 수")
                .register(meterRegistry);
    }

    public void recordTransfer(AutoTransferService.Result result) {
        counter("billage.transfer.attempts", result.name()).increment();
    }

    // 잔액 부족 등 확실한 거절
    public void recordTransferRejected() {
        counter("billage.transfer.attempts", "REJECTED").increment();
    }

    // 예상하지 못한 예외 (이체 성공 후 반영 실패 포함)
    public void recordTransferError() {
        counter("billage.transfer.attempts", "ERROR").increment();
    }

    public void recordReconciliation(TransferReconciliationService.Result result) {
        counter("billage.transfer.reconciliations", result.name()).increment();
    }

    public void alertIfReconciliationNeedsReview(int needsReview) {
        if (needsReview > 0) {
            opsAlertNotifier.send("[Billage 정산] 자동으로 판단하지 못한 회차 %d건. 거래내역 수동 확인 필요."
                    .formatted(needsReview));
        }
    }

    // 확정 거절은 재시도되는 정상 흐름이라 알리지 않는다.
    public void alertIfTransferUnresolved(int unresolved, int blocked, int failed) {
        if (unresolved > 0 || blocked > 0 || failed > 0) {
            opsAlertNotifier.send("[Billage 자동이체] 결과 불명 %d건, 미해결로 진입 차단 %d건, 예상하지 못한 오류 %d건."
                    .formatted(unresolved, blocked, failed));
        }
    }

    double countUnresolved() {
        return repaymentScheduleRepository.countByStatusAndTransferRequestedDttmLessThanEqual(
                "REQUESTED", LocalDateTime.now().minus(TransferReconciliationService.RECONCILE_AFTER));
    }

    private Counter counter(String name, String result) {
        return Counter.builder(name).tag("result", result).register(meterRegistry);
    }
}
