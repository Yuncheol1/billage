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

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (this.isActive == null) {
            this.isActive = true;
        }
        if (this.createdDttm == null) {
            this.createdDttm = now;
        }
        if (this.lastUsedDttm == null) {
            this.lastUsedDttm = now;
        }
    }

    public void deactivate() {
        this.isActive = false;
        this.invalidatedDttm = LocalDateTime.now();
    }

    public void updateToken(String newToken) {
        this.token = newToken;
        this.isActive = true;
        this.lastUsedDttm = LocalDateTime.now();
        this.invalidatedDttm = null;
    }
}
