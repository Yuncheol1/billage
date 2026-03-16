package com.ssafy.a305.be.domain.tax.entity;

import com.ssafy.a305.be.domain.contract.entity.Contract;
import com.ssafy.a305.be.domain.repayment.entity.RepaymentEvent;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "tax_calculation")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class TaxCalculation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "tax_seq")
    private Integer taxSeq;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contract_seq", nullable = false)
    private Contract contract;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_seq", nullable = false)
    private RepaymentEvent repaymentEvent;

    @Column(name = "interest_income", precision = 15, scale = 2)
    private BigDecimal interestIncome;

    @Column(name = "withholding_tax", precision = 15, scale = 2)
    private BigDecimal withholdingTax;

    @Column(name = "cumulative_tax", precision = 15, scale = 2)
    private BigDecimal cumulativeTax;

    @Column(name = "calculated_ymd")
    private String calculatedYmd;
}
