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
        // 자동이체 대상 조회용. 등치 조건(status) 먼저, 범위 조건(due_dt) 나중
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

    // NONPAID / SOMEPAID / PAID / MANUAL_PAID / WAIVED / OVERDUE / REQUESTED(이체 결과 대기)
    @Column(name = "status")
    private String status;

    @Column(name = "paid_dttm")
    private LocalDateTime paidDttm;

    // 정산 대상 판단과 거래내역 조회 시작일 기준
    @Column(name = "transfer_requested_dttm")
    private LocalDateTime transferRequestedDttm;

    @Column(name = "created_dttm")
    private LocalDateTime createdDttm;

    /**
     * 이체 적요에 넣는 회차 표식. 정산이 거래내역에서 이 값으로 이체를 찾는다.
     */
    public String transferMemo() {
        return "BL" + scheduleSeq;
    }

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
