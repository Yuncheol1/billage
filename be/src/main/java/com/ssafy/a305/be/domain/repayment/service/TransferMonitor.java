package com.ssafy.a305.be.domain.repayment.service;

import com.ssafy.a305.be.domain.repayment.repository.RepaymentScheduleRepository;
import com.ssafy.a305.be.global.external.alert.OpsAlertNotifier;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 자동이체·정산의 운영 지표와 알림.
 *
 * <ul>
 *   <li>{@code billage.transfer.attempts{result}} — 자동이체 처리 결과별 누적 건수</li>
 *   <li>{@code billage.transfer.reconciliations{result}} — 정산 결과별 누적 건수</li>
 *   <li>{@code billage.transfer.unresolved} — 요청 후 {@link TransferReconciliationService#RECONCILE_AFTER}가
 *       지났는데도 REQUESTED로 남은 회차 수. 정산이 제때 돌고 있다면 0에 가까워야 한다.</li>
 * </ul>
 *
 * <p>REQUESTED로 묶인 회차는 자동으로 풀리지 않고 새 이체를 막으므로, 사람이 봐야 할 건이 생기면
 * 실행이 끝날 때 알림을 보낸다.</p>
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

    /** 은행이 거절했음이 확실한 이체(잔액 부족 등). 다음 슬롯에서 재시도되는 정상 흐름이다. */
    public void recordTransferRejected() {
        counter("billage.transfer.attempts", "REJECTED").increment();
    }

    /** 예상하지 못한 예외로 끝난 이체. 이체 성공 후 결과 반영 실패도 여기에 든다. */
    public void recordTransferError() {
        counter("billage.transfer.attempts", "ERROR").increment();
    }

    public void recordReconciliation(TransferReconciliationService.Result result) {
        counter("billage.transfer.reconciliations", result.name()).increment();
    }

    /** 정산 실행이 끝난 뒤 호출한다. 자동으로 판단하지 못한 회차가 있으면 알린다. */
    public void alertIfReconciliationNeedsReview(int needsReview) {
        if (needsReview > 0) {
            opsAlertNotifier.send("[Billage 정산] 자동으로 판단하지 못한 회차 %d건. 거래내역 수동 확인 필요."
                    .formatted(needsReview));
        }
    }

    /**
     * 이체 실행이 끝난 뒤 호출한다. 결과가 확정되지 않았거나 예상하지 못한 오류가 난 이체가 있으면 알린다.
     * 은행의 확정 거절(잔액 부족 등)은 재시도되는 정상 흐름이라 알리지 않는다.
     */
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
