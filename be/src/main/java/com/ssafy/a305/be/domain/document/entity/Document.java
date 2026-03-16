package com.ssafy.a305.be.domain.document.entity;

import com.ssafy.a305.be.domain.contract.entity.Contract;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

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
    private Integer documentSeq;

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

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "doc_content_json", nullable = false, columnDefinition = "json")
    private String docContentJson;
}
