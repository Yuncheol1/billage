package com.ssafy.a305.be.domain.entity;

import com.ssafy.a305.be.domain.enums.UserStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "user")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_seq")
    private Long userSeq;

    @Column(name = "email", unique = true)
    private String email;

    @Column(name = "phone", unique = true)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(name = "state")  // ERD 컬럼명: state (ACTIVE / DELETED / BLOCKED)
    private UserStatus state;

    @Column(name = "created_dttm")
    private LocalDateTime createdDttm;

    @Column(name = "updated_dttm")
    private LocalDateTime updatedDttm;

    @Column(name = "deleted_dttm")
    private LocalDateTime deletedDttm;

    public void changeState(UserStatus state) {
        this.state = state;
    }

    public void updateEmail(String email) {
        this.email = email;
    }

    public void updatePhone(String phone) {
        this.phone = phone;
    }

    /** 탈퇴 처리 시 deleted_dttm 설정 */
    public void markDeleted() {
        this.state = UserStatus.DELETED;
        this.deletedDttm = LocalDateTime.now();
    }
}