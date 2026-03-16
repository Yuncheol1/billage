package com.ssafy.a305.be.domain.notification.entity;

import com.ssafy.a305.be.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "promise_reminder")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class PromiseReminder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "reminder_seq")
    private Integer reminderSeq;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_seq", nullable = false)
    private User creator;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_seq", nullable = false)
    private User target;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Column(name = "amount", precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(name = "due_dttm")
    private LocalDateTime dueDttm;

    @Column(name = "remind_dttm")
    private LocalDateTime remindDttm;

    @Column(name = "is_completed")
    private Boolean isCompleted;

    @Column(name = "created_dttm")
    private LocalDateTime createdDttm;
}
