package com.ssafy.a305.be.domain.repayment.service;

import com.ssafy.a305.be.domain.repayment.repository.RepaymentScheduleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

/**
 * 자동이체 스케줄러.
 *
 * <p>매일 10시·14시·18시에 미납 회차를 훑어 {@link AutoTransferService#execute(Integer)}를
 * 건별로 호출합니다. 성공한 회차는 PAID로 바뀌어 다음 슬롯의 대상에서 자동으로 빠지고,
 * 실패한 회차는 NONPAID로 남아 자동으로 다시 걸립니다. 즉 <b>도메인 상태가 곧 진행
 * 체크포인트</b>이므로 "어디까지 처리했는지"를 따로 저장할 필요가 없습니다.</p>
 *
 * <p>이 클래스에는 {@code @Transactional}을 붙이지 않습니다. 붙이면 전체 반복이 하나의
 * 트랜잭션이 되어 한 건의 실패가 나머지 전부를 롤백시킵니다. 트랜잭션 경계는 회차 1건을
 * 처리하는 {@link AutoTransferService} 안에서 구간별로 직접 관리합니다.</p>
 *
 * <p>결과를 알 수 없어 미해결로 남은 회차({@code UNRESOLVED})와 그 때문에 진입이 차단된 회차
 * ({@code BLOCKED})는 자동으로 풀리지 않으므로, 로그에서 따로 세어 눈에 띄게 남깁니다.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AutoTransferScheduler {

    private final RepaymentScheduleRepository repaymentScheduleRepository;

    /** 회차 1건 처리. 트랜잭션 경계는 이 서비스가 구간별로 직접 관리한다. */
    private final AutoTransferService autoTransferService;

    /** 1차 이체 10시, 2·3차 재시도 14시·18시. 남은 NONPAID만 다시 대상이 된다. */
    @Scheduled(cron = "0 0 10,14,18 * * *")
    public void runAutoTransfer() {
        LocalDate today = LocalDate.now();
        List<Integer> targets = repaymentScheduleRepository.findDueScheduleSeqs(today);

        if (targets.isEmpty()) {
            log.info("[자동이체] 대상 없음 - date={}", today);
            return;
        }

        long startedAt = System.currentTimeMillis();
        int succeeded = 0;
        int skipped = 0;
        int blocked = 0;
        int unresolved = 0;
        int failed = 0;

        for (Integer scheduleSeq : targets) {
            try {
                // 한 건의 실패가 다음 건으로 번지지 않도록 여기서 잡는다.
                switch (autoTransferService.execute(scheduleSeq)) {
                    case SUCCEEDED -> succeeded++;
                    case SKIPPED -> skipped++;
                    case BLOCKED -> blocked++;
                    case UNRESOLVED -> unresolved++;
                }
            } catch (Exception e) {
                failed++;
                log.error("[자동이체] 처리 실패 - scheduleSeq={}, 사유={}",
                        scheduleSeq, e.getMessage(), e);
            }
        }

        log.info("[자동이체] 완료 - date={}, 대상={}, 성공={}, 스킵={}, 실패={}, 소요={}ms",
                today, targets.size(), succeeded, skipped, failed,
                System.currentTimeMillis() - startedAt);

        if (blocked > 0 || unresolved > 0) {
            // 결과가 확정되지 않은 이체가 있다. 자동으로 풀리지 않으므로 확인이 필요하다.
            log.warn("[자동이체] 결과 미확정 - 이번 실행에서 결과 불명={}, 미해결로 진입 차단={}", unresolved, blocked);
        }
    }
}
