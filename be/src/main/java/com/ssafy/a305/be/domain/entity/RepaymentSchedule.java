package com.ssafy.a305.be.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "repayment_schedule")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class RepaymentSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "schedule_seq")
    private Long scheduleSeq;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contract_seq", nullable = false)
    private Contract contract;

    @Column(name = "installment_no")
    private Integer installmentNo;

    @Column(name = "due_dt")
    private LocalDate dueDt;

    @Column(name = "principal_due", precision = 15, scale = 2)
    private BigDecimal principalDue;

    @Column(name = "interest_due", precision = 15, scale = 2)
    private BigDecimal interestDue;

    @Column(name = "total_due", precision = 15, scale = 2)
    private BigDecimal totalDue;

    @Column(name = "remaining_balance_after", precision = 15, scale = 2)
    private BigDecimal remainingBalanceAfter;

    /** NONPAID / SOMEPAID / PAID / WAIVED / OVERDUE */
    @Column(name = "status")
    private String status;

    @Column(name = "paid_dttm")
    private LocalDateTime paidDttm;

    @Column(name = "created_dttm")
    private LocalDateTime createdDttm;
}
