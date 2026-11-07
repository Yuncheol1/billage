package com.ssafy.a305.be.domain.repayment.entity;

import com.ssafy.a305.be.domain.account.entity.BankTransaction;
import com.ssafy.a305.be.domain.contract.entity.Contract;
import com.ssafy.a305.be.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/*
 * 회차당 AUTO_CONFIRMED 이벤트는 1건만 허용한다. 생성 컬럼 기반 유니크 인덱스라 엔티티에는 선언하지 않는다.
 * db/manual/20261013_add_repayment_event_auto_confirmed_unique.sql
 */
@Entity
@Table(name = "repayment_event")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class RepaymentEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "event_seq")
    private Long eventSeq;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_seq", nullable = false)
    private RepaymentSchedule repaymentSchedule;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contract_seq", nullable = false)
    private Contract contract;

    /** BRONZE / SILVER / GOLD / WAIVER */
    @Column(name = "evidence_grade")
    private String evidenceGrade;

    @Column(name = "amount_paid", precision = 15, scale = 2)
    private BigDecimal amountPaid;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reported_by_user_seq", nullable = false)
    private User reportedByUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "confirmed_by_user_seq", nullable = false)
    private User confirmedByUser;

    /** REQUESTED / CONFIRMED / REJECTED / AUTO_CONFIRMED / FAILED */
    @Column(name = "status")
    private String status;

    @Column(name = "note")
    private String note;

    @Column(name = "requested_dttm")
    private LocalDateTime requestedDttm;

    @Column(name = "confirmed_dttm")
    private LocalDateTime confirmedDttm;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tx_seq")
    private BankTransaction bankTransaction;

    @Column(name = "Field")
    private String field;
}
