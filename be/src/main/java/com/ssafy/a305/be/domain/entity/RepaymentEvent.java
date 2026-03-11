package com.ssafy.a305.be.domain.entity;

import com.ssafy.a305.be.domain.enums.EvidenceGrade;
import com.ssafy.a305.be.domain.enums.RepaymentEventStatus;
import com.ssafy.a305.be.domain.enums.RepaymentEventType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "repayment_event")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class RepaymentEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "event_seq")
    private Long eventSeq;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_seq", nullable = false)
    private RepaymentSchedule repaymentSchedule;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contract_seq", nullable = false)
    private Contract contract;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type")
    private RepaymentEventType eventType;

    @Enumerated(EnumType.STRING)
    @Column(name = "evidence_grade")
    private EvidenceGrade evidenceGrade;

    @Column(name = "amount_paid", precision = 15, scale = 2)
    private BigDecimal amountPaid;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reported_by_user_seq", nullable = false)
    private User reportedByUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "confirmed_by_user_seq", nullable = false)
    private User confirmedByUser;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private RepaymentEventStatus status;

    @Column(name = "note")
    private String note;

    @Column(name = "requested_dttm")
    private LocalDateTime requestedDttm;

    @Column(name = "confirmed_dttm")
    private LocalDateTime confirmedDttm;
}
