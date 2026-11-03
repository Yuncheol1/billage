package com.ssafy.a305.be.domain.repayment.service;

import com.ssafy.a305.be.domain.repayment.repository.RepaymentScheduleRepository;
import com.ssafy.a305.be.global.exception.TransferFailException;
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
 * 자동이체 스케줄러 테스트. 정산이 이체보다 먼저 도는지, 정산 실패가 이체를 막지 않는지 확인
 */
@ExtendWith(MockitoExtension.class)
class AutoTransferSchedulerTest {

    @Mock
    private RepaymentScheduleRepository repaymentScheduleRepository;
    @Mock
    private AutoTransferService autoTransferService;
    @Mock
    private TransferReconciliationService transferReconciliationService;
    @Mock
    private TransferMonitor transferMonitor;

    private AutoTransferScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new AutoTransferScheduler(
                repaymentScheduleRepository, autoTransferService, transferReconciliationService, transferMonitor);
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

    @Test
    @DisplayName("은행의 확정 거절은 재시도되는 정상 흐름이라 오류로 알리지 않는다")
    void run_rejection_shouldNotAlertAsError() {
        given(transferReconciliationService.findTargets(any())).willReturn(List.of());
        given(repaymentScheduleRepository.findDueScheduleSeqs(any())).willReturn(List.of(9));
        given(autoTransferService.execute(9)).willThrow(new TransferFailException("SSAFY_TRANSFER_FAILED|A1014|잔액 부족"));

        scheduler.runAutoTransfer();

        then(transferMonitor).should().recordTransferRejected();
        then(transferMonitor).should().alertIfTransferUnresolved(0, 0, 0);
    }

    @Test
    @DisplayName("결과 불명과 예상하지 못한 오류는 건수를 세어 알린다")
    void run_unresolvedAndError_shouldBeAlerted() {
        given(transferReconciliationService.findTargets(any())).willReturn(List.of());
        given(repaymentScheduleRepository.findDueScheduleSeqs(any())).willReturn(List.of(9, 10, 11));
        given(autoTransferService.execute(9)).willReturn(AutoTransferService.Result.UNRESOLVED);
        given(autoTransferService.execute(10)).willReturn(AutoTransferService.Result.BLOCKED);
        given(autoTransferService.execute(11)).willThrow(new IllegalStateException("이체 후 결과 반영 실패"));

        scheduler.runAutoTransfer();

        then(transferMonitor).should().recordTransfer(AutoTransferService.Result.UNRESOLVED);
        then(transferMonitor).should().recordTransferError();
        then(transferMonitor).should().alertIfTransferUnresolved(1, 1, 1);
    }

    @Test
    @DisplayName("정산이 판단하지 못한 회차(예외 포함)는 수동 확인 건수로 알린다")
    void run_reconcileNeedsReview_shouldBeAlerted() {
        given(transferReconciliationService.findTargets(any())).willReturn(List.of(7, 8));
        given(transferReconciliationService.reconcile(7)).willReturn(TransferReconciliationService.Result.NEEDS_REVIEW);
        given(transferReconciliationService.reconcile(8)).willThrow(new IllegalStateException("DB down"));
        given(repaymentScheduleRepository.findDueScheduleSeqs(any())).willReturn(List.of());

        scheduler.runAutoTransfer();

        then(transferMonitor).should().alertIfReconciliationNeedsReview(2);
    }
}
