package com.ssafy.a305.be.domain.notification.entity;

import com.ssafy.a305.be.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "fcm_token")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class FcmToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "token_seq")
    private Long tokenSeq;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_seq", nullable = false)
    private User user;

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

    public void deactivate() {
        this.isActive = false;
        this.invalidatedDttm = LocalDateTime.now();
    }
}
