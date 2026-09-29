package com.ssafy.a305.be.domain.repayment.service;

import com.ssafy.a305.be.domain.account.entity.BankAccount;
import com.ssafy.a305.be.domain.contract.entity.Contract;
import com.ssafy.a305.be.domain.notification.service.NotificationCommandService;
import com.ssafy.a305.be.domain.repayment.entity.AutoTransferRegistration;
import com.ssafy.a305.be.domain.repayment.entity.RepaymentEvent;
import com.ssafy.a305.be.domain.repayment.entity.RepaymentSchedule;
import com.ssafy.a305.be.domain.repayment.entity.TransferAttempt;
import com.ssafy.a305.be.domain.repayment.repository.AutoTransferRegistrationRepository;
import com.ssafy.a305.be.domain.repayment.repository.RepaymentEventRepository;
import com.ssafy.a305.be.domain.repayment.repository.RepaymentScheduleRepository;
import com.ssafy.a305.be.domain.repayment.repository.TransferAttemptRepository;
import com.ssafy.a305.be.domain.user.entity.User;
import com.ssafy.a305.be.global.exception.TransferFailException;
import com.ssafy.a305.be.global.exception.TransferOutcomeUnknownException;
import com.ssafy.a305.be.global.external.ssafy.SsafyTransferApiClient;
import com.ssafy.a305.be.global.external.ssafy.dto.SsafyTransferResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;

/**
 * 자동이체 1건 처리 테스트.
 *
 * <p>{@link AutoTransferService}를 직접 생성해 <b>실제 운영 코드</b>를 호출하고, DB·Redis·외부
 * API만 Mockito로 격리한다. 트랜잭션 경계는 {@link RecordingTransactionManager}로 기록해,
 * "외부 호출이 트랜잭션 밖에서 일어나는가", "시도 기록이 호출보다 먼저 커밋되는가" 같은
 * 구조 자체를 검증한다.</p>
 */
@ExtendWith(MockitoExtension.class)
class AutoTransferServiceTest {

    private static final Integer SCHEDULE_SEQ = 1;
    private static final Integer CONTRACT_SEQ = 100;
    private static final Long ATTEMPT_SEQ = 77L;
    private static final String LOCK_TOKEN = "lock-token-001";

    @Mock
    private RepaymentScheduleRepository repaymentScheduleRepository;
    @Mock
    private AutoTransferRegistrationRepository registrationRepository;
    @Mock
    private RepaymentEventRepository repaymentEventRepository;
    @Mock
    private TransferAttemptRepository transferAttemptRepository;
    @Mock
    private NotificationCommandService notificationCommandService;
    @Mock
    private SsafyTransferApiClient ssafyTransferApiClient;
    @Mock
    private RepaymentTransferRequestGuard guard;

    private RecordingTransactionManager transactionManager;
    private List<String> events;
    private AutoTransferService service;

    // ──── API 호출 시점에 관찰한 값 ────
    private boolean txActiveAtApiCall;
    private TransferAttempt.Status attemptStatusAtApiCall;
    /** Tx1이 저장한 시도. 이후 단계에서 상태가 바뀌는지 관찰한다. */
    private TransferAttempt savedAttempt;

    // ──── 픽스쳐 ────
    private User debtor;
    private User creditor;
    private Contract contract;
    private BankAccount debtorAccount;
    private BankAccount creditorAccount;

    @BeforeEach
    void setUp() {
        transactionManager = new RecordingTransactionManager();
        events = transactionManager.events;
        service = new AutoTransferService(
                repaymentScheduleRepository,
                registrationRepository,
                repaymentEventRepository,
                transferAttemptRepository,
                notificationCommandService,
                ssafyTransferApiClient,
                guard,
                new TransactionTemplate(transactionManager)
        );

        txActiveAtApiCall = true; // 호출이 일어나지 않았다면 값이 바뀌지 않아 눈에 띈다
        attemptStatusAtApiCall = null;
        savedAttempt = null;

        // 락 해제도 이벤트에 남겨 "마지막 커밋 뒤에 풀리는가"를 순서로 검증한다.
        lenient().doAnswer(inv -> {
            events.add("RELEASE");
            return null;
        }).when(guard).release(any(), any());

        // 시도 저장소: 저장한 객체를 기억하고, 같은 객체를 다시 돌려준다.
        lenient().when(transferAttemptRepository.save(any(TransferAttempt.class))).thenAnswer(inv -> {
            TransferAttempt attempt = inv.getArgument(0);
            ReflectionTestUtils.setField(attempt, "attemptSeq", ATTEMPT_SEQ);
            savedAttempt = attempt;
            return attempt;
        });
        lenient().when(transferAttemptRepository.findById(ATTEMPT_SEQ))
                .thenAnswer(inv -> Optional.ofNullable(savedAttempt));

        debtor = User.builder()
                .userSeq(1).email("debtor@test.com")
                .ssafyUserKey("debtor-ssafy-key-001").state("ACTIVE").build();
        creditor = User.builder()
                .userSeq(2).email("creditor@test.com")
                .ssafyUserKey("creditor-ssafy-key-002").state("ACTIVE").build();
        contract = Contract.builder()
                .contractSeq(CONTRACT_SEQ)
                .contractId("C-20260325-TEST01")
                .creditor(creditor).debtor(debtor)
                .principalAmount(new BigDecimal("5000000"))
                .status("ACTIVE")
                .startDt(LocalDate.of(2026, 1, 1))
                .endDt(LocalDate.of(2027, 1, 1))
                .build();
        debtorAccount = BankAccount.builder().accountSeq(10).accountNo("0012345678901234").isPrimary(true).build();
        creditorAccount = BankAccount.builder().accountSeq(20).accountNo("0098765432109876").isPrimary(true).build();
    }

    // ──── 픽스쳐 헬퍼 ────

    /**
     * 스케줄을 만들면서 계약의 스케줄 목록에도 등록한다. 완납 판단이 allMatch라
     * 목록이 비어 있으면 계약이 종료 처리되므로 반드시 채워야 한다.
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
                .registrationSeq(1).contract(contract)
                .debtorAccount(debtorAccount).creditorAccount(creditorAccount)
                .amount(new BigDecimal(amount)).transferDay(15).isActive(true)
                .build();
    }

    /** 락 획득 + 회차 조회 성공. */
    private void givenLockedAndFound(RepaymentSchedule schedule) {
        given(guard.tryAcquire(SCHEDULE_SEQ)).willReturn(LOCK_TOKEN);
        given(repaymentScheduleRepository.findByScheduleSeqWithContractUsers(SCHEDULE_SEQ))
                .willReturn(Optional.of(schedule));
    }

    /** 락 + 회차 + 자동이체 등록까지 준비. */
    private void givenReadyToTransfer(RepaymentSchedule schedule, String registrationAmount) {
        givenLockedAndFound(schedule);
        given(registrationRepository.findActiveWithAccounts(CONTRACT_SEQ))
                .willReturn(Optional.of(newRegistration(registrationAmount)));
    }

    /** 이체 API가 성공한다. 호출 시점의 트랜잭션·시도 상태를 관찰해 둔다. */
    private void givenApiSucceeds() {
        given(ssafyTransferApiClient.transfer(anyString(), anyString(), any(), anyString(), anyString()))
                .willAnswer(inv -> {
                    observeApiCall();
                    return new SsafyTransferResponse();
                });
    }

    /** 이체 API가 예외를 던진다. */
    private void givenApiThrows(RuntimeException exception) {
        given(ssafyTransferApiClient.transfer(anyString(), anyString(), any(), anyString(), anyString()))
                .willAnswer(inv -> {
                    observeApiCall();
                    throw exception;
                });
    }

    private void observeApiCall() {
        events.add("API");
        txActiveAtApiCall = TransactionSynchronizationManager.isActualTransactionActive();
        attemptStatusAtApiCall = savedAttempt == null ? null : savedAttempt.getStatus();
    }

    // ──────────────────────────────────────────────

    @Nested
    @DisplayName("도메인 메서드")
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
    @DisplayName("성공 케이스")
    class SuccessTests {

        @Test
        @DisplayName("API 성공 시 → 회차 PAID, 시도 SUCCEEDED, 상환 이벤트 저장")
        void execute_success_shouldMarkPaidAndSaveEvent() {
            RepaymentSchedule schedule = newSchedule(SCHEDULE_SEQ, "500000");
            newSchedule(2, "500000"); // 남은 회차가 있어 계약은 종료되지 않는다
            givenReadyToTransfer(schedule, "500000");
            givenApiSucceeds();

            AutoTransferService.Result result = service.execute(SCHEDULE_SEQ);

            assertThat(result).isEqualTo(AutoTransferService.Result.SUCCEEDED);
            assertThat(schedule.getStatus()).isEqualTo("PAID");
            assertThat(schedule.getPaidDttm()).isNotNull();
            assertThat(savedAttempt.getStatus()).isEqualTo(TransferAttempt.Status.SUCCEEDED);
            assertThat(savedAttempt.getResolvedDttm()).isNotNull();

            then(ssafyTransferApiClient).should().transfer(
                    eq("0012345678901234"),   // 채무자 출금 계좌
                    eq("0098765432109876"),   // 채권자 입금 계좌
                    eq(new BigDecimal("500000")),
                    eq("BL1-1 1회차 C-20260325-TEST01"),
                    eq("debtor-ssafy-key-001")
            );

            ArgumentCaptor<RepaymentEvent> captor = ArgumentCaptor.forClass(RepaymentEvent.class);
            then(repaymentEventRepository).should().save(captor.capture());
            RepaymentEvent saved = captor.getValue();
            assertThat(saved.getEvidenceGrade()).isEqualTo("GOLD");
            assertThat(saved.getStatus()).isEqualTo("AUTO_CONFIRMED");
            assertThat(saved.getAmountPaid()).isEqualByComparingTo(new BigDecimal("500000"));
            assertThat(saved.getReportedByUser()).isEqualTo(debtor);
            assertThat(saved.getConfirmedByUser()).isEqualTo(creditor);

            then(notificationCommandService).should().sendRepaymentPaidNotifications(schedule);
        }

        @Test
        @DisplayName("이체 금액은 등록 정보가 아니라 회차의 totalDue를 쓴다")
        void execute_shouldUseScheduleTotalDue_notRegistrationAmount() {
            RepaymentSchedule schedule = newSchedule(SCHEDULE_SEQ, "416667"); // 할부 1회분
            newSchedule(2, "416667");
            givenReadyToTransfer(schedule, "5000000"); // 등록 정보에는 원금 전액이 들어 있다
            givenApiSucceeds();

            service.execute(SCHEDULE_SEQ);

            then(ssafyTransferApiClient).should().transfer(
                    anyString(), anyString(), eq(new BigDecimal("416667")), anyString(), anyString());
        }

        @Test
        @DisplayName("마지막 회차가 완납되면 계약이 CLOSED로 종료된다")
        void execute_lastInstallment_shouldCloseContract() {
            RepaymentSchedule schedule = newSchedule(SCHEDULE_SEQ, "500000");
            givenReadyToTransfer(schedule, "500000");
            givenApiSucceeds();

            service.execute(SCHEDULE_SEQ);

            assertThat(contract.getStatus()).isEqualTo("CLOSED");
            then(notificationCommandService).should().sendContractClosedNotifications(contract);
        }

        @Test
        @DisplayName("미납 회차가 남아 있으면 계약을 종료하지 않는다")
        void execute_remainingInstallment_shouldNotCloseContract() {
            RepaymentSchedule schedule = newSchedule(SCHEDULE_SEQ, "500000");
            newSchedule(2, "500000");
            givenReadyToTransfer(schedule, "500000");
            givenApiSucceeds();

            service.execute(SCHEDULE_SEQ);

            assertThat(contract.getStatus()).isEqualTo("ACTIVE");
            then(notificationCommandService).should(never()).sendContractClosedNotifications(any());
        }

        @Test
        @DisplayName("시도 번호는 직전 시도 다음 번호이고 적요 마커에도 실린다")
        void execute_shouldUseNextAttemptNo() {
            RepaymentSchedule schedule = newSchedule(SCHEDULE_SEQ, "500000");
            newSchedule(2, "500000");
            givenReadyToTransfer(schedule, "500000");
            given(transferAttemptRepository.findLastAttemptNo(SCHEDULE_SEQ)).willReturn(2);
            givenApiSucceeds();

            service.execute(SCHEDULE_SEQ);

            assertThat(savedAttempt.getAttemptNo()).isEqualTo(3);
            then(ssafyTransferApiClient).should().transfer(
                    anyString(), anyString(), any(),
                    eq("BL1-3 1회차 C-20260325-TEST01"),
                    anyString());
        }

        @Test
        @DisplayName("알림이 실패해도 이체 결과는 롤백되지 않는다")
        void execute_notificationFailure_shouldNotRollbackResult() {
            RepaymentSchedule schedule = newSchedule(SCHEDULE_SEQ, "500000");
            newSchedule(2, "500000");
            givenReadyToTransfer(schedule, "500000");
            givenApiSucceeds();
            org.mockito.BDDMockito.willThrow(new IllegalStateException("FCM down"))
                    .given(notificationCommandService).sendRepaymentPaidNotifications(schedule);

            AutoTransferService.Result result = service.execute(SCHEDULE_SEQ);

            assertThat(result).isEqualTo(AutoTransferService.Result.SUCCEEDED);
            assertThat(schedule.getStatus()).isEqualTo("PAID");
            assertThat(events).doesNotContain("ROLLBACK");
        }
    }

    // ──────────────────────────────────────────────

    @Nested
    @DisplayName("3단 트랜잭션 구조")
    class ThreeStageTests {

        @Test
        @DisplayName("은행 API는 트랜잭션 밖에서 호출된다")
        void execute_apiCall_shouldBeOutsideTransaction() {
            RepaymentSchedule schedule = newSchedule(SCHEDULE_SEQ, "500000");
            newSchedule(2, "500000");
            givenReadyToTransfer(schedule, "500000");
            givenApiSucceeds();

            service.execute(SCHEDULE_SEQ);

            assertThat(txActiveAtApiCall)
                    .as("API 호출 중 트랜잭션이 열려 있으면 커넥션·row 락을 응답 대기만큼 붙잡는다")
                    .isFalse();
        }

        @Test
        @DisplayName("시도 기록은 API 호출보다 먼저 커밋된다")
        void execute_attempt_shouldBeCommittedBeforeApiCall() {
            RepaymentSchedule schedule = newSchedule(SCHEDULE_SEQ, "500000");
            newSchedule(2, "500000");
            givenReadyToTransfer(schedule, "500000");
            givenApiSucceeds();

            service.execute(SCHEDULE_SEQ);

            // Tx1 커밋 → API → Tx2 커밋 → 락 해제
            assertThat(events).containsExactly("BEGIN", "COMMIT", "API", "BEGIN", "COMMIT", "RELEASE");
        }

        @Test
        @DisplayName("API를 호출하는 시점의 시도 상태는 REQUESTED다")
        void execute_attempt_shouldBeRequestedWhileApiCall() {
            RepaymentSchedule schedule = newSchedule(SCHEDULE_SEQ, "500000");
            newSchedule(2, "500000");
            givenReadyToTransfer(schedule, "500000");
            givenApiSucceeds();

            service.execute(SCHEDULE_SEQ);

            assertThat(attemptStatusAtApiCall).isEqualTo(TransferAttempt.Status.REQUESTED);
        }

        @Test
        @DisplayName("락은 마지막 커밋이 끝난 뒤에 해제되고, 획득할 때 받은 토큰 그대로 해제된다")
        void execute_lock_shouldBeReleasedAfterLastCommitWithSameToken() {
            RepaymentSchedule schedule = newSchedule(SCHEDULE_SEQ, "500000");
            newSchedule(2, "500000");
            givenReadyToTransfer(schedule, "500000");
            givenApiSucceeds();

            service.execute(SCHEDULE_SEQ);

            assertThat(events.get(events.size() - 1)).isEqualTo("RELEASE");
            assertThat(events.get(events.size() - 2)).isEqualTo("COMMIT");
            then(guard).should().release(SCHEDULE_SEQ, LOCK_TOKEN);
        }
    }

    // ──────────────────────────────────────────────

    @Nested
    @DisplayName("결과 불명 — 돈이 나갔는지 모르는 경우")
    class UnknownOutcomeTests {

        @Test
        @DisplayName("타임아웃 등으로 결과를 모르면 → UNRESOLVED, 시도는 REQUESTED로 남는다")
        void execute_unknownOutcome_shouldLeaveAttemptRequested() {
            RepaymentSchedule schedule = newSchedule(SCHEDULE_SEQ, "500000");
            givenReadyToTransfer(schedule, "500000");
            givenApiThrows(new TransferOutcomeUnknownException("SSAFY_TRANSFER_UNKNOWN|IO|Read timed out"));

            AutoTransferService.Result result = service.execute(SCHEDULE_SEQ);

            assertThat(result).isEqualTo(AutoTransferService.Result.UNRESOLVED);
            assertThat(savedAttempt.getStatus())
                    .as("결과 불명을 FAILED로 닫으면 다음 슬롯이 재시도해 이중 출금이 된다")
                    .isEqualTo(TransferAttempt.Status.REQUESTED);
        }

        @Test
        @DisplayName("결과 불명이면 회차를 PAID로도, 실패로도 바꾸지 않는다")
        void execute_unknownOutcome_shouldNotTouchScheduleOrRecordEvent() {
            RepaymentSchedule schedule = newSchedule(SCHEDULE_SEQ, "500000");
            givenReadyToTransfer(schedule, "500000");
            givenApiThrows(new TransferOutcomeUnknownException("timeout"));

            service.execute(SCHEDULE_SEQ);

            assertThat(schedule.getStatus()).isEqualTo("NONPAID");
            then(repaymentEventRepository).shouldHaveNoInteractions();
            then(notificationCommandService).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("결과 불명이면 결과 반영 트랜잭션(Tx2)을 열지 않고 락만 해제한다")
        void execute_unknownOutcome_shouldNotOpenSecondTransaction() {
            RepaymentSchedule schedule = newSchedule(SCHEDULE_SEQ, "500000");
            givenReadyToTransfer(schedule, "500000");
            givenApiThrows(new TransferOutcomeUnknownException("timeout"));

            service.execute(SCHEDULE_SEQ);

            assertThat(events).containsExactly("BEGIN", "COMMIT", "API", "RELEASE");
        }
    }

    // ──────────────────────────────────────────────

    @Nested
    @DisplayName("확정 실패 — 은행이 거절한 경우")
    class DefiniteFailureTests {

        @Test
        @DisplayName("은행이 거절하면 → 예외를 전파하고, 시도는 FAILED와 응답코드로 닫힌다")
        void execute_rejection_shouldCloseAttemptAsFailed() {
            RepaymentSchedule schedule = newSchedule(SCHEDULE_SEQ, "500000");
            givenReadyToTransfer(schedule, "500000");
            givenApiThrows(new TransferFailException("SSAFY_TRANSFER_FAILED|A1014|잔액 부족"));

            assertThatThrownBy(() -> service.execute(SCHEDULE_SEQ))
                    .isInstanceOf(TransferFailException.class)
                    .isNotInstanceOf(TransferOutcomeUnknownException.class)
                    .hasMessageContaining("A1014");

            assertThat(savedAttempt.getStatus()).isEqualTo(TransferAttempt.Status.FAILED);
            assertThat(savedAttempt.getResponseCode()).isEqualTo("A1014");
        }

        @Test
        @DisplayName("확정 실패 후에도 회차는 NONPAID로 남아 다음 슬롯에 다시 대상이 된다")
        void execute_rejection_shouldKeepScheduleNonpaid() {
            RepaymentSchedule schedule = newSchedule(SCHEDULE_SEQ, "500000");
            givenReadyToTransfer(schedule, "500000");
            givenApiThrows(new TransferFailException("SSAFY_TRANSFER_FAILED|A1014|잔액 부족"));

            assertThatThrownBy(() -> service.execute(SCHEDULE_SEQ)).isInstanceOf(TransferFailException.class);

            assertThat(schedule.getStatus()).isEqualTo("NONPAID");
            assertThat(schedule.getPaidDttm()).isNull();
            then(repaymentEventRepository).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("응답코드가 없는 실패(연결 실패 등)도 FAILED로 닫히며 코드는 비어 있다")
        void execute_failureWithoutCode_shouldCloseWithNullCode() {
            RepaymentSchedule schedule = newSchedule(SCHEDULE_SEQ, "500000");
            givenReadyToTransfer(schedule, "500000");
            givenApiThrows(new TransferFailException("SSAFY 이체 API 연결 실패: Connection refused"));

            assertThatThrownBy(() -> service.execute(SCHEDULE_SEQ)).isInstanceOf(TransferFailException.class);

            assertThat(savedAttempt.getStatus()).isEqualTo(TransferAttempt.Status.FAILED);
            assertThat(savedAttempt.getResponseCode()).isNull();
        }

        @Test
        @DisplayName("확정 실패에서도 락은 해제된다")
        void execute_rejection_shouldStillReleaseLock() {
            RepaymentSchedule schedule = newSchedule(SCHEDULE_SEQ, "500000");
            givenReadyToTransfer(schedule, "500000");
            givenApiThrows(new TransferFailException("SSAFY_TRANSFER_FAILED|A1014|잔액 부족"));

            assertThatThrownBy(() -> service.execute(SCHEDULE_SEQ)).isInstanceOf(TransferFailException.class);

            assertThat(events).containsExactly("BEGIN", "COMMIT", "API", "BEGIN", "COMMIT", "RELEASE");
        }
    }

    // ──────────────────────────────────────────────

    @Nested
    @DisplayName("이체 성공 후 결과 반영 실패")
    class CompletionFailureTests {

        @Test
        @DisplayName("이체는 성공했는데 반영 트랜잭션이 실패하면 → 롤백되고 락은 해제된다")
        void execute_completionFailure_shouldRollbackAndReleaseLock() {
            RepaymentSchedule schedule = newSchedule(SCHEDULE_SEQ, "500000");
            newSchedule(2, "500000");
            givenReadyToTransfer(schedule, "500000");
            givenApiSucceeds();
            given(repaymentEventRepository.save(any(RepaymentEvent.class)))
                    .willThrow(new IllegalStateException("DB down"));

            assertThatThrownBy(() -> service.execute(SCHEDULE_SEQ))
                    .isInstanceOf(IllegalStateException.class);

            // Tx1 은 이미 커밋되어 있다. Tx2 만 롤백된다.
            assertThat(events).containsExactly("BEGIN", "COMMIT", "API", "BEGIN", "ROLLBACK", "RELEASE");
        }
    }

    // ──────────────────────────────────────────────

    @Nested
    @DisplayName("진입 차단 — 게이트")
    class GateTests {

        @Test
        @DisplayName("결과가 확정되지 않은 이전 시도가 있으면 → BLOCKED, API를 호출하지 않는다")
        void execute_unresolvedAttempt_shouldBlockWithoutApiCall() {
            RepaymentSchedule schedule = newSchedule(SCHEDULE_SEQ, "500000");
            givenLockedAndFound(schedule);
            given(transferAttemptRepository.existsByScheduleSeqAndStatus(
                    SCHEDULE_SEQ, TransferAttempt.Status.REQUESTED)).willReturn(true);

            AutoTransferService.Result result = service.execute(SCHEDULE_SEQ);

            assertThat(result).isEqualTo(AutoTransferService.Result.BLOCKED);
            then(ssafyTransferApiClient).shouldHaveNoInteractions();
            then(transferAttemptRepository).should(never()).save(any());
            assertThat(schedule.getStatus()).isEqualTo("NONPAID");
        }

        @Test
        @DisplayName("이미 납부된 회차면 → SKIPPED, 시도도 이체도 없다")
        void execute_alreadyPaid_shouldSkip() {
            RepaymentSchedule schedule = newSchedule(SCHEDULE_SEQ, "500000");
            schedule.markPaid(LocalDateTime.now());
            givenLockedAndFound(schedule);

            AutoTransferService.Result result = service.execute(SCHEDULE_SEQ);

            assertThat(result).isEqualTo(AutoTransferService.Result.SKIPPED);
            then(ssafyTransferApiClient).shouldHaveNoInteractions();
            then(transferAttemptRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("회차를 찾을 수 없으면 → SKIPPED")
        void execute_scheduleNotFound_shouldSkip() {
            given(guard.tryAcquire(SCHEDULE_SEQ)).willReturn(LOCK_TOKEN);
            given(repaymentScheduleRepository.findByScheduleSeqWithContractUsers(SCHEDULE_SEQ))
                    .willReturn(Optional.empty());

            assertThat(service.execute(SCHEDULE_SEQ)).isEqualTo(AutoTransferService.Result.SKIPPED);
            then(ssafyTransferApiClient).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("자동이체 등록이 없으면 → SKIPPED, 시도를 남기지 않는다")
        void execute_noRegistration_shouldSkipWithoutAttempt() {
            RepaymentSchedule schedule = newSchedule(SCHEDULE_SEQ, "500000");
            givenLockedAndFound(schedule);
            given(registrationRepository.findActiveWithAccounts(CONTRACT_SEQ)).willReturn(Optional.empty());

            AutoTransferService.Result result = service.execute(SCHEDULE_SEQ);

            assertThat(result).isEqualTo(AutoTransferService.Result.SKIPPED);
            then(transferAttemptRepository).should(never()).save(any());
            then(ssafyTransferApiClient).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("유니크 제약이 동시 선점을 막으면 → BLOCKED, API를 호출하지 않는다")
        void execute_uniqueViolation_shouldBlockWithoutApiCall() {
            RepaymentSchedule schedule = newSchedule(SCHEDULE_SEQ, "500000");
            givenReadyToTransfer(schedule, "500000");
            // 이미 응답 로직이 걸린 스터빙이라 given(...) 형태로 덮으면 스터빙 중 그 로직이 실행된다.
            org.mockito.BDDMockito.willThrow(new DataIntegrityViolationException("uk_transfer_attempt_schedule_no"))
                    .given(transferAttemptRepository).save(any(TransferAttempt.class));

            AutoTransferService.Result result = service.execute(SCHEDULE_SEQ);

            assertThat(result).isEqualTo(AutoTransferService.Result.BLOCKED);
            then(ssafyTransferApiClient).shouldHaveNoInteractions();
            assertThat(events).containsExactly("BEGIN", "ROLLBACK", "RELEASE");
        }
    }

    // ──────────────────────────────────────────────

    @Nested
    @DisplayName("분산락")
    class LockTests {

        @Test
        @DisplayName("이미 다른 요청이 처리 중이면 → 아무것도 하지 않고 SKIPPED")
        void execute_lockNotAcquired_shouldSkipWithoutSideEffect() {
            given(guard.tryAcquire(SCHEDULE_SEQ)).willReturn(null);

            AutoTransferService.Result result = service.execute(SCHEDULE_SEQ);

            assertThat(result).isEqualTo(AutoTransferService.Result.SKIPPED);
            then(repaymentScheduleRepository).shouldHaveNoInteractions();
            then(registrationRepository).shouldHaveNoInteractions();
            then(transferAttemptRepository).shouldHaveNoInteractions();
            then(ssafyTransferApiClient).shouldHaveNoInteractions();
            then(repaymentEventRepository).shouldHaveNoInteractions();
            then(notificationCommandService).shouldHaveNoInteractions();
            assertThat(events).as("트랜잭션도 열지 않는다").isEmpty();
        }

        @Test
        @DisplayName("락을 잡지 못했으면 해제도 하지 않는다 (남의 락을 지우면 안 됨)")
        void execute_lockNotAcquired_shouldNotRelease() {
            given(guard.tryAcquire(SCHEDULE_SEQ)).willReturn(null);

            service.execute(SCHEDULE_SEQ);

            then(guard).should(never()).release(any(), any());
        }

        @Test
        @DisplayName("결과 불명이어도 락은 해제된다")
        void execute_unknownOutcome_shouldStillReleaseLock() {
            RepaymentSchedule schedule = newSchedule(SCHEDULE_SEQ, "500000");
            givenReadyToTransfer(schedule, "500000");
            givenApiThrows(new TransferOutcomeUnknownException("timeout"));

            service.execute(SCHEDULE_SEQ);

            then(guard).should().release(SCHEDULE_SEQ, LOCK_TOKEN);
        }
    }
}
