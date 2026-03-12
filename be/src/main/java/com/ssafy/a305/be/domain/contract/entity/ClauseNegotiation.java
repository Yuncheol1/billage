package com.ssafy.a305.be.domain.contract.entity;

import com.ssafy.a305.be.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "clause_negotiation")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class ClauseNegotiation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "negotiation_seq")
    private Long negotiationSeq;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "clause_seq", nullable = false)
    private ContractClause contractClause;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requested_by_user_seq", nullable = false)
    private User requestedByUser;

    @Column(name = "proposed_content", columnDefinition = "TEXT")
    private String proposedContent;

    @Column(name = "comment", columnDefinition = "TEXT")
    private String comment;

    /** IN_PROGRESS / AGREED / REJECTED */
    @Column(name = "status")
    private String status;

    @Column(name = "created_dttm")
    private LocalDateTime createdDttm;

    @Column(name = "resolved_dttm")
    private LocalDateTime resolvedDttm;
}
