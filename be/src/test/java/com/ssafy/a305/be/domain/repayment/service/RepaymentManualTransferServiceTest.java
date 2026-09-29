package com.ssafy.a305.be.domain.repayment.service;

import com.ssafy.a305.be.domain.account.entity.BankAccount;
import com.ssafy.a305.be.domain.contract.entity.Contract;
import com.ssafy.a305.be.domain.notification.service.NotificationCommandService;
import com.ssafy.a305.be.domain.repayment.dto.response.RepaymentScheduleTransferResponseDto;
import com.ssafy.a305.be.domain.repayment.entity.AutoTransferRegistration;
import com.ssafy.a305.be.domain.repayment.entity.RepaymentSchedule;
import com.ssafy.a305.be.domain.repayment.entity.TransferAttempt;
import com.ssafy.a305.be.domain.repayment.repository.AutoTransferRegistrationRepository;
import com.ssafy.a305.be.domain.repayment.repository.RepaymentScheduleRepository;
import com.ssafy.a305.be.domain.repayment.repository.TransferAttemptRepository;
import com.ssafy.a305.be.domain.user.entity.User;
import com.ssafy.a305.be.global.exception.CustomBusinessException;
import com.ssafy.a305.be.global.exception.TransferFailException;
import com.ssafy.a305.be.global.exception.TransferOutcomeUnknownException;
import com.ssafy.a305.be.global.exception.dto.ErrorCode;
import com.ssafy.a305.be.global.external.ssafy.SsafyTransferApiClient;
import com.ssafy.a305.be.global.external.ssafy.dto.SsafyTransferResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
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
 * 수동 이체 테스트.
 *
 * <p>자동이체와 같은 3단 구조를 따르는지, 그리고 <b>자동이체가 남긴 미해결 시도를 우회하지
 * 못하는지</b>를 검증한다. 후자가 없으면 자동이체가 타임아웃으로 결과를 모르는 상태에서 사용자가
 * 수동 이체 버튼을 눌러 이중 출금이 될 수 있다.</p>
 */
@ExtendWith(MockitoExtension.class)
class RepaymentManualTransferServiceTest {

    private static final Integer CONTRACT_SEQ = 100;
    private static final Integer SCHEDULE_SEQ = 1;
    private static final Integer DEBTOR_SEQ = 1;
    private static final Long ATTEMPT_SEQ = 77L;
    private static final String LOCK_TOKEN = "lock-token";

    @Mock
    private RepaymentScheduleRepository repaymentScheduleRepository;
    @Mock
    private AutoTransferRegistrationRepository registrationRepository;
    @Mock
    private SsafyTransferApiClient ssafyTransferApiClient;
    @Mock
    private NotificationCommandService notificationCommandService;
    @Mock
    private RepaymentTransferRequestGuard guard;
    @Mock
    private TransferAttemptRepository transferAttemptRepository;

    private RecordingTransactionManager transactionManager;
    private List<String> events;
    private RepaymentManualTransferService service;

    private boolean txActiveAtApiCall;
    private TransferAttempt savedAttempt;

    private User debtor;
    private Contract contract;
    private RepaymentSchedule schedule;

    @BeforeEach
    void setUp() {
        transactionManager = new RecordingTransactionManager();
        events = transactionManager.events;
        service = new RepaymentManualTransferService(
                repaymentScheduleRepository,
                registrationRepository,
                ssafyTransferApiClient,
                notificationCommandService,
                guard,
                transferAttemptRepository,
                new TransactionTemplate(transactionManager)
        );

        txActiveAtApiCall = true;
        savedAttempt = null;

        lenient().doAnswer(inv -> {
            events.add("RELEASE");
            return null;
        }).when(guard).release(any(), any());

        lenient().when(transferAttemptRepository.save(any(TransferAttempt.class))).thenAnswer(inv -> {
            TransferAttempt attempt = inv.getArgument(0);
            ReflectionTestUtils.setField(attempt, "attemptSeq", ATTEMPT_SEQ);
            savedAttempt = attempt;
            return attempt;
        });
        lenient().when(transferAttemptRepository.findById(ATTEMPT_SEQ))
                .thenAnswer(inv -> Optional.ofNullable(savedAttempt));

        debtor = User.builder().userSeq(DEBTOR_SEQ).email("debtor@test.com")
                .ssafyUserKey("debtor-key").state("ACTIVE").build();
        User creditor = User.builder().userSeq(2).email("creditor@test.com")
                .ssafyUserKey("creditor-key").state("ACTIVE").build();
        contract = Contract.builder()
                .contractSeq(CONTRACT_SEQ).contractId("C-20260325-TEST01")
                .creditor(creditor).debtor(debtor)
                .principalAmount(new BigDecimal("5000000")).status("ACTIVE")
                .startDt(LocalDate.of(2026, 1, 1)).endDt(LocalDate.of(2027, 1, 1))
                .build();
        schedule = RepaymentSchedule.builder()
                .scheduleSeq(SCHEDULE_SEQ).contract(contract).installmentNo(1)
                .totalDue(new BigDecimal("500000")).status("NONPAID").build();
        contract.addRepaymentSchedule(schedule);
        // 남은 회차가 있어 계약은 종료되지 않는다
        contract.addRepaymentSchedule(RepaymentSchedule.builder()
                .scheduleSeq(2).contract(contract).installmentNo(2)
                .totalDue(new BigDecimal("500000")).status("NONPAID").build());
    }

    private void givenLockedAndFound() {
        given(guard.tryAcquire(SCHEDULE_SEQ)).willReturn(LOCK_TOKEN);
        given(repaymentScheduleRepository.findByScheduleSeqAndContractSeqWithContractUsers(SCHEDULE_SEQ, CONTRACT_SEQ))
                .willReturn(Optional.of(schedule));
    }

    private void givenRegistration() {
        BankAccount debtorAccount = BankAccount.builder().accountSeq(10).accountNo("0012345678901234").build();
        BankAccount creditorAccount = BankAccount.builder().accountSeq(20).accountNo("0098765432109876").build();
        given(registrationRepository.findActiveWithAccounts(CONTRACT_SEQ)).willReturn(Optional.of(
                AutoTransferRegistration.builder().registrationSeq(1).contract(contract)
                        .debtorAccount(debtorAccount).creditorAccount(creditorAccount)
                        .amount(new BigDecimal("5000000")).transferDay(15).isActive(true).build()));
    }

    private void givenApiSucceeds() {
        given(ssafyTransferApiClient.transfer(anyString(), anyString(), any(), anyString(), anyString()))
                .willAnswer(inv -> {
                    events.add("API");
                    txActiveAtApiCall = TransactionSynchronizationManager.isActualTransactionActive();
                    return new SsafyTransferResponse();
                });
    }

    private void givenApiThrows(RuntimeException exception) {
        given(ssafyTransferApiClient.transfer(anyString(), anyString(), any(), anyString(), anyString()))
                .willAnswer(inv -> {
                    events.add("API");
                    throw exception;
                });
    }

    // ──────────────────────────────────────────────

    @Nested
    @DisplayName("성공")
    class Success {

        @Test
        @DisplayName("채무자가 이체하면 → 회차 PAID, 시도 SUCCEEDED, 응답에 계좌 정보")
        void transfer_success() {
            givenLockedAndFound();
            givenRegistration();
            givenApiSucceeds();

            RepaymentScheduleTransferResponseDto response =
                    service.transferRepaymentSchedule(CONTRACT_SEQ, SCHEDULE_SEQ, DEBTOR_SEQ);

            assertThat(response.status()).isEqualTo("PAID");
            assertThat(response.withdrawalAccountNo()).isEqualTo("0012345678901234");
            assertThat(response.depositAccountNo()).isEqualTo("0098765432109876");
            assertThat(schedule.getStatus()).isEqualTo("PAID");
            assertThat(savedAttempt.getStatus()).isEqualTo(TransferAttempt.Status.SUCCEEDED);
        }

        @Test
        @DisplayName("적요는 자동이체와 같은 형식이다")
        void transfer_shouldUseSameMemoFormatAsAutoTransfer() {
            givenLockedAndFound();
            givenRegistration();
            givenApiSucceeds();

            service.transferRepaymentSchedule(CONTRACT_SEQ, SCHEDULE_SEQ, DEBTOR_SEQ);

            then(ssafyTransferApiClient).should().transfer(
                    anyString(), anyString(), eq(new BigDecimal("500000")),
                    eq("BL1-1 1회차 C-20260325-TEST01"), eq("debtor-key"));
        }

        @Test
        @DisplayName("은행 API는 트랜잭션 밖에서 호출되고, 시도 기록이 먼저 커밋된다")
        void transfer_shouldFollowThreeStageStructure() {
            givenLockedAndFound();
            givenRegistration();
            givenApiSucceeds();

            service.transferRepaymentSchedule(CONTRACT_SEQ, SCHEDULE_SEQ, DEBTOR_SEQ);

            assertThat(txActiveAtApiCall).isFalse();
            assertThat(events).containsExactly("BEGIN", "COMMIT", "API", "BEGIN", "COMMIT", "RELEASE");
        }

        @Test
        @DisplayName("연체(OVERDUE) 회차도 수동 이체할 수 있다")
        void transfer_overdue_shouldBeAllowed() {
            ReflectionTestUtils.setField(schedule, "status", "OVERDUE");
            givenLockedAndFound();
            givenRegistration();
            givenApiSucceeds();

            RepaymentScheduleTransferResponseDto response =
                    service.transferRepaymentSchedule(CONTRACT_SEQ, SCHEDULE_SEQ, DEBTOR_SEQ);

            assertThat(response.status()).isEqualTo("PAID");
        }
    }

    // ──────────────────────────────────────────────

    @Nested
    @DisplayName("미해결 시도 게이트")
    class Gate {

        @Test
        @DisplayName("자동이체가 결과를 모른 채 남긴 시도가 있으면 → 수동 이체도 막힌다")
        void unresolvedAttempt_shouldBlockManualTransfer() {
            givenLockedAndFound();
            given(transferAttemptRepository.existsByScheduleSeqAndStatus(
                    SCHEDULE_SEQ, TransferAttempt.Status.REQUESTED)).willReturn(true);

            assertThatThrownBy(() -> service.transferRepaymentSchedule(CONTRACT_SEQ, SCHEDULE_SEQ, DEBTOR_SEQ))
                    .isInstanceOf(CustomBusinessException.class)
                    .satisfies(e -> assertThat(((CustomBusinessException) e).getErrorCode())
                            .isEqualTo(ErrorCode.DUPLICATE_REQUEST));

            then(ssafyTransferApiClient).shouldHaveNoInteractions();
            then(transferAttemptRepository).should(never()).save(any());
            assertThat(schedule.getStatus()).isEqualTo("NONPAID");
        }
    }

    // ──────────────────────────────────────────────

    @Nested
    @DisplayName("실패")
    class Failure {

        @Test
        @DisplayName("결과를 모르면 → 사용자에게 확인 중임을 알리고, 시도는 REQUESTED로 남는다")
        void unknownOutcome_shouldLeaveAttemptRequestedAndTellUser() {
            givenLockedAndFound();
            givenRegistration();
            givenApiThrows(new TransferOutcomeUnknownException("SSAFY_TRANSFER_UNKNOWN|IO|Read timed out"));

            assertThatThrownBy(() -> service.transferRepaymentSchedule(CONTRACT_SEQ, SCHEDULE_SEQ, DEBTOR_SEQ))
                    .isInstanceOf(CustomBusinessException.class)
                    .hasMessageContaining("다시 이체하지 말고")
                    .satisfies(e -> assertThat(((CustomBusinessException) e).getErrorCode())
                            .isEqualTo(ErrorCode.DUPLICATE_REQUEST));

            assertThat(savedAttempt.getStatus()).isEqualTo(TransferAttempt.Status.REQUESTED);
            assertThat(schedule.getStatus()).isEqualTo("NONPAID");
            assertThat(events).containsExactly("BEGIN", "COMMIT", "API", "RELEASE");
        }

        @Test
        @DisplayName("은행이 거절하면 → 예외를 전파하고 시도는 FAILED로 닫힌다")
        void rejection_shouldCloseAttemptAsFailed() {
            givenLockedAndFound();
            givenRegistration();
            givenApiThrows(new TransferFailException("SSAFY_TRANSFER_FAILED|A1014|잔액 부족"));

            assertThatThrownBy(() -> service.transferRepaymentSchedule(CONTRACT_SEQ, SCHEDULE_SEQ, DEBTOR_SEQ))
                    .isInstanceOf(TransferFailException.class);

            assertThat(savedAttempt.getStatus()).isEqualTo(TransferAttempt.Status.FAILED);
            assertThat(savedAttempt.getResponseCode()).isEqualTo("A1014");
            assertThat(schedule.getStatus()).isEqualTo("NONPAID");
            assertThat(events).containsExactly("BEGIN", "COMMIT", "API", "BEGIN", "COMMIT", "RELEASE");
        }
    }

    // ──────────────────────────────────────────────

    @Nested
    @DisplayName("검증과 락")
    class Validation {

        @Test
        @DisplayName("채무자가 아니면 → ACCESS_DENIED, 시도도 이체도 없다")
        void notDebtor_shouldBeDeniedWithoutAttempt() {
            givenLockedAndFound();

            assertThatThrownBy(() -> service.transferRepaymentSchedule(CONTRACT_SEQ, SCHEDULE_SEQ, 999))
                    .isInstanceOf(CustomBusinessException.class)
                    .satisfies(e -> assertThat(((CustomBusinessException) e).getErrorCode())
                            .isEqualTo(ErrorCode.ACCESS_DENIED));

            then(transferAttemptRepository).should(never()).save(any());
            then(ssafyTransferApiClient).shouldHaveNoInteractions();
            assertThat(events).containsExactly("BEGIN", "ROLLBACK", "RELEASE");
        }

        @Test
        @DisplayName("이미 납부된 회차면 → 이체하지 않는다")
        void alreadyPaid_shouldBeRejected() {
            schedule.markPaid(java.time.LocalDateTime.now());
            givenLockedAndFound();

            assertThatThrownBy(() -> service.transferRepaymentSchedule(CONTRACT_SEQ, SCHEDULE_SEQ, DEBTOR_SEQ))
                    .isInstanceOf(CustomBusinessException.class);

            then(ssafyTransferApiClient).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("락을 얻지 못하면 → DUPLICATE_REQUEST, 아무것도 건드리지 않고 해제도 하지 않는다")
        void lockNotAcquired_shouldRejectWithoutSideEffect() {
            given(guard.tryAcquire(SCHEDULE_SEQ)).willReturn(null);

            assertThatThrownBy(() -> service.transferRepaymentSchedule(CONTRACT_SEQ, SCHEDULE_SEQ, DEBTOR_SEQ))
                    .isInstanceOf(CustomBusinessException.class)
                    .satisfies(e -> assertThat(((CustomBusinessException) e).getErrorCode())
                            .isEqualTo(ErrorCode.DUPLICATE_REQUEST));

            then(repaymentScheduleRepository).shouldHaveNoInteractions();
            then(transferAttemptRepository).shouldHaveNoInteractions();
            then(ssafyTransferApiClient).shouldHaveNoInteractions();
            then(guard).should(never()).release(any(), any());
            assertThat(events).isEmpty();
        }
    }
}
