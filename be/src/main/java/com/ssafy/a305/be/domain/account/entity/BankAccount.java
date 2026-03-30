package com.ssafy.a305.be.domain.account.entity;

import com.ssafy.a305.be.domain.enums.BankAccountStatus;
import com.ssafy.a305.be.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "bank_account")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class BankAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "account_seq")
    private Long accountSeq;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_seq", nullable = false)
    private User user;

    @Column(name = "provider")
    private String provider;

    @Column(name = "bank_code")
    private String bankCode;

    @Column(name = "account_number_masked")
    private String accountNumberMasked;

    @Column(name = "account_token")
    private String accountToken;

    @Column(name = "account_holder_name")
    private String accountHolderName;

    @Column(name = "is_primary")
    private Boolean isPrimary;

    @Column(name = "linked_dttm")
    private LocalDateTime linkedDttm;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private BankAccountStatus status;
}
