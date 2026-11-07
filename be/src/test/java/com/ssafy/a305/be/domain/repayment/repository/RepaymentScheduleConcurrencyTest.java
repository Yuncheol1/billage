package com.ssafy.a305.be.domain.repayment.repository;

import com.ssafy.a305.be.domain.contract.entity.Contract;
import com.ssafy.a305.be.domain.contract.repository.ContractRepository;
import com.ssafy.a305.be.domain.repayment.entity.RepaymentEvent;
import com.ssafy.a305.be.domain.repayment.entity.RepaymentSchedule;
import com.ssafy.a305.be.domain.user.entity.User;
import com.ssafy.a305.be.domain.user.repository.UserRepository;
import com.ssafy.a305.be.global.config.querydsl.QuerydslConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 실제 MySQL에서 회차 상태 조건부 UPDATE가 동시 요청 중 하나만 통과시키는지 확인한다.
 * Mock 테스트는 UPDATE 결과(0/1)를 직접 정해 주므로 DB가 실제로 그렇게 동작하는지는 보지 못한다.
 * Docker가 없으면 건너뛴다.
 */
@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(QuerydslConfig.class)
@Testcontainers(disabledWithoutDocker = true)
@Transactional(propagation = Propagation.NOT_SUPPORTED) // 스레드마다 각자 트랜잭션을 커밋해야 경합이 생긴다
class RepaymentScheduleConcurrencyTest {

    private static final int THREADS = 10;
    private static final int ROUNDS = 30;
    private static final AtomicInteger SEQ = new AtomicInteger();

    @Container
    @ServiceConnection
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0.36");

    @Autowired
    private RepaymentScheduleRepository scheduleRepository;
    @Autowired
    private RepaymentEventRepository eventRepository;
    @Autowired
    private ContractRepository contractRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PlatformTransactionManager transactionManager;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private TransactionTemplate tx;
    private Contract contract;
    private User creditor;

    @BeforeEach
    void setUp() {
        tx = new TransactionTemplate(transactionManager);
        tx.executeWithoutResult(s -> {
            creditor = userRepository.save(user());
            User debtor = userRepository.save(user());
            contract = contractRepository.save(Contract.builder()
                    .contractId("C-TEST-" + SEQ.incrementAndGet())
                    .creditor(creditor)
                    .debtor(debtor)
                    .principalAmount(new BigDecimal("1000000"))
                    .interestRate(BigDecimal.ZERO)
                    .repaymentMethod("MONTHLY")
                    .totalInstallments(1)
                    .startDt(LocalDate.now().minusMonths(1))
                    .endDt(LocalDate.now())
                    .status("SIGNED")
                    .build());
        });
    }

    @Test
    @DisplayName("10개 요청이 같은 회차를 동시에 선점하면 정확히 1개만 이긴다")
    void concurrentClaims_onlyOneWins() throws Exception {
        for (int round = 0; round < ROUNDS; round++) {
            Integer seq = newSchedule("NONPAID");

            List<Integer> results = race(THREADS, i -> () ->
                    tx.execute(s -> scheduleRepository.claimForTransfer(seq, "NONPAID", LocalDateTime.now())));

            assertThat(results).as("round %d", round).containsOnly(0, 1);
            assertThat(results.stream().mapToInt(Integer::intValue).sum()).as("round %d", round).isEqualTo(1);
            assertThat(statusOf(seq)).isEqualTo("REQUESTED");
        }
    }

    @Test
    @DisplayName("이체 선점과 채권자 수동 완료가 동시에 들어오면 한쪽만 이기고, 최종 상태는 이긴 쪽과 일치한다")
    void claimVsManualPaid_onlyOneWins() throws Exception {
        for (int round = 0; round < ROUNDS; round++) {
            Integer seq = newSchedule("NONPAID");

            List<Integer> results = race(2, i -> i == 0
                    ? () -> tx.execute(s -> scheduleRepository.claimForTransfer(seq, "NONPAID", LocalDateTime.now()))
                    : () -> tx.execute(s -> scheduleRepository.markManualPaidIfUnpaid(seq, LocalDateTime.now())));

            int claimed = results.get(0);
            int manualPaid = results.get(1);
            assertThat(claimed + manualPaid).as("round %d", round).isEqualTo(1);
            assertThat(statusOf(seq)).isEqualTo(claimed == 1 ? "REQUESTED" : "MANUAL_PAID");
        }
    }

    @Test
    @DisplayName("이체 선점과 연체 전환이 동시에 들어오면 한쪽만 이기고, 선점된 회차는 연체로 덮어써지지 않는다")
    void claimVsOverdue_onlyOneWins() throws Exception {
        for (int round = 0; round < ROUNDS; round++) {
            Integer seq = newSchedule("NONPAID");

            List<Integer> results = race(2, i -> i == 0
                    ? () -> tx.execute(s -> scheduleRepository.claimForTransfer(seq, "NONPAID", LocalDateTime.now()))
                    : () -> tx.execute(s -> scheduleRepository.markOverdueIfNonpaid(seq)));

            int claimed = results.get(0);
            int overdue = results.get(1);
            assertThat(claimed + overdue).as("round %d", round).isEqualTo(1);
            assertThat(statusOf(seq)).isEqualTo(claimed == 1 ? "REQUESTED" : "OVERDUE");
        }
    }

    @Test
    @DisplayName("이체 성공 확정과 대사 확정이 동시에 들어와도 납부 확정은 1번만 통과한다")
    void concurrentMarkPaid_onlyOneWins() throws Exception {
        for (int round = 0; round < ROUNDS; round++) {
            Integer seq = newSchedule("REQUESTED");

            List<Integer> results = race(THREADS, i -> () ->
                    tx.execute(s -> scheduleRepository.markPaidIfRequested(seq, LocalDateTime.now())));

            assertThat(results.stream().mapToInt(Integer::intValue).sum()).as("round %d", round).isEqualTo(1);
            assertThat(statusOf(seq)).isEqualTo("PAID");
        }
    }

    @Test
    @DisplayName("유니크 인덱스 SQL을 적용하면 같은 회차의 자동 확정 이벤트는 2건째부터 거절된다")
    void autoConfirmedUniqueIndex_rejectsSecond() throws Exception {
        applyAutoConfirmedUniqueIndex();
        Integer seq = newSchedule("PAID");
        Integer otherSeq = newSchedule("PAID");

        tx.executeWithoutResult(s -> eventRepository.save(event(seq, "AUTO_CONFIRMED")));

        // 다른 상태의 이벤트, 다른 회차의 자동 확정은 막지 않는다
        tx.executeWithoutResult(s -> {
            eventRepository.save(event(seq, "REQUESTED"));
            eventRepository.save(event(seq, "REQUESTED"));
            eventRepository.save(event(otherSeq, "AUTO_CONFIRMED"));
        });

        assertThatThrownBy(() -> tx.executeWithoutResult(s -> eventRepository.saveAndFlush(event(seq, "AUTO_CONFIRMED"))))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    // 모든 스레드를 준비시킨 뒤 동시에 출발시킨다. 결과는 스레드 순서대로 돌려준다.
    private List<Integer> race(int threads, java.util.function.IntFunction<Callable<Integer>> taskOf) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<Future<Integer>> futures = new ArrayList<>();
            for (int i = 0; i < threads; i++) {
                Callable<Integer> task = taskOf.apply(i);
                futures.add(pool.submit(() -> {
                    ready.countDown();
                    start.await();
                    return task.call();
                }));
            }
            ready.await(10, TimeUnit.SECONDS);
            start.countDown();
            List<Integer> results = new ArrayList<>();
            for (Future<Integer> f : futures) {
                results.add(f.get(30, TimeUnit.SECONDS));
            }
            return results;
        } finally {
            pool.shutdownNow();
        }
    }

    private Integer newSchedule(String status) {
        return tx.execute(s -> {
            RepaymentSchedule schedule = RepaymentSchedule.builder()
                    .contract(contractRepository.getReferenceById(contract.getContractSeq()))
                    .installmentNo(1)
                    .dueDt(LocalDate.now())
                    .principalDue(new BigDecimal("1000000"))
                    .interestDue(BigDecimal.ZERO)
                    .totalDue(new BigDecimal("1000000"))
                    .remainingBalanceAfter(BigDecimal.ZERO)
                    .status(status)
                    .createdDttm(LocalDateTime.now())
                    .build();
            return scheduleRepository.save(schedule).getScheduleSeq();
        });
    }

    private String statusOf(Integer seq) {
        return jdbcTemplate.queryForObject(
                "select status from repayment_schedule where schedule_seq = ?", String.class, seq);
    }

    private RepaymentEvent event(Integer scheduleSeq, String status) {
        return RepaymentEvent.builder()
                .repaymentSchedule(scheduleRepository.getReferenceById(scheduleSeq))
                .contract(contractRepository.getReferenceById(contract.getContractSeq()))
                .amountPaid(new BigDecimal("1000000"))
                .reportedByUser(userRepository.getReferenceById(creditor.getUserSeq()))
                .confirmedByUser(userRepository.getReferenceById(creditor.getUserSeq()))
                .status(status)
                .requestedDttm(LocalDateTime.now())
                .build();
    }

    // db/manual의 마이그레이션 SQL을 그대로 실행한다. 이미 적용됐으면 건너뛴다.
    private void applyAutoConfirmedUniqueIndex() throws Exception {
        Integer exists = jdbcTemplate.queryForObject("""
                select count(*) from information_schema.statistics
                where table_schema = database() and table_name = 'repayment_event'
                  and index_name = 'uk_repayment_event_auto_confirmed'
                """, Integer.class);
        if (exists != null && exists > 0) {
            return;
        }
        String sql = new ClassPathResource("db/manual/20261013_add_repayment_event_auto_confirmed_unique.sql")
                .getContentAsString(StandardCharsets.UTF_8);
        String alter = Arrays.stream(sql.split(";"))
                .map(stmt -> stmt.lines().filter(line -> !line.trim().startsWith("--")).reduce("", (a, b) -> a + "\n" + b).trim())
                .filter(stmt -> stmt.startsWith("ALTER TABLE"))
                .findFirst()
                .orElseThrow();
        jdbcTemplate.execute(alter);
    }

    private User user() {
        int n = SEQ.incrementAndGet();
        return User.builder()
                .email("user" + n + "@test.com")
                .phone("010" + String.format("%08d", n))
                .state("ACTIVE")
                .createdDttm(LocalDateTime.now())
                .build();
    }
}
