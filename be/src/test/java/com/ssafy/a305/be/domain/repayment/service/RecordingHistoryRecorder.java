package com.ssafy.a305.be.domain.repayment.service;

import com.ssafy.a305.be.domain.repayment.entity.RepaymentScheduleChangeReason;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.ArrayList;
import java.util.List;

/**
 * 이력 기록을 저장하지 않고 목록에 모아 두는 테스트용 기록기.
 *
 * <p>기록 시점에 트랜잭션이 열려 있었는지도 함께 남겨, 이력이 상태 변경과 같은 트랜잭션에서
 * 저장되는지 검증할 수 있게 한다.</p>
 */
public class RecordingHistoryRecorder extends RepaymentScheduleHistoryRecorder {

    public final List<Entry> entries = new ArrayList<>();

    public RecordingHistoryRecorder() {
        super(null, null, null);
    }

    @Override
    public void record(
            Integer scheduleSeq,
            String fromStatus,
            String toStatus,
            RepaymentScheduleChangeReason reason,
            Integer changedByUserSeq,
            String detail
    ) {
        entries.add(new Entry(scheduleSeq, fromStatus, toStatus, reason, changedByUserSeq, detail,
                TransactionSynchronizationManager.isActualTransactionActive()));
    }

    public List<RepaymentScheduleChangeReason> reasons() {
        return entries.stream().map(Entry::reason).toList();
    }

    public record Entry(
            Integer scheduleSeq,
            String fromStatus,
            String toStatus,
            RepaymentScheduleChangeReason reason,
            Integer changedByUserSeq,
            String detail,
            boolean inTransaction
    ) {
    }
}
