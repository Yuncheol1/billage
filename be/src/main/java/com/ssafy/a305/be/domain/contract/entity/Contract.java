package com.ssafy.a305.be.domain.contract.entity;

import com.ssafy.a305.be.domain.repayment.entity.RepaymentSchedule;
import com.ssafy.a305.be.domain.user.entity.User;
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
    private Integer contractSeq;

    @Column(name = "contract_id")
    private String contractId;

    /** FRIEND_SMALL / FAMILY_JEONSE / PARTNER_BIZ / COLLEAGUE_EMERGENCY */
    @Column(name = "is_family")
    private Boolean family;

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

    /** BULLET / INSTALLMENT / CUSTOM */
    @Column(name = "repayment_method")
    private String repaymentMethod;

    @Column(name = "total_installments")
    private Integer totalInstallments;

    @Column(name = "start_dt")
    private LocalDate startDt;

    @Column(name = "end_dt")
    private LocalDate endDt;

    /** DRAFT / NEGOTIATING / SIGNED / ACTIVE / OVERDUE / CLOSED / DISPUTED */
    @Column(name = "status")
    private String status;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "ai_draft_content", columnDefinition = "json")
    private String aiDraftContent;

    @Column(name = "created_dttm")
    private LocalDateTime createdDttm;

    @Column(name = "updated_dttm")
    private LocalDateTime updatedDttm;

    @Column(name = "signed_dttm")
    private LocalDateTime signedDttm;

    @Builder.Default
    @Column(name = "creditor_accepted")
    private Boolean creditorAccepted = false;

    @Builder.Default
    @Column(name = "debtor_accepted")
    private Boolean debtorAccepted = false;

    @Builder.Default
    @OneToMany(mappedBy = "contract", cascade = CascadeType.ALL, orphanRemoval = true)
    private java.util.List<ContractClause> clauses = new java.util.ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "contract", cascade = CascadeType.ALL, orphanRemoval = true)
    private java.util.List<RepaymentSchedule> repaymentSchedules = new java.util.ArrayList<>();

    public void addClause(ContractClause clause) {
        this.clauses.add(clause);
        // clause.setContract(this); -> if ContractClause doesn't have setter, maybe we don't need it if we used Builder nicely
    }
    public void removeClause(ContractClause clause) {
        this.clauses.remove(clause);
    }
    public void addRepaymentSchedule(RepaymentSchedule schedule) {
        this.repaymentSchedules.add(schedule);
    }

    public void markSigned(LocalDateTime signedAt) {
        this.status = "SIGNED";
        this.signedDttm = signedAt;
        this.updatedDttm = signedAt;
    }

    public void updateContractTerms(BigDecimal principalAmount, BigDecimal interestRate) {
        if (principalAmount != null) {
            this.principalAmount = principalAmount;
        }
        if (interestRate != null) {
            this.interestRate = interestRate;
        }
        this.updatedDttm = LocalDateTime.now();
        resetAgreement();
    }

    public void resetAgreement() {
        this.creditorAccepted = false;
        this.debtorAccepted = false;
    }

    public void resetToNegotiating() {
        this.signedDttm = null;
        this.status = "NEGOTIATING";
        resetAgreement();
    }

    public void accept(Integer userSeq) {
        if (creditor.getUserSeq().equals(userSeq)) {
            this.creditorAccepted = true;
        } else if (debtor.getUserSeq().equals(userSeq)) {
            this.debtorAccepted = true;
        }
        this.updatedDttm = LocalDateTime.now();
    }

    public void setAiDraftContent(String aiDraftContent) {
        this.aiDraftContent = aiDraftContent;
    }
}
