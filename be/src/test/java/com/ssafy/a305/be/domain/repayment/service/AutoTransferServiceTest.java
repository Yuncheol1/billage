package com.ssafy.a305.be.domain.repayment.service;

import com.ssafy.a305.be.domain.account.entity.BankAccount;
import com.ssafy.a305.be.domain.contract.entity.Contract;
import com.ssafy.a305.be.domain.notification.service.NotificationCommandService;
import com.ssafy.a305.be.domain.repayment.entity.AutoTransferRegistration;
import com.ssafy.a305.be.domain.repayment.entity.RepaymentEvent;
import com.ssafy.a305.be.domain.repayment.entity.RepaymentSchedule;
import com.ssafy.a305.be.domain.repayment.entity.RepaymentScheduleChangeReason;
import com.ssafy.a305.be.domain.repayment.repository.AutoTransferRegistrationRepository;
import com.ssafy.a305.be.domain.repayment.repository.RepaymentEventRepository;
import com.ssafy.a305.be.domain.repayment.repository.RepaymentScheduleRepository;
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
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;

/**
 * 자동이체 1건 처리 테스트.
 *
 * <p>{@link AutoTransferService}를 직접 생성해 <b>실제 운영 코드</b>를 호출하고, DB·외부 API만
 * Mockito로 격리한다. 트랜잭션 경계는 {@link RecordingTransactionManager}로 기록해,
 * "외부 호출이 트랜잭션 밖에서 일어나는가", "선점이 호출보다 먼저 커밋되는가" 같은
 * 구조 자체를 검증한다.</p>
 *
 * <p>조건부 UPDATE(선점·복귀)는 저장소 목이 DB처럼 동작하도록 흉내 낸다. 조회한 상태와 같을 때만
 * 픽스쳐 회차의 상태를 바꾸고 1을, 아니면 0을 돌려준다.</p>
 */
@ExtendWith(MockitoExtension.class)
class AutoTransferServiceTest {

    private static final Integer SCHEDULE_SEQ = 1;
    private static final Integer CONTRACT_SEQ = 100;

    @Mock
    private RepaymentScheduleRepository repaymentScheduleRepository;
    @Mock
    private AutoTransferRegistrationRepository registrationRepository;
    @Mock
    private RepaymentEventRepository repaymentEventRepository;
    @Mock
    private NotificationCommandService notificationCommandService;
    @Mock
    private SsafyTransferApiClient ssafyTransferApiClient;

    private RecordingTransactionManager transactionManager;
    private List<String> events;
    private AutoTransferService service;
    private RecordingHistoryRecorder historyRecorder;

    // ──── API 호출 시점에 관찰한 값 ────
    private boolean txActiveAtApiCall;
    private String scheduleStatusAtApiCall;
    /** 선점·복귀 UPDATE가 상태를 바꿀 대상. 조회 스터빙과 같은 객체다. */
    private RepaymentSchedule target;

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
        historyRecorder = new RecordingHistoryRecorder();
        service = new AutoTransferService(
                repaymentScheduleRepository,
                registrationRepository,
                repaymentEventRepository,
                notificationCommandService,
                ssafyTransferApiClient,
                new TransactionTemplate(transactionManager),
                historyRecorder
        );

        txActiveAtApiCall = true; // 호출이 일어나지 않았다면 값이 바뀌지 않아 눈에 띈다
        scheduleStatusAtApiCall = null;
        target = null;

        // 선점: 조회한 상태 그대로일 때만 REQUESTED로 바꾼다.
        lenient().when(repaymentScheduleRepository.claimForTransfer(any(), anyString(), any()))
                .thenAnswer(inv -> {
                    events.add("CLAIM");
                    String expected = inv.getArgument(1);
                    if (target == null || !expected.equals(target.getStatus())) {
                        return 0;
                    }
                    ReflectionTestUtils.setField(target, "status", "REQUESTED");
                    ReflectionTestUtils.setField(target, "transferRequestedDttm", inv.getArgument(2));
                    return 1;
                });
        // 확정: REQUESTED일 때만 PAID로 바꾼다.
        lenient().when(repaymentScheduleRepository.markPaidIfRequested(any(), any()))
                .thenAnswer(inv -> {
                    if (target == null || !"REQUESTED".equals(target.getStatus())) {
                        return 0;
                    }
                    ReflectionTestUtils.setField(target, "status", "PAID");
                    ReflectionTestUtils.setField(target, "paidDttm", inv.getArgument(1));
                    return 1;
                });
        // 복귀: REQUESTED일 때만 되돌린다.
        lenient().when(repaymentScheduleRepository.releaseTransferClaim(any(), anyString()))
                .thenAnswer(inv -> {
                    if (target == null || !"REQUESTED".equals(target.getStatus())) {
                        return 0;
                    }
                    ReflectionTestUtils.setField(target, "status", inv.getArgument(1));
                    return 1;
                });

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

    /** 회차 조회 성공. 선점·복귀도 이 회차를 대상으로 한다. */
    private void givenFound(RepaymentSchedule schedule) {
        target = schedule;
        given(repaymentScheduleRepository.findByScheduleSeqWithContractUsers(SCHEDULE_SEQ))
                .willReturn(Optional.of(schedule));
    }

    /** 회차 + 자동이체 등록까지 준비. */
    private void givenReadyToTransfer(RepaymentSchedule schedule, String registrationAmount) {
        givenFound(schedule);
        given(registrationRepository.findActiveWithAccounts(CONTRACT_SEQ))
                .willReturn(Optional.of(newRegistration(registrationAmount)));
    }

    /** 이체 API가 성공한다. 호출 시점의 트랜잭션·회차 상태를 관찰해 둔다. */
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
        scheduleStatusAtApiCall = target == null ? null : target.getStatus();
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

        @Test
        @DisplayName("transferMemo(): 적요는 BL + 회차 PK다")
        void transferMemo_shouldBePrefixedSchedulePk() {
            assertThat(newSchedule(12345, "500000").transferMemo()).isEqualTo("BL12345");
        }
    }

    // ──────────────────────────────────────────────

    @Nested
    @DisplayName("성공 케이스")
    class SuccessTests {

        @Test
        @DisplayName("API 성공 시 → 회차 PAID, 상환 이벤트 저장, 적요는 회차 식별자")
        void execute_success_shouldMarkPaidAndSaveEvent() {
            RepaymentSchedule schedule = newSchedule(SCHEDULE_SEQ, "500000");
            newSchedule(2, "500000"); // 남은 회차가 있어 계약은 종료되지 않는다
            givenReadyToTransfer(schedule, "500000");
            givenApiSucceeds();

            AutoTransferService.Result result = service.execute(SCHEDULE_SEQ);

            assertThat(result).isEqualTo(AutoTransferService.Result.SUCCEEDED);
            assertThat(schedule.getStatus()).isEqualTo("PAID");
            assertThat(schedule.getPaidDttm()).isNotNull();

            then(ssafyTransferApiClient).should().transfer(
                    eq("0012345678901234"),   // 채무자 출금 계좌
                    eq("0098765432109876"),   // 채권자 입금 계좌
                    eq(new BigDecimal("500000")),
                    eq("BL1"),
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
        @DisplayName("선점은 API 호출보다 먼저 커밋된다")
        void execute_claim_shouldBeCommittedBeforeApiCall() {
            RepaymentSchedule schedule = newSchedule(SCHEDULE_SEQ, "500000");
            newSchedule(2, "500000");
            givenReadyToTransfer(schedule, "500000");
            givenApiSucceeds();

            service.execute(SCHEDULE_SEQ);

            // Tx1(선점) 커밋 → API → Tx2 커밋
            assertThat(events).containsExactly("BEGIN", "CLAIM", "COMMIT", "API", "BEGIN", "COMMIT");
        }

        @Test
        @DisplayName("API를 호출하는 시점의 회차 상태는 REQUESTED이고 요청 시각이 남는다")
        void execute_schedule_shouldBeRequestedWhileApiCall() {
            RepaymentSchedule schedule = newSchedule(SCHEDULE_SEQ, "500000");
            newSchedule(2, "500000");
            givenReadyToTransfer(schedule, "500000");
            givenApiSucceeds();

            service.execute(SCHEDULE_SEQ);

            assertThat(scheduleStatusAtApiCall).isEqualTo("REQUESTED");
            assertThat(schedule.getTransferRequestedDttm()).isNotNull();
        }

        @Test
        @DisplayName("선점은 조회한 상태를 조건으로 건다")
        void execute_claim_shouldUseReadStatusAsCondition() {
            RepaymentSchedule schedule = newSchedule(SCHEDULE_SEQ, "500000");
            newSchedule(2, "500000");
            givenReadyToTransfer(schedule, "500000");
            givenApiSucceeds();

            service.execute(SCHEDULE_SEQ);

            then(repaymentScheduleRepository).should().claimForTransfer(eq(SCHEDULE_SEQ), eq("NONPAID"), any());
        }
    }

    // ──────────────────────────────────────────────

    @Nested
    @DisplayName("결과 불명 — 돈이 나갔는지 모르는 경우")
    class UnknownOutcomeTests {

        @Test
        @DisplayName("타임아웃 등으로 결과를 모르면 → UNRESOLVED, 회차는 REQUESTED로 남는다")
        void execute_unknownOutcome_shouldLeaveScheduleRequested() {
            RepaymentSchedule schedule = newSchedule(SCHEDULE_SEQ, "500000");
            givenReadyToTransfer(schedule, "500000");
            givenApiThrows(new TransferOutcomeUnknownException("SSAFY_TRANSFER_UNKNOWN|IO|Read timed out"));

            AutoTransferService.Result result = service.execute(SCHEDULE_SEQ);

            assertThat(result).isEqualTo(AutoTransferService.Result.UNRESOLVED);
            assertThat(schedule.getStatus())
                    .as("결과 불명을 미납으로 되돌리면 다음 슬롯이 재시도해 이중 출금이 된다")
                    .isEqualTo("REQUESTED");
            then(repaymentScheduleRepository).should(never()).releaseTransferClaim(any(), anyString());
        }

        @Test
        @DisplayName("결과 불명이면 상환 이벤트도 알림도 남기지 않는다")
        void execute_unknownOutcome_shouldNotRecordEvent() {
            RepaymentSchedule schedule = newSchedule(SCHEDULE_SEQ, "500000");
            givenReadyToTransfer(schedule, "500000");
            givenApiThrows(new TransferOutcomeUnknownException("timeout"));

            service.execute(SCHEDULE_SEQ);

            then(repaymentEventRepository).shouldHaveNoInteractions();
            then(notificationCommandService).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("결과 불명이면 결과 반영 없이, 이력만 짧은 트랜잭션으로 남긴다")
        void execute_unknownOutcome_shouldOnlyRecordHistory() {
            RepaymentSchedule schedule = newSchedule(SCHEDULE_SEQ, "500000");
            givenReadyToTransfer(schedule, "500000");
            givenApiThrows(new TransferOutcomeUnknownException("timeout"));

            service.execute(SCHEDULE_SEQ);

            assertThat(events).containsExactly("BEGIN", "CLAIM", "COMMIT", "API", "BEGIN", "COMMIT");
            then(repaymentScheduleRepository).should(never()).markPaidIfRequested(any(), any());
            then(repaymentScheduleRepository).should(never()).releaseTransferClaim(any(), anyString());
        }
    }

    // ──────────────────────────────────────────────

    @Nested
    @DisplayName("확정 실패 — 은행이 거절한 경우")
    class DefiniteFailureTests {

        @Test
        @DisplayName("은행이 거절하면 → 예외를 전파하고, 회차는 NONPAID로 돌아가 다음 슬롯에 다시 대상이 된다")
        void execute_rejection_shouldRestoreNonpaid() {
            RepaymentSchedule schedule = newSchedule(SCHEDULE_SEQ, "500000");
            givenReadyToTransfer(schedule, "500000");
            givenApiThrows(new TransferFailException("SSAFY_TRANSFER_FAILED|A1014|잔액 부족"));

            assertThatThrownBy(() -> service.execute(SCHEDULE_SEQ))
                    .isInstanceOf(TransferFailException.class)
                    .isNotInstanceOf(TransferOutcomeUnknownException.class)
                    .hasMessageContaining("A1014");

            assertThat(schedule.getStatus()).isEqualTo("NONPAID");
            assertThat(schedule.getPaidDttm()).isNull();
            then(repaymentScheduleRepository).should().releaseTransferClaim(SCHEDULE_SEQ, "NONPAID");
            then(repaymentEventRepository).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("응답코드가 없는 실패(연결 실패 등)도 NONPAID로 되돌린다")
        void execute_failureWithoutCode_shouldRestoreNonpaid() {
            RepaymentSchedule schedule = newSchedule(SCHEDULE_SEQ, "500000");
            givenReadyToTransfer(schedule, "500000");
            givenApiThrows(new TransferFailException("SSAFY 이체 API 연결 실패: Connection refused"));

            assertThatThrownBy(() -> service.execute(SCHEDULE_SEQ)).isInstanceOf(TransferFailException.class);

            assertThat(schedule.getStatus()).isEqualTo("NONPAID");
        }

        @Test
        @DisplayName("복귀는 별도의 짧은 트랜잭션으로 커밋된다")
        void execute_rejection_shouldReleaseInOwnTransaction() {
            RepaymentSchedule schedule = newSchedule(SCHEDULE_SEQ, "500000");
            givenReadyToTransfer(schedule, "500000");
            givenApiThrows(new TransferFailException("SSAFY_TRANSFER_FAILED|A1014|잔액 부족"));

            assertThatThrownBy(() -> service.execute(SCHEDULE_SEQ)).isInstanceOf(TransferFailException.class);

            assertThat(events).containsExactly("BEGIN", "CLAIM", "COMMIT", "API", "BEGIN", "COMMIT");
        }
    }

    // ──────────────────────────────────────────────

    @Nested
    @DisplayName("이체 성공 후 결과 반영 실패")
    class CompletionFailureTests {

        @Test
        @DisplayName("결과 반영 전에 정산이 먼저 PAID로 확정했으면 → 이벤트·알림 없이 조용히 끝난다")
        void execute_reconciledFirst_shouldSkipQuietly() {
            RepaymentSchedule schedule = newSchedule(SCHEDULE_SEQ, "500000");
            givenReadyToTransfer(schedule, "500000");
            // 이체 API 응답을 기다리는 사이 정산이 같은 회차를 확정한 상황이다.
            given(ssafyTransferApiClient.transfer(anyString(), anyString(), any(), anyString(), anyString()))
                    .willAnswer(inv -> {
                        ReflectionTestUtils.setField(schedule, "status", "PAID");
                        return new SsafyTransferResponse();
                    });

            assertThat(service.execute(SCHEDULE_SEQ)).isEqualTo(AutoTransferService.Result.SUCCEEDED);

            then(repaymentEventRepository).shouldHaveNoInteractions();
            then(notificationCommandService).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("이체는 성공했는데 반영 트랜잭션이 실패하면 → Tx2만 롤백된다")
        void execute_completionFailure_shouldRollbackOnlySecondTransaction() {
            RepaymentSchedule schedule = newSchedule(SCHEDULE_SEQ, "500000");
            newSchedule(2, "500000");
            givenReadyToTransfer(schedule, "500000");
            givenApiSucceeds();
            given(repaymentEventRepository.save(any(RepaymentEvent.class)))
                    .willThrow(new IllegalStateException("DB down"));

            assertThatThrownBy(() -> service.execute(SCHEDULE_SEQ))
                    .isInstanceOf(IllegalStateException.class);

            // 선점(Tx1)은 이미 커밋되어 있어 회차는 REQUESTED로 남고, 정산이 바로잡는다.
            assertThat(events).containsExactly("BEGIN", "CLAIM", "COMMIT", "API", "BEGIN", "ROLLBACK");
        }

        @Test
        @DisplayName("결과 반영 시점에 회차가 REQUESTED가 아니면 덮어쓰지 않는다")
        void execute_scheduleNotRequestedAtCompletion_shouldNotOverwrite() {
            RepaymentSchedule schedule = newSchedule(SCHEDULE_SEQ, "500000");
            givenReadyToTransfer(schedule, "500000");
            givenApiSucceeds();
            // 선점은 성공했다고 답하지만 상태는 바꾸지 않는다. 그 사이 다른 경로가 상태를 바꾼 상황이다.
            // 이미 응답 로직이 걸린 스터빙이라 given(...) 형태로 덮으면 스터빙 중 그 로직이 실행된다.
            willReturn(1).given(repaymentScheduleRepository).claimForTransfer(any(), anyString(), any());

            assertThatThrownBy(() -> service.execute(SCHEDULE_SEQ))
                    .isInstanceOf(IllegalStateException.class);

            assertThat(schedule.getStatus()).isEqualTo("NONPAID");
            then(repaymentEventRepository).shouldHaveNoInteractions();
        }
    }

    // ──────────────────────────────────────────────

    @Nested
    @DisplayName("진입 차단")
    class GateTests {

        @Test
        @DisplayName("이전 이체의 결과가 확정되지 않은(REQUESTED) 회차면 → BLOCKED, 선점도 API도 없다")
        void execute_requestedSchedule_shouldBlockWithoutApiCall() {
            RepaymentSchedule schedule = newSchedule(SCHEDULE_SEQ, "500000");
            ReflectionTestUtils.setField(schedule, "status", "REQUESTED");
            givenFound(schedule);

            AutoTransferService.Result result = service.execute(SCHEDULE_SEQ);

            assertThat(result).isEqualTo(AutoTransferService.Result.BLOCKED);
            then(ssafyTransferApiClient).shouldHaveNoInteractions();
            then(repaymentScheduleRepository).should(never()).claimForTransfer(any(), anyString(), any());
        }

        @Test
        @DisplayName("조회와 선점 사이에 다른 요청이 먼저 선점하면 → BLOCKED, API를 호출하지 않는다")
        void execute_claimLost_shouldBlockWithoutApiCall() {
            RepaymentSchedule schedule = newSchedule(SCHEDULE_SEQ, "500000");
            givenReadyToTransfer(schedule, "500000");
            // 이미 응답 로직이 걸린 스터빙이라 given(...) 형태로 덮으면 스터빙 중 그 로직이 실행된다.
            willReturn(0).given(repaymentScheduleRepository).claimForTransfer(any(), anyString(), any());

            AutoTransferService.Result result = service.execute(SCHEDULE_SEQ);

            assertThat(result).isEqualTo(AutoTransferService.Result.BLOCKED);
            then(ssafyTransferApiClient).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("이미 납부된 회차면 → SKIPPED, 선점도 이체도 없다")
        void execute_alreadyPaid_shouldSkip() {
            RepaymentSchedule schedule = newSchedule(SCHEDULE_SEQ, "500000");
            schedule.markPaid(LocalDateTime.now());
            givenFound(schedule);

            AutoTransferService.Result result = service.execute(SCHEDULE_SEQ);

            assertThat(result).isEqualTo(AutoTransferService.Result.SKIPPED);
            then(ssafyTransferApiClient).shouldHaveNoInteractions();
            then(repaymentScheduleRepository).should(never()).claimForTransfer(any(), anyString(), any());
        }

        @Test
        @DisplayName("회차를 찾을 수 없으면 → SKIPPED")
        void execute_scheduleNotFound_shouldSkip() {
            given(repaymentScheduleRepository.findByScheduleSeqWithContractUsers(SCHEDULE_SEQ))
                    .willReturn(Optional.empty());

            assertThat(service.execute(SCHEDULE_SEQ)).isEqualTo(AutoTransferService.Result.SKIPPED);
            then(ssafyTransferApiClient).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("자동이체 등록이 없으면 → SKIPPED, 선점하지 않는다")
        void execute_noRegistration_shouldSkipWithoutClaim() {
            RepaymentSchedule schedule = newSchedule(SCHEDULE_SEQ, "500000");
            givenFound(schedule);
            given(registrationRepository.findActiveWithAccounts(CONTRACT_SEQ)).willReturn(Optional.empty());

            AutoTransferService.Result result = service.execute(SCHEDULE_SEQ);

            assertThat(result).isEqualTo(AutoTransferService.Result.SKIPPED);
            then(repaymentScheduleRepository).should(never()).claimForTransfer(any(), anyString(), any());
            then(ssafyTransferApiClient).shouldHaveNoInteractions();
            assertThat(schedule.getStatus()).isEqualTo("NONPAID");
        }
    }

    // ──────────────────────────────────────────────

    @Nested
    @DisplayName("상태 변경 이력")
    class HistoryTests {

        @Test
        @DisplayName("성공하면 → 선점과 확정이 각각 상태 변경과 같은 트랜잭션에서 남는다")
        void execute_success_shouldRecordClaimAndSuccess() {
            RepaymentSchedule schedule = newSchedule(SCHEDULE_SEQ, "500000");
            newSchedule(2, "500000");
            givenReadyToTransfer(schedule, "500000");
            givenApiSucceeds();

            service.execute(SCHEDULE_SEQ);

            assertThat(historyRecorder.entries).containsExactly(
                    new RecordingHistoryRecorder.Entry(SCHEDULE_SEQ, "NONPAID", "REQUESTED",
                            RepaymentScheduleChangeReason.AUTO_TRANSFER_REQUESTED, null, null, true),
                    new RecordingHistoryRecorder.Entry(SCHEDULE_SEQ, "REQUESTED", "PAID",
                            RepaymentScheduleChangeReason.TRANSFER_SUCCEEDED, null, null, true));
        }

        @Test
        @DisplayName("은행이 거절하면 → 되돌린 상태와 은행 응답을 남긴다")
        void execute_rejection_shouldRecordRejectionWithResponse() {
            RepaymentSchedule schedule = newSchedule(SCHEDULE_SEQ, "500000");
            givenReadyToTransfer(schedule, "500000");
            givenApiThrows(new TransferFailException("SSAFY_TRANSFER_FAILED|A1014|잔액 부족"));

            assertThatThrownBy(() -> service.execute(SCHEDULE_SEQ)).isInstanceOf(TransferFailException.class);

            assertThat(historyRecorder.entries.get(1)).isEqualTo(
                    new RecordingHistoryRecorder.Entry(SCHEDULE_SEQ, "REQUESTED", "NONPAID",
                            RepaymentScheduleChangeReason.TRANSFER_REJECTED, null,
                            "SSAFY_TRANSFER_FAILED|A1014|잔액 부족", true));
        }

        @Test
        @DisplayName("결과를 모르면 → 상태는 그대로 두고 결과 불명 사실과 사유를 남긴다")
        void execute_unknownOutcome_shouldRecordUnknown() {
            RepaymentSchedule schedule = newSchedule(SCHEDULE_SEQ, "500000");
            givenReadyToTransfer(schedule, "500000");
            givenApiThrows(new TransferOutcomeUnknownException("SSAFY_TRANSFER_UNKNOWN|IO|Read timed out"));

            service.execute(SCHEDULE_SEQ);

            assertThat(historyRecorder.entries.get(1)).isEqualTo(
                    new RecordingHistoryRecorder.Entry(SCHEDULE_SEQ, "REQUESTED", "REQUESTED",
                            RepaymentScheduleChangeReason.TRANSFER_OUTCOME_UNKNOWN, null,
                            "SSAFY_TRANSFER_UNKNOWN|IO|Read timed out", true));
        }

        @Test
        @DisplayName("결과 불명 이력 기록이 실패해도 → 결과는 UNRESOLVED, 회차는 REQUESTED로 남는다")
        void execute_unknownOutcomeHistoryFailure_shouldNotChangeResult() {
            RecordingHistoryRecorder failingOnUnknown = new RecordingHistoryRecorder() {
                @Override
                public void record(Integer scheduleSeq, String fromStatus, String toStatus,
                                   RepaymentScheduleChangeReason reason, Integer changedByUserSeq, String detail) {
                    if (reason == RepaymentScheduleChangeReason.TRANSFER_OUTCOME_UNKNOWN) {
                        throw new IllegalStateException("DB down");
                    }
                    super.record(scheduleSeq, fromStatus, toStatus, reason, changedByUserSeq, detail);
                }
            };
            service = new AutoTransferService(repaymentScheduleRepository, registrationRepository,
                    repaymentEventRepository, notificationCommandService, ssafyTransferApiClient,
                    new TransactionTemplate(transactionManager), failingOnUnknown);
            RepaymentSchedule schedule = newSchedule(SCHEDULE_SEQ, "500000");
            givenReadyToTransfer(schedule, "500000");
            givenApiThrows(new TransferOutcomeUnknownException("timeout"));

            assertThat(service.execute(SCHEDULE_SEQ)).isEqualTo(AutoTransferService.Result.UNRESOLVED);
            assertThat(schedule.getStatus()).isEqualTo("REQUESTED");
        }

        @Test
        @DisplayName("다른 요청이 먼저 선점했으면 → 이력을 남기지 않는다 (그 요청이 남긴다)")
        void execute_claimLost_shouldNotRecord() {
            RepaymentSchedule schedule = newSchedule(SCHEDULE_SEQ, "500000");
            givenReadyToTransfer(schedule, "500000");
            willReturn(0).given(repaymentScheduleRepository).claimForTransfer(any(), anyString(), any());

            service.execute(SCHEDULE_SEQ);

            assertThat(historyRecorder.entries).isEmpty();
        }

        @Test
        @DisplayName("정산이 먼저 확정했으면 → 확정 이력을 다시 남기지 않는다")
        void execute_reconciledFirst_shouldNotRecordSuccess() {
            RepaymentSchedule schedule = newSchedule(SCHEDULE_SEQ, "500000");
            givenReadyToTransfer(schedule, "500000");
            given(ssafyTransferApiClient.transfer(anyString(), anyString(), any(), anyString(), anyString()))
                    .willAnswer(inv -> {
                        ReflectionTestUtils.setField(schedule, "status", "PAID");
                        return new SsafyTransferResponse();
                    });

            service.execute(SCHEDULE_SEQ);

            assertThat(historyRecorder.reasons())
                    .containsExactly(RepaymentScheduleChangeReason.AUTO_TRANSFER_REQUESTED);
        }
    }
}
