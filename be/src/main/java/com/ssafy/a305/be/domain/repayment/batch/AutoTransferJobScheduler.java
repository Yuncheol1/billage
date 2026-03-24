package com.ssafy.a305.be.domain.repayment.batch;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * 자동이체 배치 스케줄러.
 *
 * <p>매일 10시, 14시, 18시에 autoTransferJob을 트리거합니다.</p>
 * <p>JobParameters에 runDate + slot 조합을 넣어
 * JobRepository가 동일 날짜+slot의 중복 실행을 자동 방어합니다.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AutoTransferJobScheduler {

    private final JobLauncher jobLauncher;
    private final Job autoTransferJob;

    /** 1차 이체: 매일 오전 10시 */
    @Scheduled(cron = "0 0 10 * * *")
    public void runSlot10() {
        runJob("SLOT_10");
    }

    /** 2차 재시도: 매일 오후 2시 */
    @Scheduled(cron = "0 0 14 * * *")
    public void runSlot14() {
        runJob("SLOT_14");
    }

    /** 3차 재시도: 매일 오후 6시 */
    @Scheduled(cron = "0 0 18 * * *")
    public void runSlot18() {
        runJob("SLOT_18");
    }

    private void runJob(String slot) {
        try {
            JobParameters params = new JobParametersBuilder()
                    .addString("runDate", LocalDate.now().toString())
                    .addString("slot", slot)
                    .toJobParameters();

            log.info("[자동이체 스케줄러] Job 실행 - runDate={}, slot={}", LocalDate.now(), slot);
            jobLauncher.run(autoTransferJob, params);

        } catch (Exception e) {
            log.error("[자동이체 스케줄러] Job 실행 실패 - slot={}, 사유: {}", slot, e.getMessage(), e);
        }
    }
}
