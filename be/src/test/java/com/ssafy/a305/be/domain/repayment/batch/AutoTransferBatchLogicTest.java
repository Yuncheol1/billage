package com.ssafy.a305.be.domain.repayment.batch;

import com.ssafy.a305.be.domain.account.entity.BankAccount;
import com.ssafy.a305.be.domain.contract.entity.Contract;
import com.ssafy.a305.be.domain.notification.service.NotificationCommandService;
import com.ssafy.a305.be.domain.repayment.entity.AutoTransferRegistration;
import com.ssafy.a305.be.domain.repayment.entity.RepaymentEvent;
import com.ssafy.a305.be.domain.repayment.entity.RepaymentSchedule;
import com.ssafy.a305.be.domain.repayment.repository.AutoTransferRegistrationRepository;
import com.ssafy.a305.be.domain.repayment.repository.RepaymentEventRepository;
import com.ssafy.a305.be.domain.repayment.service.RepaymentTransferRequestGuard;
import com.ssafy.a305.be.domain.user.entity.User;
import com.ssafy.a305.be.global.exception.TransferFailException;
import com.ssafy.a305.be.global.external.ssafy.SsafyTransferApiClient;
import com.ssafy.a305.be.global.external.ssafy.dto.SsafyTransferResponse;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

/**
 * 자동이체 배치 로직 단위 테스트.
 *
 * <p>{@link AutoTransferBatchConfig}를 직접 생성해 <b>실제 운영 Processor 빈</b>을 얻고,
 * DB/Redis/외부 API 의존성만 Mockito로 격리합니다.
 * 로직을 복제하지 않으므로 운영 코드가 바뀌면 이 테스트가 함께 깨집니다.</p>
 */
@ExtendWith(MockitoExtension.class)
class AutoTransferBatchLogicTest {

    private static final Integer SCHEDULE_SEQ = 1;
    private static final Integer CONTRACT_SEQ = 100;
    private static final String LOCK_TOKEN = "lock-token-001";

    @Mock
    private EntityManagerFactory entityManagerFactory;

    @Mock
    private SsafyTransferApiClient ssafyTransferApiClient;

    @Mock
    private AutoTransferRegistrationRepository registrationRepository;

    @Mock
    private RepaymentEventRepository repaymentEventRepository;

    @Mock
    private NotificationCommandService notificationCommandService;

    @Mock
    private RepaymentTransferRequestGuard guard;

    private ItemProcessor<RepaymentSchedule, RepaymentSchedule> processor;

    // ──── 테스트 픽스쳐 ────
    private User debtor;
    private User creditor;
    private Contract contract;
    private BankAccount debtorAccount;
    private BankAccount creditorAccount;

    @BeforeEach
    void setUp() {
        // 운영 Configuration을 그대로 생성해 실제 Processor 람다를 꺼낸다.
        // 생성자 인자 순서는 AutoTransferBatchConfig의 필드 선언 순서와 같아야 한다.
        AutoTransferBatchConfig config = new AutoTransferBatchConfig(
                entityManagerFactory,
                ssafyTransferApiClient,
                registrationRepository,
                repaymentEventRepository,
                notificationCommandService,
                guard
        );
        processor = config.autoTransferProcessor();

        // 픽스쳐 생성
        debtor = User.builder()
                .userSeq(1)
                .email("debtor@test.com")
                .ssafyUserKey("debtor-ssafy-key-001")
                .state("ACTIVE")
                .build();

        creditor = User.builder()
                .userSeq(2)
                .email("creditor@test.com")
                .ssafyUserKey("creditor-ssafy-key-002")
                .state("ACTIVE")
                .build();

        contract = Contract.builder()
                .contractSeq(CONTRACT_SEQ)
                .contractId("C-20260325-TEST01")
                .creditor(creditor)
                .debtor(debtor)
                .principalAmount(new BigDecimal("5000000"))
                .status("ACTIVE")
                .startDt(LocalDate.of(2026, 1, 1))
                .endDt(LocalDate.of(2027, 1, 1))
                .build();

        debtorAccount = BankAccount.builder()
                .accountSeq(10)
                .accountNo("0012345678901234")
                .isPrimary(true)
                .build();

        creditorAccount = BankAccount.builder()
                .accountSeq(20)
                .accountNo("0098765432109876")
                .isPrimary(true)
                .build();
    }

    // ──── 픽스쳐 헬퍼 ────

    /**
     * 스케줄을 만들면서 계약의 스케줄 목록에도 등록한다.
     *
     * <p>Processor는 계약의 전체 스케줄이 모두 완납됐는지를 allMatch로 판단한다.
     * 목록이 비어 있으면 allMatch가 true가 되어 계약이 CLOSED로 처리되므로,
     * 테스트 픽스쳐에서 반드시 목록을 채워야 한다.</p>
     */
    private RepaymentSchedule newSchedule(Integer scheduleSeq, String totalDue) {
        RepaymentSchedule schedule = RepaymentSchedule.builder()
                .scheduleSeq(scheduleSeq)
                .contract(contract)
                .installmentNo(scheduleSeq)
                .totalDue(new BigDecimal(totalDue))
                .status("NONPAID")
                .build();
        contract.addRepaymentSchedule(schedule);
        return schedule;
    }

    private AutoTransferRegistration newRegistration(String amount) {
        return AutoTransferRegistration.builder()
                .registrationSeq(1)
                .contract(contract)
                .debtorAccount(debtorAccount)
                .creditorAccount(creditorAccount)
                .amount(new BigDecimal(amount))
                .transferDay(15)
                .isActive(true)
                .build();
    }

    /** 락 획득 성공 + 자동이체 등록 존재 + 이체 API 성공. */
    private void givenHappyPath(String registrationAmount) {
        given(guard.tryAcquire(SCHEDULE_SEQ)).willReturn(LOCK_TOKEN);
        given(registrationRepository.findActiveWithAccounts(CONTRACT_SEQ))
                .willReturn(Optional.of(newRegistration(registrationAmount)));
        given(ssafyTransferApiClient.transfer(anyString(), anyString(), any(), anyString(), anyString()))
                .willReturn(new SsafyTransferResponse());
    }

    // ──────────────────────────────────────────────

    @Nested
    @DisplayName("도메인 메서드 테스트")
    class DomainMethodTests {

        @Test
        @DisplayName("markPaid(): 상태가 PAID로 변경되고 paidDttm이 설정된다")
        void markPaid_shouldChangeToPaid() {
            RepaymentSchedule schedule = newSchedule(SCHEDULE_SEQ, "500000");

            LocalDateTime paidAt = LocalDateTime.of(2026, 3, 25, 10, 0, 0);
            schedule.markPaid(paidAt);

            assertThat(schedule.getStatus()).isEqualTo("PAID");
            assertThat(schedule.getPaidDttm()).isEqualTo(paidAt);
        }

        @Test
        @DisplayName("markOverdue(): 상태가 OVERDUE로 변경된다")
        void markOverdue_shouldChangeToOverdue() {
            RepaymentSchedule schedule = newSchedule(SCHEDULE_SEQ, "500000");

            schedule.markOverdue();

            assertThat(schedule.getStatus()).isEqualTo("OVERDUE");
        }
    }

    // ──────────────────────────────────────────────

    @Nested
    @DisplayName("Processor 성공 케이스")
    class ProcessorSuccessTests {

        @Test
        @DisplayName("자동이체 등록이 있고 API 성공 시 → PAID + RepaymentEvent 저장")
        void process_success_shouldMarkPaidAndSaveEvent() throws Exception {
            // given
            RepaymentSchedule schedule = newSchedule(SCHEDULE_SEQ, "500000");
            newSchedule(2, "500000"); // 남은 회차가 있어 계약은 아직 CLOSED되지 않는다
            givenHappyPath("500000");

            // when
            RepaymentSchedule result = processor.process(schedule);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo("PAID");
            assertThat(result.getPaidDttm()).isNotNull();

            // SSAFY API가 올바른 파라미터로 호출되었는지 검증
            then(ssafyTransferApiClient).should().transfer(
                    eq("0012345678901234"),   // 채무자 출금 계좌
                    eq("0098765432109876"),   // 채권자 입금 계좌
                    eq(new BigDecimal("500000")),
                    eq("C-20260325-TEST01"),
                    eq("debtor-ssafy-key-001")
            );

            // RepaymentEvent가 저장되었는지 검증
            ArgumentCaptor<RepaymentEvent> eventCaptor = ArgumentCaptor.forClass(RepaymentEvent.class);
            then(repaymentEventRepository).should().save(eventCaptor.capture());

            RepaymentEvent savedEvent = eventCaptor.getValue();
            assertThat(savedEvent.getEvidenceGrade()).isEqualTo("GOLD");
            assertThat(savedEvent.getStatus()).isEqualTo("AUTO_CONFIRMED");
            assertThat(savedEvent.getAmountPaid()).isEqualByComparingTo(new BigDecimal("500000"));
            assertThat(savedEvent.getReportedByUser()).isEqualTo(debtor);
            assertThat(savedEvent.getConfirmedByUser()).isEqualTo(creditor);

            // 상환 완료 알림 발송
            then(notificationCommandService).should().sendRepaymentPaidNotifications(schedule);
        }

        @Test
        @DisplayName("이체 금액은 Registration이 아니라 Schedule의 totalDue를 사용해야 한다")
        void process_shouldUseScheduleTotalDue_notRegistrationAmount() throws Exception {
            // given: Registration 금액(500만)과 Schedule totalDue(약 42만)가 다른 경우
            RepaymentSchedule schedule = newSchedule(SCHEDULE_SEQ, "416667"); // 할부 1회분
            newSchedule(2, "416667");
            givenHappyPath("5000000"); // 메타 데이터 (원금 전체)

            // when
            processor.process(schedule);

            // then: 416,667원이 이체되어야 함 (5,000,000원이 아님)
            then(ssafyTransferApiClient).should().transfer(
                    anyString(), anyString(),
                    eq(new BigDecimal("416667")),
                    anyString(), anyString()
            );
        }

        @Test
        @DisplayName("마지막 회차가 완납되면 계약이 CLOSED로 종료된다")
        void process_lastInstallment_shouldCloseContract() throws Exception {
            // given: 이 계약의 유일한 회차
            RepaymentSchedule schedule = newSchedule(SCHEDULE_SEQ, "500000");
            givenHappyPath("500000");

            // when
            processor.process(schedule);

            // then
            assertThat(contract.getStatus()).isEqualTo("CLOSED");
            then(notificationCommandService).should().sendContractClosedNotifications(contract);
        }

        @Test
        @DisplayName("미납 회차가 남아 있으면 계약을 종료하지 않는다")
        void process_remainingInstallment_shouldNotCloseContract() throws Exception {
            // given
            RepaymentSchedule schedule = newSchedule(SCHEDULE_SEQ, "500000");
            newSchedule(2, "500000"); // 아직 NONPAID
            givenHappyPath("500000");

            // when
            processor.process(schedule);

            // then
            assertThat(contract.getStatus()).isEqualTo("ACTIVE");
            then(notificationCommandService).should(never()).sendContractClosedNotifications(any());
        }
    }

    // ──────────────────────────────────────────────

    @Nested
    @DisplayName("Processor 실패/스킵 케이스")
    class ProcessorFailTests {

        @Test
        @DisplayName("자동이체 등록이 없으면 → null 반환 (Skip)")
        void process_noRegistration_shouldReturnNull() throws Exception {
            // given
            RepaymentSchedule schedule = newSchedule(SCHEDULE_SEQ, "500000");

            given(guard.tryAcquire(SCHEDULE_SEQ)).willReturn(LOCK_TOKEN);
            given(registrationRepository.findActiveWithAccounts(CONTRACT_SEQ))
                    .willReturn(Optional.empty());

            // when
            RepaymentSchedule result = processor.process(schedule);

            // then
            assertThat(result).isNull();
            then(ssafyTransferApiClient).shouldHaveNoInteractions();
            then(repaymentEventRepository).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("SSAFY API 실패 시 → TransferFailException 발생")
        void process_apiFailure_shouldThrowException() {
            // given
            RepaymentSchedule schedule = newSchedule(SCHEDULE_SEQ, "500000");

            given(guard.tryAcquire(SCHEDULE_SEQ)).willReturn(LOCK_TOKEN);
            given(registrationRepository.findActiveWithAccounts(CONTRACT_SEQ))
                    .willReturn(Optional.of(newRegistration("500000")));
            given(ssafyTransferApiClient.transfer(anyString(), anyString(), any(), anyString(), anyString()))
                    .willThrow(new TransferFailException("잔액 부족"));

            // when & then
            assertThatThrownBy(() -> processor.process(schedule))
                    .isInstanceOf(TransferFailException.class)
                    .hasMessageContaining("잔액 부족");

            // 실패 시 상태 변경 안 됨
            assertThat(schedule.getStatus()).isEqualTo("NONPAID");
            // 실패 시 RepaymentEvent 저장 안 됨
            then(repaymentEventRepository).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("API 실패 후에도 원본 Schedule의 status가 NONPAID로 유지된다 (멱등성)")
        void process_apiFailure_shouldNotMutateSchedule() {
            // given
            RepaymentSchedule schedule = newSchedule(SCHEDULE_SEQ, "500000");

            given(guard.tryAcquire(SCHEDULE_SEQ)).willReturn(LOCK_TOKEN);
            given(registrationRepository.findActiveWithAccounts(CONTRACT_SEQ))
                    .willReturn(Optional.of(newRegistration("500000")));
            given(ssafyTransferApiClient.transfer(anyString(), anyString(), any(), anyString(), anyString()))
                    .willThrow(new TransferFailException("네트워크 오류"));

            // when
            try {
                processor.process(schedule);
            } catch (Exception ignored) {
            }

            // then: Schedule은 변하지 않아야 함
            assertThat(schedule.getStatus()).isEqualTo("NONPAID");
            assertThat(schedule.getPaidDttm()).isNull();
        }
    }

    // ──────────────────────────────────────────────

    @Nested
    @DisplayName("분산락 시나리오")
    class LockTests {

        @Test
        @DisplayName("이미 다른 워커가 처리 중이면 → 아무것도 하지 않고 Skip")
        void process_lockNotAcquired_shouldSkipWithoutSideEffect() throws Exception {
            // given: 락 획득 실패
            RepaymentSchedule schedule = newSchedule(SCHEDULE_SEQ, "500000");
            given(guard.tryAcquire(SCHEDULE_SEQ)).willReturn(null);

            // when
            RepaymentSchedule result = processor.process(schedule);

            // then: 조회도, 이체도, 이벤트 저장도 일어나지 않아야 한다
            assertThat(result).isNull();
            assertThat(schedule.getStatus()).isEqualTo("NONPAID");
            then(registrationRepository).shouldHaveNoInteractions();
            then(ssafyTransferApiClient).shouldHaveNoInteractions();
            then(repaymentEventRepository).shouldHaveNoInteractions();
            then(notificationCommandService).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("락을 잡지 못했으면 해제도 하지 않는다 (남의 락을 지우면 안 됨)")
        void process_lockNotAcquired_shouldNotRelease() throws Exception {
            // given
            RepaymentSchedule schedule = newSchedule(SCHEDULE_SEQ, "500000");
            given(guard.tryAcquire(SCHEDULE_SEQ)).willReturn(null);

            // when
            processor.process(schedule);

            // then
            then(guard).should(never()).release(any(), any());
        }

        @Test
        @DisplayName("트랜잭션이 없으면 finally에서 락을 해제한다")
        void process_withoutTransaction_shouldReleaseInFinally() throws Exception {
            // given
            RepaymentSchedule schedule = newSchedule(SCHEDULE_SEQ, "500000");
            newSchedule(2, "500000");
            givenHappyPath("500000");
            assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();

            // when
            processor.process(schedule);

            // then
            then(guard).should(times(1)).release(SCHEDULE_SEQ, LOCK_TOKEN);
        }

        @Test
        @DisplayName("획득할 때 발급된 토큰 그대로 해제한다 (CAS 해제의 전제)")
        void process_shouldReleaseWithTheSameToken() throws Exception {
            // given
            RepaymentSchedule schedule = newSchedule(SCHEDULE_SEQ, "500000");
            newSchedule(2, "500000");
            givenHappyPath("500000");

            // when
            processor.process(schedule);

            // then
            ArgumentCaptor<String> tokenCaptor = ArgumentCaptor.forClass(String.class);
            then(guard).should().release(eq(SCHEDULE_SEQ), tokenCaptor.capture());
            assertThat(tokenCaptor.getValue()).isEqualTo(LOCK_TOKEN);
        }

        @Test
        @DisplayName("이체가 실패해도 락은 반드시 해제된다")
        void process_transferFailure_shouldStillRelease() {
            // given
            RepaymentSchedule schedule = newSchedule(SCHEDULE_SEQ, "500000");

            given(guard.tryAcquire(SCHEDULE_SEQ)).willReturn(LOCK_TOKEN);
            given(registrationRepository.findActiveWithAccounts(CONTRACT_SEQ))
                    .willReturn(Optional.of(newRegistration("500000")));
            given(ssafyTransferApiClient.transfer(anyString(), anyString(), any(), anyString(), anyString()))
                    .willThrow(new TransferFailException("잔액 부족"));

            // when
            assertThatThrownBy(() -> processor.process(schedule))
                    .isInstanceOf(TransferFailException.class);

            // then: 예외가 전파되더라도 락은 남지 않는다
            then(guard).should().release(SCHEDULE_SEQ, LOCK_TOKEN);
        }

        @Test
        @DisplayName("트랜잭션이 있으면 process() 시점엔 락을 유지하고, 커밋 완료 후에 해제한다")
        void process_withTransaction_shouldReleaseAfterCommit() throws Exception {
            TransactionSynchronizationManager.initSynchronization();
            try {
                // given
                RepaymentSchedule schedule = newSchedule(SCHEDULE_SEQ, "500000");
                newSchedule(2, "500000");
                givenHappyPath("500000");

                // when
                processor.process(schedule);

                // then: 아직 커밋 전이므로 해제되면 안 된다.
                // 여기서 해제하면 커밋되지 않은 상태를 다른 워커가 읽어 이중 이체가 발생한다.
                then(guard).should(never()).release(any(), any());

                // 트랜잭션 커밋을 흉내 낸다
                List<TransactionSynchronization> syncs =
                        TransactionSynchronizationManager.getSynchronizations();
                assertThat(syncs).hasSize(1);
                syncs.forEach(sync -> sync.afterCompletion(TransactionSynchronization.STATUS_COMMITTED));

                // 커밋이 끝난 뒤에야 해제된다
                then(guard).should(times(1)).release(SCHEDULE_SEQ, LOCK_TOKEN);
            } finally {
                // ThreadLocal이므로 정리하지 않으면 같은 스레드의 다음 테스트가 오염된다
                TransactionSynchronizationManager.clearSynchronization();
            }
        }

        @Test
        @DisplayName("트랜잭션이 롤백되어도 락은 해제된다 (TTL까지 방치되지 않음)")
        void process_withTransaction_shouldReleaseAfterRollback() throws Exception {
            TransactionSynchronizationManager.initSynchronization();
            try {
                // given
                RepaymentSchedule schedule = newSchedule(SCHEDULE_SEQ, "500000");
                newSchedule(2, "500000");
                givenHappyPath("500000");

                // when
                processor.process(schedule);

                List<TransactionSynchronization> syncs =
                        TransactionSynchronizationManager.getSynchronizations();
                syncs.forEach(sync -> sync.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));

                // then
                then(guard).should(times(1)).release(SCHEDULE_SEQ, LOCK_TOKEN);
            } finally {
                TransactionSynchronizationManager.clearSynchronization();
            }
        }
    }
}
