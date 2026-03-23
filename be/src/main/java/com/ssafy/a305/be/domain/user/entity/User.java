package com.ssafy.a305.be.domain.user.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Entity
@Table(name = "users")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class User {

    private static final DateTimeFormatter DELETED_VALUE_TIMESTAMP_FORMATTER =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_seq")
    private Integer userSeq;

    @Column(name = "email", unique = true)
    private String email;

    @Column(name = "phone", unique = true)
    private String phone;

    /** PENDING / ACTIVE / DELETED / BLOCKED */
    @Column(name = "state")
    private String state;

    @Column(name = "created_dttm")
    private LocalDateTime createdDttm;

    @Column(name = "updated_dttm")
    private LocalDateTime updatedDttm;

    @Column(name = "deleted_dttm")
    private LocalDateTime deletedDttm;

    @OneToOne(mappedBy = "user")
    private UserProfile profile;

    public void changeState(String state) {
        this.state = state;
        this.updatedDttm = LocalDateTime.now();
    }

    public void updateEmail(String email) {
        this.email = email;
        this.updatedDttm = LocalDateTime.now();
    }

    public void updatePhone(String phone) {
        this.phone = phone;
        this.updatedDttm = LocalDateTime.now();
    }

    public void markDeleted() {
        LocalDateTime deletedAt = LocalDateTime.now();
        this.state = "DELETED";
        this.email = appendDeletionSuffix(this.email, deletedAt);
        this.phone = appendDeletionSuffix(this.phone, deletedAt);
        this.deletedDttm = deletedAt;
        this.updatedDttm = deletedAt;
    }

    private String appendDeletionSuffix(String value, LocalDateTime deletedAt) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value + "_del" + deletedAt.format(DELETED_VALUE_TIMESTAMP_FORMATTER);
    }
}
