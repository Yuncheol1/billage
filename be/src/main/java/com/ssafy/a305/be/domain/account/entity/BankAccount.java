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
    private Integer accountSeq;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_seq", nullable = true)
    private User user;

    /** External account provider (e.g. SSAFY) */
    @Column(name = "provider", nullable = false, length = 30)
    private String provider;

    /** Product unique number used for account opening request */
    @Column(name = "account_type_unique_no", nullable = false, length = 40)
    private String accountTypeUniqueNo;

    /** Provider account identifier */
    @Column(name = "account_no", nullable = false, unique = true, length = 64)
    private String accountNo;

    /** Masked account number for UI display */
    @Column(name = "account_no_masked", length = 64)
    private String accountNoMasked;

    @Column(name = "is_primary")
    private Boolean isPrimary;

    @Column(name = "linked_dttm")
    private LocalDateTime linkedDttm;

    @Column(name = "closed_dttm")
    private LocalDateTime closedDttm;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private BankAccountStatus status;
}