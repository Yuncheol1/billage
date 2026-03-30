package com.ssafy.a305.be.domain.account.entity;

import com.ssafy.a305.be.domain.enums.BankTransactionDirection;
import com.ssafy.a305.be.domain.repayment.entity.RepaymentSchedule;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "bank_transaction")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class BankTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "tx_seq")
    private Long txSeq;

    @Column(name = "provider")
    private String provider;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_seq", nullable = false)
    private BankAccount bankAccount;

    @Column(name = "tx_dttm")
    private LocalDateTime txDttm;

    @Column(name = "amount", precision = 15, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "direction")
    private BankTransactionDirection direction;

    @Column(name = "counterparty_name")
    private String counterpartyName;

    @Column(name = "memo")
    private String memo;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_payload", columnDefinition = "json")
    private String rawPayload;

    @Column(name = "hash")
    private String hash;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "matched_schedule_seq")
    private RepaymentSchedule matchedSchedule;

    @Column(name = "match_confidence", precision = 5, scale = 4)
    private BigDecimal matchConfidence;

    /** UNMATCHED / MATCHED / IGNORED / PENDING_REVIEW */
    @Column(name = "match_status")
    private String matchStatus;
}
