package com.ssafy.a305.be.domain.repayment.entity;

import com.ssafy.a305.be.domain.contract.entity.Contract;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "repayment_schedule",
        /*
         * 자동이체 대상 조회(status = 'NONPAID' AND due_dt <= :today)를 위한 인덱스.
         * 등치 조건인 status가 선행, 범위 조건인 due_dt가 후행이어야 한다.
         * 순서를 바꾸면 범위 조건 뒤의 컬럼을 타지 못한다.
         *
         * 이 인덱스가 없으면 옵티마이저가 계약 테이블을 전체 스캔한 뒤
         * 계약마다 회차를 되짚는 계획을 선택한다.
         */
        indexes = @Index(name = "idx_schedule_status_due_dt", columnList = "status, due_dt")
)
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class RepaymentSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "schedule_seq")
    private Integer scheduleSeq;

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

    /** NONPAID / SOMEPAID / PAID / MANUAL_PAID / WAIVED / OVERDUE */
    @Column(name = "status")
    private String status;

    @Column(name = "paid_dttm")
    private LocalDateTime paidDttm;

    @Column(name = "created_dttm")
    private LocalDateTime createdDttm;

    /**
     * 상환 완료 처리
     */
    public void markPaid(LocalDateTime paidAt) {
        this.status = "PAID";
        this.paidDttm = paidAt;
    }

    /**
     * 외부 거래 확인 기반 수동 상환 완료 처리
     */
    public void markManualPaid(LocalDateTime paidAt) {
        this.status = "MANUAL_PAID";
        this.paidDttm = paidAt;
    }

    /**
     * 연체 처리
     */
    public void markOverdue() {
        this.status = "OVERDUE";
    }
}
