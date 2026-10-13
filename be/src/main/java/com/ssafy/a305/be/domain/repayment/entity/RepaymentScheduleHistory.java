package com.ssafy.a305.be.domain.repayment.entity;

import com.ssafy.a305.be.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 회차 상태 변경 이력. 추가만 하고 고치거나 지우지 않는다.
 *
 * <p>회차의 상태 칸은 마지막 값만 남으므로, 미납 → 처리 중 → 미납 → 처리 중 → 완료를 거쳐도
 * 완료만 보인다. "왜 그날 출금이 안 됐는지"를 답하려면 바뀔 때마다 한 줄씩 쌓아야 한다.</p>
 *
 * <p>상태를 바꾼 조건부 UPDATE와 같은 트랜잭션에서 저장하므로, 상태가 바뀌면 이력도 남고
 * 롤백되면 같이 사라진다. 결과 불명처럼 상태가 그대로인 기록만 예외로 별도 트랜잭션에 남긴다.</p>
 */
@Entity
@Table(
        name = "repayment_schedule_history",
        indexes = @Index(name = "idx_schedule_history_schedule", columnList = "schedule_seq, history_seq")
)
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class RepaymentScheduleHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "history_seq")
    private Long historySeq;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_seq", nullable = false)
    private RepaymentSchedule repaymentSchedule;

    @Column(name = "from_status", length = 20)
    private String fromStatus;

    @Column(name = "to_status", nullable = false, length = 20)
    private String toStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "reason", nullable = false, length = 40)
    private RepaymentScheduleChangeReason reason;

    /** 바꾼 사용자. 스케줄러·정산·운영자 API처럼 사용자가 아닌 경로면 비어 있다. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "changed_by_user_seq")
    private User changedByUser;

    /** 은행 응답, 확인 근거 등. */
    @Column(name = "detail", length = 500)
    private String detail;

    @Column(name = "created_dttm", nullable = false)
    private LocalDateTime createdDttm;
}
