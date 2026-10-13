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
 * 회차 상태 변경 이력을 남긴다.
 *
 * <p>트랜잭션을 직접 열지 않고 호출자의 트랜잭션에 참여한다. 상태를 바꾼 조건부 UPDATE가
 * 1건을 바꿨을 때만 부른다. 0건이면 다른 경로가 먼저 바꾼 것이고, 그 경로가 이력을 남긴다.</p>
 *
 * <p>회차·사용자는 프록시 참조로만 연결해 이력 한 줄을 남기려고 다시 조회하지 않는다.</p>
 */
@Component
@RequiredArgsConstructor
public class RepaymentScheduleHistoryRecorder {

    static final int DETAIL_MAX_LENGTH = 500;

    private final RepaymentScheduleHistoryRepository historyRepository;
    private final RepaymentScheduleRepository repaymentScheduleRepository;
    private final UserRepository userRepository;

    /**
     * @param changedByUserSeq 바꾼 사용자. 스케줄러·정산·운영자 API면 null
     * @param detail           은행 응답, 확인 근거 등. 길면 잘라서 남긴다
     */
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
