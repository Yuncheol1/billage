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

    /**
     * NONPAID / SOMEPAID / PAID / MANUAL_PAID / WAIVED / OVERDUE / REQUESTED
     *
     * <p>REQUESTED는 은행에 이체를 요청했고 결과가 아직 확정되지 않은 상태다. 이 상태인 동안에는
     * 새 이체·수동 완료·연체 처리가 모두 이 회차를 건너뛴다. 돈이 이미 나갔을 수 있기 때문이다.</p>
     */
    @Column(name = "status")
    private String status;

    @Column(name = "paid_dttm")
    private LocalDateTime paidDttm;

    /**
     * 마지막으로 은행에 이체를 요청한 시각.
     *
     * <p>정산이 "지금 응답을 기다리는 중인 요청"과 "결과를 모른 채 멈춘 요청"을 구분하고,
     * 거래내역 조회 기간을 잡는 기준으로 쓴다.</p>
     */
    @Column(name = "transfer_requested_dttm")
    private LocalDateTime transferRequestedDttm;

    @Column(name = "created_dttm")
    private LocalDateTime createdDttm;

    /**
     * 이체 적요에 싣는 회차 식별자.
     *
     * <p>은행 응답을 받지 못하면 돈이 나갔는지 확인할 방법은 거래내역뿐인데, 거래내역에서 우리가
     * 내용을 정할 수 있는 칸은 적요 하나다. 그래서 회차 PK를 적요에 실어 보내고, 정산은 이 값으로
     * 거래를 찾는다. {@code BL} 접두사는 사용자가 직접 송금하며 적은 숫자와 구분하기 위한 것이다.</p>
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
