package com.ssafy.a305.be.domain.repayment.service;

import com.ssafy.a305.be.domain.repayment.repository.RepaymentScheduleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.inOrder;

/**
 * 자동이체 스케줄러 테스트.
 *
 * <p>정산이 이체보다 먼저 돌아야, 정산으로 미납에 돌아온 회차가 같은 실행에서 다시 이체된다.
 * 또 한 건의 정산 실패가 이체 단계를 막지 않아야 한다.</p>
 */
@ExtendWith(MockitoExtension.class)
class AutoTransferSchedulerTest {

    @Mock
    private RepaymentScheduleRepository repaymentScheduleRepository;
    @Mock
    private AutoTransferService autoTransferService;
    @Mock
    private TransferReconciliationService transferReconciliationService;

    private AutoTransferScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new AutoTransferScheduler(
                repaymentScheduleRepository, autoTransferService, transferReconciliationService);
    }

    @Test
    @DisplayName("정산을 먼저 하고, 그 뒤에 이체 대상을 조회한다")
    void run_shouldReconcileBeforeTransfer() {
        given(transferReconciliationService.findTargets(any())).willReturn(List.of(7));
        given(transferReconciliationService.reconcile(7)).willReturn(TransferReconciliationService.Result.RELEASED);
        given(repaymentScheduleRepository.findDueScheduleSeqs(any())).willReturn(List.of(7));
        given(autoTransferService.execute(7)).willReturn(AutoTransferService.Result.SUCCEEDED);

        scheduler.runAutoTransfer();

        InOrder order = inOrder(transferReconciliationService, repaymentScheduleRepository, autoTransferService);
        order.verify(transferReconciliationService).reconcile(7);
        order.verify(repaymentScheduleRepository).findDueScheduleSeqs(any());
        order.verify(autoTransferService).execute(7);
    }

    @Test
    @DisplayName("정산 한 건이 실패해도 다음 정산과 이체 단계는 계속된다")
    void run_reconcileFailure_shouldNotStopOthers() {
        given(transferReconciliationService.findTargets(any())).willReturn(List.of(7, 8));
        given(transferReconciliationService.reconcile(7)).willThrow(new IllegalStateException("DB down"));
        given(transferReconciliationService.reconcile(8)).willReturn(TransferReconciliationService.Result.CONFIRMED_PAID);
        given(repaymentScheduleRepository.findDueScheduleSeqs(any())).willReturn(List.of(9));
        given(autoTransferService.execute(9)).willReturn(AutoTransferService.Result.SUCCEEDED);

        scheduler.runAutoTransfer();

        then(transferReconciliationService).should().reconcile(8);
        then(autoTransferService).should().execute(9);
    }
}
