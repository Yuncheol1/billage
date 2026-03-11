package com.ssafy.a305.be.domain.entity;

import com.ssafy.a305.be.domain.enums.TermsType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 약관 원본 (SERVICE, PRIVACY, FIN_API, MARKETING 등)
 */
@Entity
@Table(name = "terms_master")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class TermsMaster {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "terms_seq")
    private Long termsSeq;

    @Column(name = "terms_key", nullable = false)
    private String termsKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "terms_type", nullable = false)
    private TermsType termsType;

    @Column(name = "terms_content", nullable = false, columnDefinition = "TEXT")
    private String termsContent;

    @Column(name = "terms_version", nullable = false)
    private String termsVersion;

    @Column(name = "created_dttm", nullable = false)
    private LocalDateTime createdDttm;
}
