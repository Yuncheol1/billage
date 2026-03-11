package com.ssafy.a305.be.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Expo 푸시 알림 토큰 (90일 미사용 토큰 정리용 last_used_dttm)
 */
@Entity
@Table(name = "expo_token")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class ExpoToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "token_seq")
    private Long tokenSeq;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_seq", nullable = false)
    private User user;

    @Column(name = "device_id")
    private String deviceId;

    @Column(name = "token")
    private String token;

    @Column(name = "is_active")
    private Boolean isActive;

    @Column(name = "last_used_dttm")
    private LocalDateTime lastUsedDttm;

    @Column(name = "created_dttm")
    private LocalDateTime createdDttm;

    @Column(name = "invalidated_dttm")
    private LocalDateTime invalidatedDttm;
}
