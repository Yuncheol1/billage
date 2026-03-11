package com.ssafy.a305.be.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "document")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "document_seq")
    private Long documentSeq;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contract_seq", nullable = false)
    private Contract contract;

    /** CONTRACT_ORIGINAL / WAIVER_AGREEMENT / EARLY_SETTLEMENT / TAX_PACKAGE */
    @Column(name = "doc_type")
    private String docType;

    @Column(name = "version")
    private Integer version;

    @Column(name = "file_url")
    private String fileUrl;

    @Column(name = "sha256_hash")
    private String sha256Hash;

    /** SYSTEM / USER */
    @Column(name = "generated_by")
    private String generatedBy;

    @Column(name = "generated_dttm")
    private LocalDateTime generatedDttm;
}
