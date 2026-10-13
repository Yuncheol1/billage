package com.ssafy.a305.be.domain.repayment.service;

import com.ssafy.a305.be.domain.account.entity.BankAccount;
import com.ssafy.a305.be.domain.contract.entity.Contract;
import com.ssafy.a305.be.domain.notification.service.NotificationCommandService;
import com.ssafy.a305.be.domain.repayment.dto.response.RepaymentScheduleTransferResponseDto;
import com.ssafy.a305.be.domain.repayment.entity.AutoTransferRegistration;
import com.ssafy.a305.be.domain.repayment.entity.RepaymentSchedule;
import com.ssafy.a305.be.domain.repayment.repository.AutoTransferRegistrationRepository;
import com.ssafy.a305.be.domain.repayment.repository.RepaymentScheduleRepository;
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
import static org.mockito.BDDMockito.willReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;

/**
 * 수동 이체 테스트.
 *
 * <p>자동이체와 같은 3단 구조를 따르는지, 그리고 <b>자동이체가 결과를 모른 채 남긴 회차를
 * 우회하지 못하는지</b>를 검증한다. 후자가 없으면 자동이체가 타임아웃으로 결과를 모르는 상태에서
 * 사용자가 수동 이체 버튼을 눌러 이중 출금이 될 수 있다.</p>
 */
@ExtendWith(MockitoExtension.class)
class RepaymentManualTransferServiceTest {

    private static final Integer CONTRACT_SEQ = 100;
    private static final Integer SCHEDULE_SEQ = 1;
    private static final Integer DEBTOR_SEQ = 1;
    private static final Integer CREDITOR_SEQ = 2;

    @Mock
    private RepaymentScheduleRepository repaymentScheduleRepository;
    @Mock
    private AutoTransferRegistrationRepository registrationRepository;
    @Mock
    private SsafyTransferApiClient ssafyTransferApiClient;
    @Mock
    private NotificationCommandService notificationCommandService;

    private RecordingTransactionManager transactionManager;
    private List<String> events;
    private RepaymentManualTransferService service;

    private boolean txActiveAtApiCall;

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
                new TransactionTemplate(transactionManager)
        );

        txActiveAtApiCall = true;

        // 선점·복귀 UPDATE를 DB처럼 흉내 낸다. 조건이 맞을 때만 픽스쳐 회차의 상태를 바꾼다.
        lenient().when(repaymentScheduleRepository.claimForTransfer(any(), anyString(), any()))
                .thenAnswer(inv -> {
                    events.add("CLAIM");
                    if (!inv.getArgument(1).equals(schedule.getStatus())) {
                        return 0;
                    }
                    ReflectionTestUtils.setField(schedule, "status", "REQUESTED");
                    return 1;
                });
        lenient().when(repaymentScheduleRepository.releaseTransferClaim(any(), anyString()))
                .thenAnswer(inv -> {
                    if (!"REQUESTED".equals(schedule.getStatus())) {
                        return 0;
                    }
                    ReflectionTestUtils.setField(schedule, "status", inv.getArgument(1));
                    return 1;
                });

        debtor = User.builder().userSeq(DEBTOR_SEQ).email("debtor@test.com")
                .ssafyUserKey("debtor-key").state("ACTIVE").build();
        User creditor = User.builder().userSeq(CREDITOR_SEQ).email("creditor@test.com")
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

    private void givenFound() {
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

    private void assertErrorCode(Throwable e, ErrorCode expected) {
        assertThat(((CustomBusinessException) e).getErrorCode()).isEqualTo(expected);
    }

    // ──────────────────────────────────────────────

    @Nested
    @DisplayName("성공")
    class Success {

        @Test
        @DisplayName("채무자가 이체하면 → 회차 PAID, 응답에 계좌 정보")
        void transfer_success() {
            givenFound();
            givenRegistration();
            givenApiSucceeds();

            RepaymentScheduleTransferResponseDto response =
                    service.transferRepaymentSchedule(CONTRACT_SEQ, SCHEDULE_SEQ, DEBTOR_SEQ);

            assertThat(response.status()).isEqualTo("PAID");
            assertThat(response.withdrawalAccountNo()).isEqualTo("0012345678901234");
            assertThat(response.depositAccountNo()).isEqualTo("0098765432109876");
            assertThat(schedule.getStatus()).isEqualTo("PAID");
        }

        @Test
        @DisplayName("적요는 자동이체와 같은 회차 식별자다")
        void transfer_shouldUseSameMemoAsAutoTransfer() {
            givenFound();
            givenRegistration();
            givenApiSucceeds();

            service.transferRepaymentSchedule(CONTRACT_SEQ, SCHEDULE_SEQ, DEBTOR_SEQ);

            then(ssafyTransferApiClient).should().transfer(
                    anyString(), anyString(), eq(new BigDecimal("500000")), eq("BL1"), eq("debtor-key"));
        }

        @Test
        @DisplayName("은행 API는 트랜잭션 밖에서 호출되고, 선점이 먼저 커밋된다")
        void transfer_shouldFollowThreeStageStructure() {
            givenFound();
            givenRegistration();
            givenApiSucceeds();

            service.transferRepaymentSchedule(CONTRACT_SEQ, SCHEDULE_SEQ, DEBTOR_SEQ);

            assertThat(txActiveAtApiCall).isFalse();
            assertThat(events).containsExactly("BEGIN", "CLAIM", "COMMIT", "API", "BEGIN", "COMMIT");
        }

        @Test
        @DisplayName("연체(OVERDUE) 회차도 수동 이체할 수 있고, 선점 조건은 OVERDUE다")
        void transfer_overdue_shouldBeAllowed() {
            ReflectionTestUtils.setField(schedule, "status", "OVERDUE");
            givenFound();
            givenRegistration();
            givenApiSucceeds();

            RepaymentScheduleTransferResponseDto response =
                    service.transferRepaymentSchedule(CONTRACT_SEQ, SCHEDULE_SEQ, DEBTOR_SEQ);

            assertThat(response.status()).isEqualTo("PAID");
            then(repaymentScheduleRepository).should().claimForTransfer(eq(SCHEDULE_SEQ), eq("OVERDUE"), any());
        }
    }

    // ──────────────────────────────────────────────

    @Nested
    @DisplayName("진입 차단")
    class Gate {

        @Test
        @DisplayName("자동이체가 결과를 모른 채 남긴(REQUESTED) 회차면 → 수동 이체도 막힌다")
        void requestedSchedule_shouldBlockManualTransfer() {
            ReflectionTestUtils.setField(schedule, "status", "REQUESTED");
            givenFound();

            assertThatThrownBy(() -> service.transferRepaymentSchedule(CONTRACT_SEQ, SCHEDULE_SEQ, DEBTOR_SEQ))
                    .isInstanceOf(CustomBusinessException.class)
                    .satisfies(e -> assertErrorCode(e, ErrorCode.DUPLICATE_REQUEST));

            then(ssafyTransferApiClient).shouldHaveNoInteractions();
            then(repaymentScheduleRepository).should(never()).claimForTransfer(any(), anyString(), any());
        }

        @Test
        @DisplayName("조회와 선점 사이에 자동이체가 먼저 선점하면 → DUPLICATE_REQUEST, API를 호출하지 않는다")
        void claimLost_shouldRejectWithoutApiCall() {
            givenFound();
            givenRegistration();
            // 이미 응답 로직이 걸린 스터빙이라 given(...) 형태로 덮으면 스터빙 중 그 로직이 실행된다.
            willReturn(0).given(repaymentScheduleRepository).claimForTransfer(any(), anyString(), any());

            assertThatThrownBy(() -> service.transferRepaymentSchedule(CONTRACT_SEQ, SCHEDULE_SEQ, DEBTOR_SEQ))
                    .isInstanceOf(CustomBusinessException.class)
                    .satisfies(e -> assertErrorCode(e, ErrorCode.DUPLICATE_REQUEST));

            then(ssafyTransferApiClient).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("이체 결과 확인 중인 회차는 채권자의 수동 완료 처리도 막힌다")
        void requestedSchedule_shouldBlockManualPaid() {
            ReflectionTestUtils.setField(schedule, "status", "REQUESTED");
            givenFound();

            assertThatThrownBy(() -> service.markRepaymentScheduleManualPaid(CONTRACT_SEQ, SCHEDULE_SEQ, CREDITOR_SEQ))
                    .isInstanceOf(CustomBusinessException.class)
                    .satisfies(e -> assertErrorCode(e, ErrorCode.INVALID_CONTRACT_STATUS));

            assertThat(schedule.getStatus())
                    .as("이체가 나갔을 수 있는 회차를 현금 수령으로 처리하면 채무자가 두 번 낸 셈이 된다")
                    .isEqualTo("REQUESTED");
        }

        @Test
        @DisplayName("확인 직후 이체가 회차를 선점했으면 수동 완료 처리가 덮어쓰지 않는다")
        void claimedRightAfterCheck_shouldNotOverwrite() {
            givenFound();
            given(repaymentScheduleRepository.markManualPaidIfUnpaid(eq(SCHEDULE_SEQ), any())).willReturn(0);

            assertThatThrownBy(() -> service.markRepaymentScheduleManualPaid(CONTRACT_SEQ, SCHEDULE_SEQ, CREDITOR_SEQ))
                    .isInstanceOf(CustomBusinessException.class)
                    .satisfies(e -> assertErrorCode(e, ErrorCode.DUPLICATE_REQUEST));
            then(notificationCommandService).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("미납 회차는 조건부 UPDATE를 통과해 MANUAL_PAID가 된다")
        void unpaid_shouldBeMarkedManualPaid() {
            givenFound();
            given(repaymentScheduleRepository.markManualPaidIfUnpaid(eq(SCHEDULE_SEQ), any())).willReturn(1);

            assertThat(service.markRepaymentScheduleManualPaid(CONTRACT_SEQ, SCHEDULE_SEQ, CREDITOR_SEQ).status())
                    .isEqualTo("MANUAL_PAID");
        }
    }

    // ──────────────────────────────────────────────

    @Nested
    @DisplayName("실패")
    class Failure {

        @Test
        @DisplayName("결과를 모르면 → 사용자에게 확인 중임을 알리고, 회차는 REQUESTED로 남는다")
        void unknownOutcome_shouldLeaveScheduleRequestedAndTellUser() {
            givenFound();
            givenRegistration();
            givenApiThrows(new TransferOutcomeUnknownException("SSAFY_TRANSFER_UNKNOWN|IO|Read timed out"));

            assertThatThrownBy(() -> service.transferRepaymentSchedule(CONTRACT_SEQ, SCHEDULE_SEQ, DEBTOR_SEQ))
                    .isInstanceOf(CustomBusinessException.class)
                    .hasMessageContaining("다시 이체하지 말고")
                    .satisfies(e -> assertErrorCode(e, ErrorCode.DUPLICATE_REQUEST));

            assertThat(schedule.getStatus()).isEqualTo("REQUESTED");
            assertThat(events).containsExactly("BEGIN", "CLAIM", "COMMIT", "API");
        }

        @Test
        @DisplayName("은행이 거절하면 → 예외를 전파하고 회차는 NONPAID로 돌아간다")
        void rejection_shouldRestoreNonpaid() {
            givenFound();
            givenRegistration();
            givenApiThrows(new TransferFailException("SSAFY_TRANSFER_FAILED|A1014|잔액 부족"));

            assertThatThrownBy(() -> service.transferRepaymentSchedule(CONTRACT_SEQ, SCHEDULE_SEQ, DEBTOR_SEQ))
                    .isInstanceOf(TransferFailException.class);

            assertThat(schedule.getStatus()).isEqualTo("NONPAID");
            assertThat(events).containsExactly("BEGIN", "CLAIM", "COMMIT", "API", "BEGIN", "COMMIT");
        }

        @Test
        @DisplayName("연체 회차가 거절되면 → OVERDUE로 돌아간다 (연체가 미납으로 바뀌지 않는다)")
        void rejection_onOverdue_shouldRestoreOverdue() {
            ReflectionTestUtils.setField(schedule, "status", "OVERDUE");
            givenFound();
            givenRegistration();
            givenApiThrows(new TransferFailException("SSAFY_TRANSFER_FAILED|A1014|잔액 부족"));

            assertThatThrownBy(() -> service.transferRepaymentSchedule(CONTRACT_SEQ, SCHEDULE_SEQ, DEBTOR_SEQ))
                    .isInstanceOf(TransferFailException.class);

            assertThat(schedule.getStatus()).isEqualTo("OVERDUE");
        }
    }

    // ──────────────────────────────────────────────

    @Nested
    @DisplayName("검증")
    class Validation {

        @Test
        @DisplayName("채무자가 아니면 → ACCESS_DENIED, 선점도 이체도 없다")
        void notDebtor_shouldBeDeniedWithoutClaim() {
            givenFound();

            assertThatThrownBy(() -> service.transferRepaymentSchedule(CONTRACT_SEQ, SCHEDULE_SEQ, 999))
                    .isInstanceOf(CustomBusinessException.class)
                    .satisfies(e -> assertErrorCode(e, ErrorCode.ACCESS_DENIED));

            then(repaymentScheduleRepository).should(never()).claimForTransfer(any(), anyString(), any());
            then(ssafyTransferApiClient).shouldHaveNoInteractions();
            assertThat(events).containsExactly("BEGIN", "ROLLBACK");
        }

        @Test
        @DisplayName("이미 납부된 회차면 → 이체하지 않는다")
        void alreadyPaid_shouldBeRejected() {
            schedule.markPaid(java.time.LocalDateTime.now());
            givenFound();

            assertThatThrownBy(() -> service.transferRepaymentSchedule(CONTRACT_SEQ, SCHEDULE_SEQ, DEBTOR_SEQ))
                    .isInstanceOf(CustomBusinessException.class);

            then(ssafyTransferApiClient).shouldHaveNoInteractions();
        }
    }
}
