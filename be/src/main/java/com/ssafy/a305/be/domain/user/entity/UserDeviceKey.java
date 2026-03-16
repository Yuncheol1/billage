package com.ssafy.a305.be.domain.user.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_device_key")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class UserDeviceKey {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "key_seq")
    private Integer keySeq;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_seq", nullable = false)
    private User user;

    @Column(name = "device_id_hash", nullable = false, length = 255)
    private String deviceIdHash;

    @Column(name = "public_key_pem", nullable = false, columnDefinition = "TEXT")
    private String publicKeyPem;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    @Column(name = "registered_dttm", nullable = false)
    private LocalDateTime registeredDttm;

    @Column(name = "revoked_dttm")
    private LocalDateTime revokedDttm;
}
