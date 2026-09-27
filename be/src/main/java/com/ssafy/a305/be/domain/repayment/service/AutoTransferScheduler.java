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
 * 트랜잭션이 되어 한 건의 실패가 나머지 전부를 롤백시킵니다. 트랜잭션 경계는
 * {@code AutoTransferService} 쪽에만 있어야 합니다.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AutoTransferScheduler {

    private final RepaymentScheduleRepository repaymentScheduleRepository;

    /** 별도 빈이어야 프록시를 거쳐 건별 트랜잭션이 적용된다. */
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
        int failed = 0;

        for (Integer scheduleSeq : targets) {
            try {
                // 건별 트랜잭션. 한 건의 실패가 다음 건으로 번지지 않도록 여기서 잡는다.
                switch (autoTransferService.execute(scheduleSeq)) {
                    case SUCCEEDED -> succeeded++;
                    case SKIPPED -> skipped++;
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
    }
}
