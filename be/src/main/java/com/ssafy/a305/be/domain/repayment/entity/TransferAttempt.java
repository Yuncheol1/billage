package com.ssafy.a305.be.domain.repayment.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 상환 회차에 대한 이체 시도 원장.
 *
 * <p>은행 API는 멱등성을 보장하지 않고, 타임아웃이 나면 돈이 나갔는지 우리가 알 수 없다.
 * 그래서 외부 호출 <b>이전에</b> 시도를 {@link Status#REQUESTED}로 먼저 커밋해 두고,
 * 결과가 확정됐을 때만 다음 상태로 옮긴다. 응답을 못 받은 시도는 REQUESTED로 남아
 * "결과 불명" 표시가 되고, 그 회차의 새 이체는 차단된다.</p>
 *
 * <p>회차 자체의 상태({@code RepaymentSchedule.status})와 분리한 이유:
 * 납부 여부와 이체 시도 결과는 다른 개념이고, 시도가 여러 번 쌓여도 이력을 잃지 않아야 한다.</p>
 */
@Entity
@Table(
        name = "transfer_attempt",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_transfer_attempt_schedule_no",
                columnNames = {"schedule_seq", "attempt_no"}
        )
)
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class TransferAttempt {

    public enum Status {
        /** 이체를 요청했지만 결과를 확정하지 못한 상태. 새 시도를 막는다. */
        REQUESTED,
        /** 은행이 성공 응답을 줬다. */
        SUCCEEDED,
        /** 은행이 명시적으로 거절했다. 돈이 나가지 않은 것이 확실하다. */
        FAILED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "attempt_seq")
    private Long attemptSeq;

    @Column(name = "schedule_seq", nullable = false)
    private Integer scheduleSeq;

    /** 같은 회차의 몇 번째 시도인지(1부터). 적요 마커에도 실린다. */
    @Column(name = "attempt_no", nullable = false)
    private Integer attemptNo;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private Status status;

    @Column(name = "amount", precision = 15, scale = 2)
    private BigDecimal amount;

    /** 확정 실패일 때 은행 응답코드. */
    @Column(name = "response_code", length = 20)
    private String responseCode;

    @Column(name = "requested_dttm", nullable = false)
    private LocalDateTime requestedDttm;

    @Column(name = "resolved_dttm")
    private LocalDateTime resolvedDttm;

    public static TransferAttempt open(Integer scheduleSeq, int attemptNo, BigDecimal amount, LocalDateTime now) {
        return TransferAttempt.builder()
                .scheduleSeq(scheduleSeq)
                .attemptNo(attemptNo)
                .status(Status.REQUESTED)
                .amount(amount)
                .requestedDttm(now)
                .build();
    }

    public void markSucceeded(LocalDateTime now) {
        this.status = Status.SUCCEEDED;
        this.resolvedDttm = now;
    }

    public void markFailed(String responseCode, LocalDateTime now) {
        this.status = Status.FAILED;
        this.responseCode = responseCode;
        this.resolvedDttm = now;
    }
}
