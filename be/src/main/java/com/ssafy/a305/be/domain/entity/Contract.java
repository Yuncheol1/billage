package com.ssafy.a305.be.domain.entity;

import com.ssafy.a305.be.domain.enums.ContractStatus;
import com.ssafy.a305.be.domain.enums.ContractTemplateType;
import com.ssafy.a305.be.domain.enums.RepaymentMethod;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "contract")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Contract {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "contract_seq")
    private Long contractSeq;

    @Column(name = "contract_id")
    private String contractId;

    @Enumerated(EnumType.STRING)
    @Column(name = "template_type")
    private ContractTemplateType templateType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creditor_user_seq", nullable = false)
    private User creditor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "debtor_user_seq", nullable = false)
    private User debtor;

    @Column(name = "principal_amount", precision = 15, scale = 2)
    private BigDecimal principalAmount;

    @Column(name = "interest_rate", precision = 5, scale = 4)
    private BigDecimal interestRate;

    @Enumerated(EnumType.STRING)
    @Column(name = "repayment_method")
    private RepaymentMethod repaymentMethod;

    @Column(name = "total_installments")
    private Integer totalInstallments;

    @Column(name = "start_dt")
    private LocalDate startDt;

    @Column(name = "end_dt")
    private LocalDate endDt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private ContractStatus status;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "ai_draft_content", columnDefinition = "json")
    private String aiDraftContent;

    @Column(name = "created_dttm")
    private LocalDateTime createdDttm;

    @Column(name = "updated_dttm")
    private LocalDateTime updatedDttm;

    @Column(name = "signed_dttm")
    private LocalDateTime signedDttm;
}
