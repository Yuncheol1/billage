package com.ssafy.a305.be.domain.notification.entity;

import com.ssafy.a305.be.domain.contract.entity.Contract;
import com.ssafy.a305.be.domain.repayment.entity.RepaymentSchedule;
import com.ssafy.a305.be.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

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

    /** SENT / FAILED */
    @Column(name = "status")
    private String status;

    @Column(name = "fcm_message_id")
    private String fcmMessageId;

    @Column(name = "sent_dttm")
    private LocalDateTime sentDttm;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contract_seq", nullable = false)
    private Contract contract;

    public void markAsRead() {
        this.isRead = true;
    }

    public void markDispatchResult(String status, String fcmMessageId, LocalDateTime sentDttm) {
        this.status = status;
        this.fcmMessageId = fcmMessageId;
        this.sentDttm = sentDttm;
    }
}
