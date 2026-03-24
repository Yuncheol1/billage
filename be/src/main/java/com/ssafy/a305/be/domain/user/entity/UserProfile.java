package com.ssafy.a305.be.domain.user.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_profile")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class UserProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_profile_seq")
    private Integer userProfileSeq;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_seq", nullable = false, unique = true)
    private User user;

    @Column(name = "rrn", nullable = false)
    private String rrn;

    @Column(name = "real_name")
    private String realName;

    @Column(name = "phone_verified")
    private Boolean phoneVerified;

    @Column(name = "profile_completed_dt")
    private LocalDate profileCompletedDt;

    @Column(name = "lock_enabled")
    private Boolean lockEnabled;

    /** BIOMETRIC / PATTERN / NONE */
    @Column(name = "lock_type")
    private String lockType;

    @Column(name = "pattern_hash")
    private String patternHash;

    @Column(name = "on_time_count")
    private Integer onTimeCount;

    @Column(name = "overdue_count")
    private Integer overdueCount;

    @Column(name = "resolved_count")
    private Integer resolvedCount;

    @Column(name = "avg_delay_days", precision = 5, scale = 2)
    private BigDecimal avgDelayDays;

    /** BRONZE / SILVER / GOLD / PLATINUM */
    @Column(name = "badge_level")
    private String badgeLevel;

    @Column(name = "badge_is_public")
    private Boolean badgeIsPublic;

    @Column(name = "updated_dttm")
    private LocalDateTime updatedDttm;

    public void syncFromKakao(String realName, Boolean phoneVerified, String rrn) {
        this.realName = realName;
        this.phoneVerified = phoneVerified;
        if ((this.rrn == null || this.rrn.isBlank()) && rrn != null && !rrn.isBlank()) {
            this.rrn = rrn;
        }
        this.updatedDttm = LocalDateTime.now();
    }
}
