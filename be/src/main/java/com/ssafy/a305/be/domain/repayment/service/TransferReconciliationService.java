package com.ssafy.a305.be.domain.repayment.service;

import com.ssafy.a305.be.domain.account.entity.BankAccount;
import com.ssafy.a305.be.domain.bank.client.SsafyDemandDepositClient;
import com.ssafy.a305.be.domain.bank.client.dto.response.SsafyDemandDepositTransactionRecDto;
import com.ssafy.a305.be.domain.bank.client.dto.response.SsafyInquireTransactionHistoryListRecDto;
import com.ssafy.a305.be.domain.contract.entity.Contract;
import com.ssafy.a305.be.domain.notification.service.NotificationCommandService;
import com.ssafy.a305.be.domain.repayment.dto.response.UnresolvedTransferResponseDto;
import com.ssafy.a305.be.domain.repayment.entity.AutoTransferRegistration;
import com.ssafy.a305.be.domain.repayment.entity.RepaymentEvent;
import com.ssafy.a305.be.domain.repayment.entity.RepaymentSchedule;
import com.ssafy.a305.be.domain.repayment.entity.RepaymentScheduleChangeReason;
import com.ssafy.a305.be.domain.repayment.repository.AutoTransferRegistrationRepository;
import com.ssafy.a305.be.domain.repayment.repository.RepaymentEventRepository;
import com.ssafy.a305.be.domain.repayment.repository.RepaymentScheduleRepository;
import com.ssafy.a305.be.global.exception.CustomBusinessException;
import com.ssafy.a305.be.global.exception.dto.ErrorCode;
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
 * 결과를 모르는 이체(REQUESTED)를 채무자 거래내역과 대조해 확정한다.
 * 적요의 회차 표식과 금액이 맞는 출금이 1건이면 PAID, 0건이면 되돌림, 그 외는 수동 확인.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TransferReconciliationService {

    // 막 보낸 이체는 거래내역에 아직 안 보일 수 있다. 이체 응답 대기(10초)보다 충분히 길게 둔다.
    public static final Duration RECONCILE_AFTER = Duration.ofMinutes(10);

    private static final DateTimeFormatter SSAFY_DATE = DateTimeFormatter.BASIC_ISO_DATE;

    public enum Result {
        CONFIRMED_PAID,
        RELEASED,
        NEEDS_REVIEW, // REQUESTED 유지, 사람이 확인
        SKIPPED       // 그 사이 다른 경로가 상태를 바꿈
    }

    private final RepaymentScheduleRepository repaymentScheduleRepository;
    private final AutoTransferRegistrationRepository registrationRepository;
    private final RepaymentEventRepository repaymentEventRepository;
    private final NotificationCommandService notificationCommandService;
    private final SsafyDemandDepositClient ssafyDemandDepositClient;
    private final TransactionTemplate transactionTemplate;
    private final RepaymentScheduleHistoryRecorder historyRecorder;

    public List<Integer> findTargets(LocalDateTime now) {
        return repaymentScheduleRepository.findStaleRequestedScheduleSeqs(now.minus(RECONCILE_AFTER));
    }

    public Result reconcile(Integer scheduleSeq) {
        LookupPlan plan = transactionTemplate.execute(status -> prepare(scheduleSeq));
        if (plan == null) {
            return Result.SKIPPED;
        }
        if (plan.accountNo() == null || plan.userKey() == null || plan.requestedAt() == null) {
            log.warn("[정산] scheduleSeq={} 조회에 필요한 정보(계좌·userKey·요청 시각)가 없어 수동 확인 필요.", scheduleSeq);
            return Result.NEEDS_REVIEW;
        }

        // 외부 조회는 트랜잭션 밖에서
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

        if (matched == 1) {
            Boolean confirmed = transactionTemplate.execute(
                    status -> confirmPaid(scheduleSeq, plan.amount(), "이체 결과 정산 확인",
                            RepaymentScheduleChangeReason.RECONCILED_PAID));
            if (!Boolean.TRUE.equals(confirmed)) {
                return Result.SKIPPED;
            }
            log.info("[정산] scheduleSeq={} 출금 확인 → PAID", scheduleSeq);
            return Result.CONFIRMED_PAID;
        }
        if (matched == 0) {
            String restoreStatus = restoreStatusOf(plan.dueDt(), LocalDate.now());
            transactionTemplate.executeWithoutResult(status ->
                    release(scheduleSeq, restoreStatus, RepaymentScheduleChangeReason.RECONCILED_NOT_WITHDRAWN,
                            "거래내역에 일치하는 출금 없음"));
            log.info("[정산] scheduleSeq={} 출금 없음 → {}", scheduleSeq, restoreStatus);
            return Result.RELEASED;
        }
        // 2건 이상이면 이중 출금일 수 있어 자동으로 처리하지 않는다.
        log.error("[정산] scheduleSeq={} 일치하는 출금이 {}건. 이중 출금 여부 수동 확인 필요.", scheduleSeq, matched);
        return Result.NEEDS_REVIEW;
    }

    /** 운영자가 직접 확인할 미해결 회차 목록 */
    public List<UnresolvedTransferResponseDto> findUnresolved(LocalDateTime now) {
        return transactionTemplate.execute(status -> repaymentScheduleRepository
                .findStaleRequestedWithContract(now.minus(RECONCILE_AFTER))
                .stream()
                .map(schedule -> new UnresolvedTransferResponseDto(
                        schedule.getScheduleSeq(),
                        schedule.getContract().getContractSeq(),
                        schedule.getContract().getContractId(),
                        schedule.getInstallmentNo(),
                        schedule.getTotalDue(),
                        schedule.getDueDt(),
                        schedule.getTransferRequestedDttm(),
                        schedule.transferMemo()))
                .toList());
    }

    /** 운영자 확인 결과로 확정한다. withdrawn이면 PAID, 아니면 되돌림. */
    public Result resolveByReview(Integer scheduleSeq, boolean withdrawn, String note, LocalDateTime now) {
        if (note == null || note.isBlank()) {
            throw new CustomBusinessException(ErrorCode.INVALID_INPUT_VALUE, "확인 근거(note)를 입력해야 합니다.");
        }
        return transactionTemplate.execute(status -> {
            RepaymentSchedule schedule = repaymentScheduleRepository.findByScheduleSeqWithContractUsers(scheduleSeq)
                    .orElseThrow(() -> new CustomBusinessException(ErrorCode.INVALID_INPUT_VALUE, "회차를 찾을 수 없습니다."));
            if (!"REQUESTED".equals(schedule.getStatus())) {
                log.info("[정산 확인] scheduleSeq={} 이미 {} 상태라 건너뜀", scheduleSeq, schedule.getStatus());
                return Result.SKIPPED;
            }
            // 은행이 아직 처리 중일 수 있는 이체를 "출금 없음"으로 되돌리면 이중 출금이 된다.
            LocalDateTime requestedAt = schedule.getTransferRequestedDttm();
            if (requestedAt != null && requestedAt.isAfter(now.minus(RECONCILE_AFTER))) {
                throw new CustomBusinessException(ErrorCode.INVALID_INPUT_VALUE,
                        "이체 요청 후 %d분이 지나지 않아 은행이 아직 처리 중일 수 있습니다."
                                .formatted(RECONCILE_AFTER.toMinutes()));
            }

            if (withdrawn) {
                if (!confirmPaid(scheduleSeq, schedule.getTotalDue(), "운영자 확인: " + note,
                        RepaymentScheduleChangeReason.REVIEWED_PAID)) {
                    return Result.SKIPPED;
                }
                log.info("[정산 확인] scheduleSeq={} 출금 확인 → PAID. 근거={}", scheduleSeq, note);
                return Result.CONFIRMED_PAID;
            }
            String restoreStatus = restoreStatusOf(schedule.getDueDt(), now.toLocalDate());
            release(scheduleSeq, restoreStatus, RepaymentScheduleChangeReason.REVIEWED_NOT_WITHDRAWN,
                    "운영자 확인: " + note);
            log.info("[정산 확인] scheduleSeq={} 출금 없음 → {}. 근거={}", scheduleSeq, restoreStatus, note);
            return Result.RELEASED;
        });
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

    private boolean isTransferOf(SsafyDemandDepositTransactionRecDto row, Pattern memoPattern, BigDecimal expectedAmount) {
        String summary = row.transactionSummary();
        if (summary == null || !memoPattern.matcher(summary).find()) {
            return false;
        }
        BigDecimal amount = parseAmount(row.transactionBalance());
        return amount != null && expectedAmount != null && amount.compareTo(expectedAmount) == 0;
    }

    // 출금 금액에 부호가 붙어 올 수 있어 절댓값으로 비교
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

    // 이체 전 상태는 남아 있지 않으므로 연체 스케줄러와 같은 기준으로 정한다.
    static String restoreStatusOf(LocalDate dueDt, LocalDate today) {
        return dueDt != null && dueDt.isBefore(today) ? "OVERDUE" : "NONPAID";
    }

    private void release(Integer scheduleSeq, String restoreStatus, RepaymentScheduleChangeReason reason, String detail) {
        if (repaymentScheduleRepository.releaseTransferClaim(scheduleSeq, restoreStatus) == 1) {
            historyRecorder.record(scheduleSeq, "REQUESTED", restoreStatus, reason, null, detail);
        }
    }

    /** 다른 경로가 먼저 확정했으면 false를 반환하고 아무것도 하지 않는다. */
    private boolean confirmPaid(Integer scheduleSeq, BigDecimal amount, String note,
                                RepaymentScheduleChangeReason reason) {
        LocalDateTime now = LocalDateTime.now();
        if (repaymentScheduleRepository.markPaidIfRequested(scheduleSeq, now) == 0) {
            log.info("[정산] scheduleSeq={} 다른 경로가 먼저 확정해 건너뜀", scheduleSeq);
            return false;
        }
        historyRecorder.record(scheduleSeq, "REQUESTED", "PAID", reason, null, note);
        RepaymentSchedule schedule = repaymentScheduleRepository
                .findByScheduleSeqWithContractUsers(scheduleSeq)
                .orElseThrow(() -> new IllegalStateException("정산 중 회차를 찾을 수 없습니다. scheduleSeq=" + scheduleSeq));
        Contract contract = schedule.getContract();

        RepaymentEvent event = RepaymentEvent.builder()
                .repaymentSchedule(schedule)
                .contract(contract)
                .evidenceGrade("GOLD")
                .amountPaid(amount)
                .reportedByUser(contract.getDebtor())
                .confirmedByUser(contract.getCreditor())
                .status("AUTO_CONFIRMED")
                .note(note)
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
        return true;
    }

    private record LookupPlan(
            Integer scheduleSeq,
            LocalDateTime requestedAt,
            LocalDate dueDt,
            BigDecimal amount,
            String memo,
            String accountNo,
            String userKey
    ) {
        // BL12가 BL123에 걸리지 않게 뒤에 숫자가 오면 제외. 은행이 문구를 덧붙일 수 있어 포함 여부로 찾는다.
        Pattern memoPattern() {
            return Pattern.compile(Pattern.quote(memo) + "(?!\\d)");
        }
    }
}
