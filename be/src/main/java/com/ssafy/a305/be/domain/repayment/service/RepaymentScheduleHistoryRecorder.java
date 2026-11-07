package com.ssafy.a305.be.domain.repayment.service;

import com.ssafy.a305.be.domain.repayment.entity.RepaymentScheduleChangeReason;
import com.ssafy.a305.be.domain.repayment.entity.RepaymentScheduleHistory;
import com.ssafy.a305.be.domain.repayment.repository.RepaymentScheduleHistoryRepository;
import com.ssafy.a305.be.domain.repayment.repository.RepaymentScheduleRepository;
import com.ssafy.a305.be.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 회차 상태 변경 이력. 호출자 트랜잭션에 참여하고, 조건부 UPDATE가 1건일 때만 호출한다.
 */
@Component
@RequiredArgsConstructor
public class RepaymentScheduleHistoryRecorder {

    static final int DETAIL_MAX_LENGTH = 500;

    private final RepaymentScheduleHistoryRepository historyRepository;
    private final RepaymentScheduleRepository repaymentScheduleRepository;
    private final UserRepository userRepository;

    // changedByUserSeq: 스케줄러·정산·운영자 API면 null
    public void record(
            Integer scheduleSeq,
            String fromStatus,
            String toStatus,
            RepaymentScheduleChangeReason reason,
            Integer changedByUserSeq,
            String detail
    ) {
        historyRepository.save(RepaymentScheduleHistory.builder()
                .repaymentSchedule(repaymentScheduleRepository.getReferenceById(scheduleSeq))
                .fromStatus(fromStatus)
                .toStatus(toStatus)
                .reason(reason)
                .changedByUser(changedByUserSeq == null ? null : userRepository.getReferenceById(changedByUserSeq))
                .detail(truncate(detail))
                .createdDttm(LocalDateTime.now())
                .build());
    }

    private static String truncate(String detail) {
        if (detail == null || detail.length() <= DETAIL_MAX_LENGTH) {
            return detail;
        }
        return detail.substring(0, DETAIL_MAX_LENGTH);
    }
}
