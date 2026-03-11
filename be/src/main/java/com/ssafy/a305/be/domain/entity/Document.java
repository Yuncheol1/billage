package com.ssafy.a305.be.domain.entity;

import com.ssafy.a305.be.domain.enums.DocumentType;
import com.ssafy.a305.be.domain.enums.GeneratedBy;
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

    @Enumerated(EnumType.STRING)
    @Column(name = "doc_type")
    private DocumentType docType;

    @Column(name = "version")
    private Integer version;

    @Column(name = "file_url")
    private String fileUrl;

    @Column(name = "sha256_hash")
    private String sha256Hash;

    @Enumerated(EnumType.STRING)
    @Column(name = "generated_by")
    private GeneratedBy generatedBy;

    @Column(name = "generated_dttm")
    private LocalDateTime generatedDttm;
}
