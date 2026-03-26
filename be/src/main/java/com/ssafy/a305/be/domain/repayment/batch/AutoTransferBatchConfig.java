package com.ssafy.a305.be.domain.repayment.batch;

import com.ssafy.a305.be.domain.repayment.entity.AutoTransferRegistration;
import com.ssafy.a305.be.domain.repayment.entity.RepaymentEvent;
import com.ssafy.a305.be.domain.repayment.entity.RepaymentSchedule;
import com.ssafy.a305.be.domain.notification.service.NotificationCommandService;
import com.ssafy.a305.be.domain.repayment.repository.AutoTransferRegistrationRepository;
import com.ssafy.a305.be.domain.repayment.repository.RepaymentEventRepository;
import com.ssafy.a305.be.global.exception.TransferFailException;
import com.ssafy.a305.be.global.external.ssafy.SsafyTransferApiClient;
import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.database.JpaCursorItemReader;
import org.springframework.batch.item.database.JpaItemWriter;
import org.springframework.batch.item.database.builder.JpaCursorItemReaderBuilder;
import org.springframework.batch.item.database.builder.JpaItemWriterBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 자동이체 Spring Batch 설정.
 *
 * <p>Chunk(100건) 기반으로 NONPAID 상환 스케줄을 읽어
 * SSAFY 이체 API를 호출하고, 결과에 따라 상태를 변경합니다.</p>
 *
 * <p>FaultTolerant 설정으로 개별 건 실패(TransferFailException) 시
 * 해당 건만 Skip하고 나머지 건은 정상 처리됩니다.</p>
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class AutoTransferBatchConfig {

    private static final int CHUNK_SIZE = 100;

    private final EntityManagerFactory entityManagerFactory;
    private final SsafyTransferApiClient ssafyTransferApiClient;
    private final AutoTransferRegistrationRepository registrationRepository;
    private final RepaymentEventRepository repaymentEventRepository;
    private final NotificationCommandService notificationCommandService;

    // ──────────────── Job ────────────────

    @Bean
    public Job autoTransferJob(JobRepository jobRepository, Step autoTransferStep) {
        return new JobBuilder("autoTransferJob", jobRepository)
                .start(autoTransferStep)
                .build();
    }

    // ──────────────── Step ────────────────

    @Bean
    public Step autoTransferStep(JobRepository jobRepository,
                                 PlatformTransactionManager transactionManager) {
        return new StepBuilder("autoTransferStep", jobRepository)
                .<RepaymentSchedule, RepaymentSchedule>chunk(CHUNK_SIZE, transactionManager)
                .reader(autoTransferReader())
                .processor(autoTransferProcessor())
                .writer(autoTransferWriter())
                .faultTolerant()
                .skip(TransferFailException.class)
                .skipLimit(Integer.MAX_VALUE)
                .build();
    }

    // ──────────────── Reader ────────────────

    @Bean
    @StepScope
    public JpaCursorItemReader<RepaymentSchedule> autoTransferReader() {
        return new JpaCursorItemReaderBuilder<RepaymentSchedule>()
                .name("autoTransferReader")
                .entityManagerFactory(entityManagerFactory)
                .queryString(
                        "SELECT rs FROM RepaymentSchedule rs " +
                        "JOIN FETCH rs.contract c " +
                        "JOIN FETCH c.debtor " +
                        "JOIN FETCH c.creditor " +
                        "WHERE rs.dueDt <= :today " +
                        "AND rs.status = 'NONPAID' " +
                        "AND c.status in ('SIGNED', 'ACTIVE', 'OVERDUE')")
                .parameterValues(Map.of("today", LocalDate.now()))
                .build();
    }

    // ──────────────── Processor ────────────────

    @Bean
    public ItemProcessor<RepaymentSchedule, RepaymentSchedule> autoTransferProcessor() {
        return schedule -> {
            Integer contractSeq = schedule.getContract().getContractSeq();

            // 1. 자동이체 등록 정보 조회 (계좌 정보)
            AutoTransferRegistration registration = registrationRepository
                    .findActiveWithAccounts(contractSeq)
                    .orElse(null);

            if (registration == null) {
                log.warn("[자동이체] contractSeq={} 에 대한 활성 자동이체 등록이 없습니다. Skip.", contractSeq);
                return null; // null 반환 → Writer에 전달되지 않음 (필터링)
            }

            String withdrawalAccountNo = registration.getDebtorAccount().getAccountNo();
            String depositAccountNo = registration.getCreditorAccount().getAccountNo();
            BigDecimal amount = schedule.getTotalDue();
            String contractId = schedule.getContract().getContractId();

            // 2. SSAFY 계좌이체 API 호출 (실패 시 TransferFailException → Skip)
            // userKey는 채무자(출금자)의 키를 사용
            String debtorUserKey = schedule.getContract().getDebtor().getSsafyUserKey();
            ssafyTransferApiClient.transfer(
                    withdrawalAccountNo,
                    depositAccountNo,
                    amount,
                    contractId,
                    debtorUserKey
            );

            // 3. 성공 → 상환 스케줄 상태 변경
            LocalDateTime now = LocalDateTime.now();
            schedule.markPaid(now);

            // 4. 상환 이벤트 기록 (evidenceGrade=GOLD, status=AUTO_CONFIRMED)
            RepaymentEvent event = RepaymentEvent.builder()
                    .repaymentSchedule(schedule)
                    .contract(schedule.getContract())
                    .evidenceGrade("GOLD")
                    .amountPaid(amount)
                    .reportedByUser(schedule.getContract().getDebtor())
                    .confirmedByUser(schedule.getContract().getCreditor())
                    .status("AUTO_CONFIRMED")
                    .note("자동이체 처리")
                    .requestedDttm(now)
                    .confirmedDttm(now)
                    .build();
            repaymentEventRepository.save(event);

            notificationCommandService.sendRepaymentPaidNotifications(schedule);

            boolean allSchedulesCompleted = schedule.getContract().getRepaymentSchedules().stream()
                    .allMatch(item -> "PAID".equals(item.getStatus())
                            || "MANUAL_PAID".equals(item.getStatus())
                            || "WAIVED".equals(item.getStatus()));
            if (allSchedulesCompleted) {
                schedule.getContract().markClosed(now);
                notificationCommandService.sendContractClosedNotifications(schedule.getContract());
            }

            log.info("[자동이체] 성공 - scheduleSeq={}, contractSeq={}, amount={}",
                    schedule.getScheduleSeq(), contractSeq, amount);

            return schedule;
        };
    }

    // ──────────────── Writer ────────────────

    @Bean
    public JpaItemWriter<RepaymentSchedule> autoTransferWriter() {
        return new JpaItemWriterBuilder<RepaymentSchedule>()
                .entityManagerFactory(entityManagerFactory)
                .build();
    }
}
