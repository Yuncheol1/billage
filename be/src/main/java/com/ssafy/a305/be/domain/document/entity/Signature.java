package com.ssafy.a305.be.domain.document.entity;

import com.ssafy.a305.be.domain.enums.SignatureRole;
import com.ssafy.a305.be.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Table(name = "signature")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Signature {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "signature_seq")
    private Long signatureSeq;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_seq", nullable = false)
    private Document document;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_seq", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "role")
    private SignatureRole role;

    /** BIOMETRIC / PATTERN / DEVICE_ID */
    @Column(name = "method")
    private String method;

    @Column(name = "device_id_hash")
    private String deviceIdHash;

    @Column(name = "biometric_verified")
    private Boolean biometricVerified;

    @Column(name = "ip_address")
    private String ipAddress;

    @Column(name = "user_agent")
    private String userAgent;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "signature_payload", columnDefinition = "json")
    private String signaturePayload;

    @Column(name = "signed_dttm")
    private LocalDateTime signedDttm;
}
