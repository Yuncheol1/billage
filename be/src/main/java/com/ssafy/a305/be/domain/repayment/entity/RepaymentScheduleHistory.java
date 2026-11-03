package com.ssafy.a305.be.domain.repayment.entity;

import com.ssafy.a305.be.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 회차 상태 변경 이력 (추가만 하고 수정·삭제하지 않음)
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

    // 스케줄러·정산·운영자 API면 null
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "changed_by_user_seq")
    private User changedByUser;

    @Column(name = "detail", length = 500)
    private String detail;

    @Column(name = "created_dttm", nullable = false)
    private LocalDateTime createdDttm;
}
