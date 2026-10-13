package com.ssafy.a305.be.domain.repayment.service;

import com.ssafy.a305.be.domain.repayment.repository.RepaymentScheduleRepository;
import com.ssafy.a305.be.global.external.alert.OpsAlertNotifier;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

/**
 * 자동이체 운영 지표·알림 테스트.
 *
 * <p>결과별 건수가 지표로 쌓이고, 정산 대기 시간이 지난 REQUESTED 회차 수가 게이지로 보이며,
 * 사람이 봐야 할 건이 있을 때만 알림이 나가는지 고정한다.</p>
 */
@ExtendWith(MockitoExtension.class)
class TransferMonitorTest {

    @Mock
    private RepaymentScheduleRepository repaymentScheduleRepository;
    @Mock
    private OpsAlertNotifier opsAlertNotifier;

    private SimpleMeterRegistry registry;
    private TransferMonitor monitor;

    @BeforeEach
    void setUp() {
        registry = new SimpleMeterRegistry();
        monitor = new TransferMonitor(registry, repaymentScheduleRepository, opsAlertNotifier);
    }

    @Test
    @DisplayName("이체·정산 결과가 결과별 카운터로 쌓인다")
    void record_shouldCountByResult() {
        monitor.recordTransfer(AutoTransferService.Result.SUCCEEDED);
        monitor.recordTransfer(AutoTransferService.Result.SUCCEEDED);
        monitor.recordTransferRejected();
        monitor.recordReconciliation(TransferReconciliationService.Result.NEEDS_REVIEW);

        assertThat(registry.get("billage.transfer.attempts").tag("result", "SUCCEEDED").counter().count()).isEqualTo(2);
        assertThat(registry.get("billage.transfer.attempts").tag("result", "REJECTED").counter().count()).isEqualTo(1);
        assertThat(registry.get("billage.transfer.reconciliations").tag("result", "NEEDS_REVIEW").counter().count())
                .isEqualTo(1);
    }

    @Test
    @DisplayName("정산 대기 시간이 지난 REQUESTED 회차 수를 게이지로 보여준다")
    void unresolvedGauge_shouldReadStaleRequestedCount() {
        given(repaymentScheduleRepository.countByStatusAndTransferRequestedDttmLessThanEqual(
                eq("REQUESTED"), any(LocalDateTime.class))).willReturn(3L);

        assertThat(registry.get("billage.transfer.unresolved").gauge().value()).isEqualTo(3);
    }

    @Test
    @DisplayName("사람이 봐야 할 건이 없으면 알림을 보내지 않는다")
    void alert_shouldStayQuietWhenNothingToReview() {
        monitor.alertIfReconciliationNeedsReview(0);
        monitor.alertIfTransferUnresolved(0, 0, 0);

        then(opsAlertNotifier).should(org.mockito.Mockito.never()).send(anyString());
    }

    @Test
    @DisplayName("수동 확인 건이나 결과 불명 건이 있으면 건수를 담아 알린다")
    void alert_shouldSendCounts() {
        monitor.alertIfReconciliationNeedsReview(2);
        monitor.alertIfTransferUnresolved(1, 0, 0);

        then(opsAlertNotifier).should().send(contains("수동 확인"));
        then(opsAlertNotifier).should().send(contains("결과 불명 1건"));
    }
}
