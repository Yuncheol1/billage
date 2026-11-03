package com.ssafy.a305.be.domain.repayment.service;

import com.ssafy.a305.be.domain.repayment.entity.RepaymentScheduleChangeReason;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.ArrayList;
import java.util.List;

/**
 * 이력을 저장하지 않고 목록에 모으는 테스트용 기록기 (기록 시점의 트랜잭션 여부 포함)
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
