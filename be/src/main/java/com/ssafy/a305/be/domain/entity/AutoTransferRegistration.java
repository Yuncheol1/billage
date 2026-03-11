package com.ssafy.a305.be.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "auto_transfer_registration")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class AutoTransferRegistration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "registration_seq")
    private Long registrationSeq;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contract_seq", nullable = false)
    private Contract contract;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "debtor_account_seq", nullable = false)
    private BankAccount debtorAccount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creditor_account_seq", nullable = false)
    private BankAccount creditorAccount;

    @Column(name = "amount", precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(name = "transfer_day")
    private Integer transferDay;

    @Column(name = "is_active")
    private Boolean isActive;

    @Column(name = "registered_dttm")
    private LocalDateTime registeredDttm;

    @Column(name = "canceled_dttm")
    private LocalDateTime canceledDttm;
}
