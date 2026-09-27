package com.ssafy.a305.be.domain.repayment.service;

import com.ssafy.a305.be.domain.contract.entity.Contract;
import com.ssafy.a305.be.domain.notification.service.NotificationCommandService;
import com.ssafy.a305.be.domain.repayment.entity.AutoTransferRegistration;
import com.ssafy.a305.be.domain.repayment.entity.RepaymentEvent;
import com.ssafy.a305.be.domain.repayment.entity.RepaymentSchedule;
import com.ssafy.a305.be.domain.repayment.repository.AutoTransferRegistrationRepository;
import com.ssafy.a305.be.domain.repayment.repository.RepaymentEventRepository;
import com.ssafy.a305.be.domain.repayment.repository.RepaymentScheduleRepository;
import com.ssafy.a305.be.global.exception.TransferFailException;
import com.ssafy.a305.be.global.external.ssafy.SsafyTransferApiClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 자동이체 1건을 처리한다.
 *
 * <p><b>1건 = 1트랜잭션</b>이 이 클래스의 핵심 계약이다. 이체는 은행에서 일어나는
 * 되돌릴 수 없는 부수 효과이므로, 여러 건을 한 트랜잭션으로 묶으면 뒤쪽 건의 실패가
 * 앞쪽 건의 DB 변경을 롤백시켜 "돈은 나갔는데 스케줄은 NONPAID"인 상태를 만든다.
 * 회차마다 트랜잭션을 끊어 그 전파를 차단한다.</p>
 *
 * <p>호출자는 반드시 다른 빈이어야 한다. 같은 클래스 안에서 호출하면 Spring 프록시를
 * 거치지 않아 {@code @Transactional}이 적용되지 않고, 결국 모든 건이 호출자의 단일
 * 트랜잭션에 합류해 위 문제가 그대로 재현된다.</p>
 *
 * @see AutoTransferScheduler
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AutoTransferService {

    /** 처리 1건의 결과. 이체 실패는 예외로 전파되므로 여기에 포함하지 않는다. */
    public enum Result {
        /** 이체와 후처리까지 정상 완료. */
        SUCCEEDED,
        /** 처리 대상이 아니어서 건너뜀(락 점유 중, 등록 없음, 상태 변경됨 등). */
        SKIPPED
    }

    private final RepaymentScheduleRepository repaymentScheduleRepository;
    private final AutoTransferRegistrationRepository registrationRepository;
    private final RepaymentEventRepository repaymentEventRepository;
    private final NotificationCommandService notificationCommandService;
    private final SsafyTransferApiClient ssafyTransferApiClient;
    private final RepaymentTransferRequestGuard repaymentTransferRequestGuard;

    /**
     * 상환 회차 1건에 대해 자동이체를 실행한다.
     *
     * @param scheduleSeq 처리할 상환 회차
     * @return 성공 또는 건너뜀
     * @throws TransferFailException 이체 API가 실패한 경우. 호출자가 건별로 잡아 격리한다.
     */
    @Transactional
    public Result execute(Integer scheduleSeq) {
        String lockToken = repaymentTransferRequestGuard.tryAcquire(scheduleSeq);
        if (lockToken == null) {
            log.info("[자동이체] scheduleSeq={}는 이미 처리 중이어서 Skip.", scheduleSeq);
            return Result.SKIPPED;
        }

        // 트랜잭션이 살아 있으면 커밋/롤백이 끝난 뒤에 해제한다.
        // 커밋 전에 풀면 확정되지 않은 상태를 다른 워커가 읽어 이중 이체가 발생한다.
        boolean releaseInFinally = true;
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCompletion(int status) {
                    repaymentTransferRequestGuard.release(scheduleSeq, lockToken);
                }
            });
            releaseInFinally = false;
        }

        try {
            RepaymentSchedule schedule = repaymentScheduleRepository
                    .findByScheduleSeqWithContractUsers(scheduleSeq)
                    .orElse(null);
            if (schedule == null) {
                log.warn("[자동이체] scheduleSeq={} 회차를 찾을 수 없습니다. Skip.", scheduleSeq);
                return Result.SKIPPED;
            }

            // 대상 목록을 뽑은 뒤 수동 이체 등으로 이미 처리됐을 수 있으므로 다시 확인한다.
            // 건별로 읽기 때문에 가능한 방어이며, 중복 이체를 막는다.
            if (!"NONPAID".equals(schedule.getStatus())) {
                log.info("[자동이체] scheduleSeq={} 상태가 {}(으)로 바뀌어 Skip.",
                        scheduleSeq, schedule.getStatus());
                return Result.SKIPPED;
            }

            Contract contract = schedule.getContract();
            Integer contractSeq = contract.getContractSeq();

            // 1. 자동이체 등록 정보 조회 (계좌 정보)
            AutoTransferRegistration registration = registrationRepository
                    .findActiveWithAccounts(contractSeq)
                    .orElse(null);
            if (registration == null) {
                log.warn("[자동이체] contractSeq={} 에 대한 활성 자동이체 등록이 없습니다. Skip.", contractSeq);
                return Result.SKIPPED;
            }

            String withdrawalAccountNo = registration.getDebtorAccount().getAccountNo();
            String depositAccountNo = registration.getCreditorAccount().getAccountNo();
            BigDecimal amount = schedule.getTotalDue();

            // 적요에 회차 마커를 심는다. 결과를 알 수 없는 호출 이후 이 마커로
            // 거래내역을 뒤져 실제 이체 여부를 판정한다.
            String memo = RepaymentTransferMemo.of(schedule);

            // 2. SSAFY 계좌이체 API 호출 (실패 시 TransferFailException 전파)
            // userKey는 채무자(출금자)의 키를 사용
            String debtorUserKey = contract.getDebtor().getSsafyUserKey();
            ssafyTransferApiClient.transfer(
                    withdrawalAccountNo,
                    depositAccountNo,
                    amount,
                    memo,
                    debtorUserKey
            );

            // 3. 성공 → 상환 스케줄 상태 변경
            LocalDateTime now = LocalDateTime.now();
            schedule.markPaid(now);

            // 4. 상환 이벤트 기록 (evidenceGrade=GOLD, status=AUTO_CONFIRMED)
            RepaymentEvent event = RepaymentEvent.builder()
                    .repaymentSchedule(schedule)
                    .contract(contract)
                    .evidenceGrade("GOLD")
                    .amountPaid(amount)
                    .reportedByUser(contract.getDebtor())
                    .confirmedByUser(contract.getCreditor())
                    .status("AUTO_CONFIRMED")
                    .note("자동이체 처리")
                    .requestedDttm(now)
                    .confirmedDttm(now)
                    .build();
            repaymentEventRepository.save(event);

            sendRepaymentPaidNotificationsSafely(schedule);

            boolean allSchedulesCompleted = contract.getRepaymentSchedules().stream()
                    .allMatch(item -> "PAID".equals(item.getStatus())
                            || "MANUAL_PAID".equals(item.getStatus())
                            || "WAIVED".equals(item.getStatus()));
            if (allSchedulesCompleted) {
                contract.markClosed(now);
                sendContractClosedNotificationsSafely(contract);
            }

            log.info("[자동이체] 성공 - scheduleSeq={}, contractSeq={}, amount={}",
                    scheduleSeq, contractSeq, amount);

            return Result.SUCCEEDED;
        } finally {
            if (releaseInFinally) {
                repaymentTransferRequestGuard.release(scheduleSeq, lockToken);
            }
        }
    }

    // ──────────────── 알림 (실패해도 이체 결과를 롤백하지 않는다) ────────────────

    /**
     * 알림 전송은 이체의 부수 효과일 뿐이므로 실패를 전파하지 않습니다.
     *
     * <p>여기서 예외가 새어나가면 이미 이체가 나간 건의 트랜잭션이 롤백됩니다.
     * 즉 돈은 빠져나갔는데 스케줄은 NONPAID로 남는 상태가 되므로 반드시 삼켜야 합니다.</p>
     */
    private void sendRepaymentPaidNotificationsSafely(RepaymentSchedule schedule) {
        try {
            notificationCommandService.sendRepaymentPaidNotifications(schedule);
        } catch (Exception e) {
            log.error("[자동이체] 상환 완료 알림 전송 실패 - scheduleSeq={}", schedule.getScheduleSeq(), e);
        }
    }

    /** @see #sendRepaymentPaidNotificationsSafely(RepaymentSchedule) */
    private void sendContractClosedNotificationsSafely(Contract contract) {
        try {
            notificationCommandService.sendContractClosedNotifications(contract);
        } catch (Exception e) {
            log.error("[자동이체] 계약 종료 알림 전송 실패 - contractSeq={}", contract.getContractSeq(), e);
        }
    }
}
