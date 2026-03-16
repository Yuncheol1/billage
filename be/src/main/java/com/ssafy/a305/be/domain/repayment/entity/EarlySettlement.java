package com.ssafy.a305.be.domain.repayment.entity;

import com.ssafy.a305.be.domain.contract.entity.Contract;
import com.ssafy.a305.be.domain.document.entity.Document;
import com.ssafy.a305.be.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "early_settlement")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class EarlySettlement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "settlement_seq")
    private Integer settlementSeq;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contract_seq", nullable = false)
    private Contract contract;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requested_by_user_seq", nullable = false)
    private User requestedByUser;

    @Column(name = "remaining_principal", precision = 15, scale = 2)
    private BigDecimal remainingPrincipal;

    @Column(name = "remaining_interest", precision = 15, scale = 2)
    private BigDecimal remainingInterest;

    @Column(name = "settlement_amount", precision = 15, scale = 2)
    private BigDecimal settlementAmount;

    /** REQUESTED / AGREED / REJECTED */
    @Column(name = "status")
    private String status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_seq", nullable = false)
    private Document document;

    @Column(name = "created_dttm")
    private LocalDateTime createdDttm;

    @Column(name = "resolved_dttm")
    private LocalDateTime resolvedDttm;
}
