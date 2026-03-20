package com.ssafy.a305.be.domain.contract.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 계약 조항 (INTEREST, REPAYMENT_DATE, PENALTY 등)
 */
@Entity
@Table(name = "contract_clause")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class ContractClause {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "clause_seq")
    private Integer clauseSeq;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contract_seq", nullable = false)
    private Contract contract;

    @Column(name = "clause_id")
    private String clauseId;

    @Column(name = "title")
    private String title;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Column(name = "order_no")
    private Integer orderNo;

    @Column(name = "created_dttm")
    private LocalDateTime createdDttm;

    @Column(name = "updated_dttm")
    private LocalDateTime updatedDttm;

    public void updateContent(String newContent) {
        this.content = newContent;
        this.updatedDttm = LocalDateTime.now();
    }
}
