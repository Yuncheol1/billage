package com.ssafy.a305.be.domain.repayment.service;

import com.ssafy.a305.be.domain.account.entity.BankAccount;
import com.ssafy.a305.be.domain.bank.client.SsafyDemandDepositClient;
import com.ssafy.a305.be.domain.bank.client.dto.response.SsafyDemandDepositTransactionRecDto;
import com.ssafy.a305.be.domain.bank.client.dto.response.SsafyInquireTransactionHistoryListRecDto;
import com.ssafy.a305.be.domain.contract.entity.Contract;
import com.ssafy.a305.be.domain.notification.service.NotificationCommandService;
import com.ssafy.a305.be.domain.repayment.entity.AutoTransferRegistration;
import com.ssafy.a305.be.domain.repayment.entity.RepaymentEvent;
import com.ssafy.a305.be.domain.repayment.entity.RepaymentSchedule;
import com.ssafy.a305.be.domain.repayment.repository.AutoTransferRegistrationRepository;
import com.ssafy.a305.be.domain.repayment.repository.RepaymentEventRepository;
import com.ssafy.a305.be.domain.repayment.repository.RepaymentScheduleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 결과를 모른 채 REQUESTED로 남은 회차를 거래내역과 대조해 확정한다.
 *
 * <p>은행 응답을 받지 못한 이체는 돈이 나갔는지 우리 쪽에서 알 수 없다. 은행이 멱등성을
 * 보장하지 않으므로 다시 보내 볼 수도 없다. 남은 확인 수단은 채무자 계좌의 거래내역뿐이고,
 * 이체할 때 적요에 실어 보낸 회차 식별자({@link RepaymentSchedule#transferMemo()})로
 * 그 회차의 이체를 찾는다.</p>
 *
 * <ul>
 *   <li>일치하는 출금이 1건 → 돈이 나갔다. PAID로 확정한다.</li>
 *   <li>0건 → 나가지 않았다. 이체 전 상태로 되돌려 다음 이체가 다시 시도하게 한다.</li>
 *   <li>2건 이상, 조회 실패 → 판단할 수 없다. REQUESTED로 두고 사람이 확인한다.</li>
 * </ul>
 *
 * <p>거래내역 조회는 외부 호출이므로 트랜잭션 밖에서 하고, 상태 반영만 짧은 트랜잭션으로 묶는다.</p>
 *
 * @see AutoTransferScheduler
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TransferReconciliationService {

    /**
     * 요청 후 이 시간이 지나야 정산 대상이 된다.
     *
     * <p>막 요청한 이체는 아직 은행이 처리 중이라 거래내역에 보이지 않을 수 있다. 이를 "나가지
     * 않았다"로 판정해 되돌리면, 직후 이체가 완료되고 다음 이체가 또 나가 이중 출금이 된다.
     * 이체 API의 응답 대기 시간보다 충분히 길어야 한다.</p>
     */
    public static final Duration RECONCILE_AFTER = Duration.ofMinutes(10);

    private static final DateTimeFormatter SSAFY_DATE = DateTimeFormatter.BASIC_ISO_DATE;

    /** 처리 1건의 결과. */
    public enum Result {
        /** 일치하는 출금을 찾아 PAID로 확정했다. */
        CONFIRMED_PAID,
        /** 출금이 없어 이체 전 상태로 되돌렸다. */
        RELEASED,
        /** 자동으로 판단할 수 없어 REQUESTED로 남겼다. 사람이 확인해야 한다. */
        NEEDS_REVIEW,
        /** 그 사이 상태가 바뀌어 정산할 필요가 없다. */
        SKIPPED
    }

    private final RepaymentScheduleRepository repaymentScheduleRepository;
    private final AutoTransferRegistrationRepository registrationRepository;
    private final RepaymentEventRepository repaymentEventRepository;
    private final NotificationCommandService notificationCommandService;
    private final SsafyDemandDepositClient ssafyDemandDepositClient;
    private final TransactionTemplate transactionTemplate;

    /** 정산할 회차 목록. 요청한 지 {@link #RECONCILE_AFTER} 이상 지난 REQUESTED만 담는다. */
    public List<Integer> findTargets(LocalDateTime now) {
        return repaymentScheduleRepository.findStaleRequestedScheduleSeqs(now.minus(RECONCILE_AFTER));
    }

    /** 회차 1건을 정산한다. */
    public Result reconcile(Integer scheduleSeq) {
        // ── 조회에 필요한 값 읽기 ──
        LookupPlan plan = transactionTemplate.execute(status -> prepare(scheduleSeq));
        if (plan == null) {
            return Result.SKIPPED;
        }
        if (plan.accountNo() == null || plan.userKey() == null || plan.requestedAt() == null) {
            log.warn("[정산] scheduleSeq={} 조회에 필요한 정보(계좌·userKey·요청 시각)가 없어 수동 확인 필요.", scheduleSeq);
            return Result.NEEDS_REVIEW;
        }

        // ── 거래내역 조회: 트랜잭션 밖 ──
        long matched;
        try {
            SsafyInquireTransactionHistoryListRecDto rec = ssafyDemandDepositClient.inquireTransactionHistoryList(
                    plan.userKey(),
                    plan.accountNo(),
                    plan.requestedAt().toLocalDate().format(SSAFY_DATE),
                    LocalDate.now().format(SSAFY_DATE),
                    // 거래 구분 코드는 문서와 실제 값이 엇갈려 전체로 조회하고 적요·금액으로 거른다.
                    "A",
                    "ASC"
            );
            List<SsafyDemandDepositTransactionRecDto> rows =
                    rec == null || rec.list() == null ? List.of() : rec.list();
            Pattern memoPattern = plan.memoPattern();
            matched = rows.stream().filter(row -> isTransferOf(row, memoPattern, plan.amount())).count();
        } catch (RuntimeException e) {
            log.warn("[정산] scheduleSeq={} 거래내역 조회 실패. 다음 실행에서 다시 시도한다. 사유={}",
                    scheduleSeq, e.getMessage());
            return Result.NEEDS_REVIEW;
        }

        // ── 결과 반영 ──
        if (matched == 1) {
            transactionTemplate.executeWithoutResult(status -> confirmPaid(scheduleSeq, plan.amount()));
            log.info("[정산] scheduleSeq={} 출금 확인 → PAID", scheduleSeq);
            return Result.CONFIRMED_PAID;
        }
        if (matched == 0) {
            String restoreStatus = restoreStatusOf(plan.dueDt(), LocalDate.now());
            transactionTemplate.executeWithoutResult(status ->
                    repaymentScheduleRepository.releaseTransferClaim(scheduleSeq, restoreStatus));
            log.info("[정산] scheduleSeq={} 출금 없음 → {}", scheduleSeq, restoreStatus);
            return Result.RELEASED;
        }
        // 같은 회차의 출금이 여러 건이면 이미 이중 출금이 일어났을 수 있다. 자동으로 처리하지 않는다.
        log.error("[정산] scheduleSeq={} 일치하는 출금이 {}건. 이중 출금 여부 수동 확인 필요.", scheduleSeq, matched);
        return Result.NEEDS_REVIEW;
    }

    private LookupPlan prepare(Integer scheduleSeq) {
        RepaymentSchedule schedule = repaymentScheduleRepository
                .findByScheduleSeqWithContractUsers(scheduleSeq)
                .orElse(null);
        if (schedule == null || !"REQUESTED".equals(schedule.getStatus())) {
            return null;
        }
        Contract contract = schedule.getContract();
        // 이체를 보낸 출금 계좌. 그 사이 등록이 바뀌었다면 찾지 못해 수동 확인으로 넘어간다.
        String accountNo = registrationRepository.findActiveWithAccounts(contract.getContractSeq())
                .map(AutoTransferRegistration::getDebtorAccount)
                .map(BankAccount::getAccountNo)
                .orElse(null);
        String userKey = contract.getDebtor().getSsafyUserKey();
        return new LookupPlan(
                scheduleSeq,
                schedule.getTransferRequestedDttm(),
                schedule.getDueDt(),
                schedule.getTotalDue(),
                schedule.transferMemo(),
                accountNo,
                userKey == null || userKey.isBlank() ? null : userKey
        );
    }

    /** 거래내역 한 건이 이 회차의 이체인지 판정한다. */
    private boolean isTransferOf(SsafyDemandDepositTransactionRecDto row, Pattern memoPattern, BigDecimal expectedAmount) {
        String summary = row.transactionSummary();
        if (summary == null || !memoPattern.matcher(summary).find()) {
            return false;
        }
        BigDecimal amount = parseAmount(row.transactionBalance());
        return amount != null && expectedAmount != null && amount.compareTo(expectedAmount) == 0;
    }

    /** 출금 금액은 부호가 붙어 올 수 있어 절댓값으로 비교한다. */
    private BigDecimal parseAmount(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(value.trim().replace(",", "")).abs();
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 출금이 없었을 때 되돌릴 상태.
     *
     * <p>정산 시점에는 이체 전 상태를 알 수 없다. REQUESTED가 아니었다면 연체 스케줄러가 했을
     * 처리와 같게 맞춘다. 납부 기한이 지났으면 연체, 아니면 미납이다.</p>
     */
    static String restoreStatusOf(LocalDate dueDt, LocalDate today) {
        return dueDt != null && dueDt.isBefore(today) ? "OVERDUE" : "NONPAID";
    }

    /** 출금이 확인된 회차를 PAID로 확정한다. 자동이체 성공과 같은 후처리를 한다. */
    private void confirmPaid(Integer scheduleSeq, BigDecimal amount) {
        RepaymentSchedule schedule = repaymentScheduleRepository
                .findByScheduleSeqWithContractUsers(scheduleSeq)
                .orElseThrow(() -> new IllegalStateException("정산 중 회차를 찾을 수 없습니다. scheduleSeq=" + scheduleSeq));
        if (!"REQUESTED".equals(schedule.getStatus())) {
            // 조회하는 사이 다른 경로가 확정했다. 덮어쓰지 않는다.
            return;
        }
        Contract contract = schedule.getContract();

        LocalDateTime now = LocalDateTime.now();
        schedule.markPaid(now);

        RepaymentEvent event = RepaymentEvent.builder()
                .repaymentSchedule(schedule)
                .contract(contract)
                .evidenceGrade("GOLD")
                .amountPaid(amount)
                .reportedByUser(contract.getDebtor())
                .confirmedByUser(contract.getCreditor())
                .status("AUTO_CONFIRMED")
                .note("이체 결과 정산 확인")
                .requestedDttm(schedule.getTransferRequestedDttm())
                .confirmedDttm(now)
                .build();
        repaymentEventRepository.save(event);

        try {
            notificationCommandService.sendRepaymentPaidNotifications(schedule);
        } catch (Exception e) {
            log.error("[정산] 상환 완료 알림 전송 실패 - scheduleSeq={}", scheduleSeq, e);
        }

        boolean allSchedulesCompleted = contract.getRepaymentSchedules().stream()
                .allMatch(item -> "PAID".equals(item.getStatus())
                        || "MANUAL_PAID".equals(item.getStatus())
                        || "WAIVED".equals(item.getStatus()));
        if (allSchedulesCompleted) {
            contract.markClosed(now);
            try {
                notificationCommandService.sendContractClosedNotifications(contract);
            } catch (Exception e) {
                log.error("[정산] 계약 종료 알림 전송 실패 - contractSeq={}", contract.getContractSeq(), e);
            }
        }
    }

    /** 트랜잭션 밖에서 거래내역을 조회하고 판정하는 데 필요한 값만 담는다. */
    private record LookupPlan(
            Integer scheduleSeq,
            LocalDateTime requestedAt,
            LocalDate dueDt,
            BigDecimal amount,
            String memo,
            String accountNo,
            String userKey
    ) {
        /**
         * 적요 안에서 회차 식별자를 찾는 패턴.
         *
         * <p>뒤에 숫자가 이어지면 다른 회차다({@code BL12}와 {@code BL123}). 은행이 적요 앞뒤에
         * 자체 문구를 붙일 수 있어 완전 일치가 아니라 포함 여부로 찾는다.</p>
         */
        Pattern memoPattern() {
            return Pattern.compile(Pattern.quote(memo) + "(?!\\d)");
        }
    }
}
