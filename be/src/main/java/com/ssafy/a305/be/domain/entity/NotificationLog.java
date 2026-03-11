package com.ssafy.a305.be.domain.entity;

import com.ssafy.a305.be.domain.enums.NotificationStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * event_key: REPAYMENT_D7/D3/D1, SIGN_COMPLETE, CONTRACT_SHARED, CONTRACT_MODIFIED,
 * CONTRACT_AGREED, MANUAL_TRANSFER_REQUEST, CASH_CONFIRM_REQUEST, WAIVER_ISSUED, OVERDUE 등
 */
@Entity
@Table(name = "notification_log")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class NotificationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notification_seq")
    private Long notificationSeq;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_seq", nullable = false)
    private User user;

    @Column(name = "event_key")
    private String eventKey;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_seq", nullable = false)
    private RepaymentSchedule repaymentSchedule;

    @Column(name = "title")
    private String title;

    @Column(name = "message", columnDefinition = "TEXT")
    private String message;

    @Column(name = "is_read")
    private Boolean isRead;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private NotificationStatus status;

    @Column(name = "expo_message_id")
    private String expoMessageId;

    @Column(name = "sent_dttm")
    private LocalDateTime sentDttm;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contract_seq", nullable = false)
    private Contract contract;
}
