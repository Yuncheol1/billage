package com.ssafy.a305.be.domain.repayment.service;

import com.ssafy.a305.be.domain.account.entity.BankAccount;
import com.ssafy.a305.be.domain.bank.client.SsafyDemandDepositClient;
import com.ssafy.a305.be.domain.bank.client.dto.response.SsafyDemandDepositTransactionRecDto;
import com.ssafy.a305.be.domain.bank.client.dto.response.SsafyInquireTransactionHistoryListRecDto;
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
import com.ssafy.a305.be.global.exception.CustomBusinessException;
import com.ssafy.a305.be.global.exception.dto.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
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
 * 결과 불명 회차 정산 테스트.
 *
 * <p>거래내역 조회 결과에 따라 회차를 확정·복귀·보류하는지, 그리고 <b>다른 회차의 거래를 이
 * 회차의 것으로 오인하지 않는지</b>를 검증한다. 오인하면 나가지 않은 이체를 납부로 처리하거나,
 * 나간 이체를 미납으로 되돌려 이중 출금이 된다.</p>
 */
@ExtendWith(MockitoExtension.class)
class TransferReconciliationServiceTest {

    private static final Integer SCHEDULE_SEQ = 12;
    private static final Integer CONTRACT_SEQ = 100;
    private static final String DEBTOR_ACCOUNT_NO = "0012345678901234";
    private static final LocalDateTime REQUESTED_AT = LocalDateTime.now().minusHours(4);

    @Mock
    private RepaymentScheduleRepository repaymentScheduleRepository;
    @Mock
    private AutoTransferRegistrationRepository registrationRepository;
    @Mock
    private RepaymentEventRepository repaymentEventRepository;
    @Mock
    private NotificationCommandService notificationCommandService;
    @Mock
    private SsafyDemandDepositClient ssafyDemandDepositClient;

    private RecordingTransactionManager transactionManager;
    private TransferReconciliationService service;
    private RecordingHistoryRecorder historyRecorder;

    private Contract contract;
    private RepaymentSchedule schedule;

    @BeforeEach
    void setUp() {
        transactionManager = new RecordingTransactionManager();
        historyRecorder = new RecordingHistoryRecorder();
        service = new TransferReconciliationService(
                repaymentScheduleRepository,
                registrationRepository,
                repaymentEventRepository,
                notificationCommandService,
                ssafyDemandDepositClient,
                new TransactionTemplate(transactionManager),
                historyRecorder
        );

        User debtor = User.builder().userSeq(1).email("debtor@test.com")
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
                .dueDt(LocalDate.now())
                .totalDue(new BigDecimal("500000"))
                .status("REQUESTED")
                .transferRequestedDttm(REQUESTED_AT)
                .build();
        contract.addRepaymentSchedule(schedule);
        // 남은 회차가 있어 계약은 종료되지 않는다
        contract.addRepaymentSchedule(RepaymentSchedule.builder()
                .scheduleSeq(13).contract(contract).installmentNo(2)
                .totalDue(new BigDecimal("500000")).status("NONPAID").build());

        lenient().when(repaymentScheduleRepository.findByScheduleSeqWithContractUsers(SCHEDULE_SEQ))
                .thenReturn(Optional.of(schedule));
        lenient().when(registrationRepository.findActiveWithAccounts(CONTRACT_SEQ)).thenReturn(Optional.of(
                AutoTransferRegistration.builder().registrationSeq(1).contract(contract)
                        .debtorAccount(BankAccount.builder().accountSeq(10).accountNo(DEBTOR_ACCOUNT_NO).build())
                        .creditorAccount(BankAccount.builder().accountSeq(20).accountNo("0098765432109876").build())
                        .amount(new BigDecimal("5000000")).transferDay(15).isActive(true).build()));
        // 확정: REQUESTED일 때만 PAID로 바꾼다.
        lenient().when(repaymentScheduleRepository.markPaidIfRequested(any(), any()))
                .thenAnswer(inv -> {
                    if (schedule == null || !"REQUESTED".equals(schedule.getStatus())) {
                        return 0;
                    }
                    ReflectionTestUtils.setField(schedule, "status", "PAID");
                    ReflectionTestUtils.setField(schedule, "paidDttm", inv.getArgument(1));
                    return 1;
                });
        // 복귀 UPDATE를 DB처럼 흉내 낸다.
        lenient().when(repaymentScheduleRepository.releaseTransferClaim(any(), anyString()))
                .thenAnswer(inv -> {
                    if (!"REQUESTED".equals(schedule.getStatus())) {
                        return 0;
                    }
                    ReflectionTestUtils.setField(schedule, "status", inv.getArgument(1));
                    return 1;
                });
    }

    // ──── 픽스쳐 헬퍼 ────

    private static SsafyDemandDepositTransactionRecDto row(String summary, String amount) {
        return new SsafyDemandDepositTransactionRecDto(
                "TX-" + summary, "20260325", "100003", "2", "출금(이체)",
                "0098765432109876", amount, "1000000", summary, "");
    }

    private void givenHistory(SsafyDemandDepositTransactionRecDto... rows) {
        given(ssafyDemandDepositClient.inquireTransactionHistoryList(
                anyString(), anyString(), anyString(), anyString(), anyString(), anyString()))
                .willReturn(new SsafyInquireTransactionHistoryListRecDto(
                        String.valueOf(rows.length), Arrays.asList(rows)));
    }

    // ──────────────────────────────────────────────

    @Nested
    @DisplayName("판정 결과에 따른 처리")
    class Outcome {

        @Test
        @DisplayName("출금 1건이어도 이체 결과 반영이 먼저 확정했으면 → 이벤트를 다시 만들지 않는다")
        void oneMatch_butConfirmedFirst_shouldSkip() {
            givenHistory(row("BL12", "500000"));
            willReturn(0).given(repaymentScheduleRepository).markPaidIfRequested(any(), any());

            assertThat(service.reconcile(SCHEDULE_SEQ)).isEqualTo(TransferReconciliationService.Result.SKIPPED);
            then(repaymentEventRepository).should(never()).save(any());
            then(notificationCommandService).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("일치하는 출금이 1건이면 → PAID로 확정하고 상환 이벤트를 남긴다")
        void oneMatch_shouldConfirmPaid() {
            givenHistory(row("BL12", "500000"));

            TransferReconciliationService.Result result = service.reconcile(SCHEDULE_SEQ);

            assertThat(result).isEqualTo(TransferReconciliationService.Result.CONFIRMED_PAID);
            assertThat(schedule.getStatus()).isEqualTo("PAID");
            assertThat(schedule.getPaidDttm()).isNotNull();

            ArgumentCaptor<RepaymentEvent> captor = ArgumentCaptor.forClass(RepaymentEvent.class);
            then(repaymentEventRepository).should().save(captor.capture());
            assertThat(captor.getValue().getAmountPaid()).isEqualByComparingTo(new BigDecimal("500000"));
            assertThat(captor.getValue().getRequestedDttm()).isEqualTo(REQUESTED_AT);
            then(notificationCommandService).should().sendRepaymentPaidNotifications(schedule);
        }

        @Test
        @DisplayName("출금이 없고 납부 기한이 지나지 않았으면 → NONPAID로 되돌린다")
        void noMatch_beforeDue_shouldReleaseToNonpaid() {
            givenHistory(row("다른 거래", "500000"));

            TransferReconciliationService.Result result = service.reconcile(SCHEDULE_SEQ);

            assertThat(result).isEqualTo(TransferReconciliationService.Result.RELEASED);
            assertThat(schedule.getStatus()).isEqualTo("NONPAID");
            then(repaymentEventRepository).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("출금이 없고 납부 기한이 지났으면 → OVERDUE로 되돌린다")
        void noMatch_afterDue_shouldReleaseToOverdue() {
            ReflectionTestUtils.setField(schedule, "dueDt", LocalDate.now().minusDays(1));
            givenHistory();

            service.reconcile(SCHEDULE_SEQ);

            assertThat(schedule.getStatus()).isEqualTo("OVERDUE");
        }

        @Test
        @DisplayName("일치하는 출금이 2건 이상이면 → 이중 출금 가능성이 있어 REQUESTED로 두고 수동 확인")
        void multipleMatches_shouldNeedReview() {
            givenHistory(row("BL12", "500000"), row("BL12", "500000"));

            TransferReconciliationService.Result result = service.reconcile(SCHEDULE_SEQ);

            assertThat(result).isEqualTo(TransferReconciliationService.Result.NEEDS_REVIEW);
            assertThat(schedule.getStatus()).isEqualTo("REQUESTED");
            then(repaymentScheduleRepository).should(never()).releaseTransferClaim(any(), anyString());
            then(repaymentEventRepository).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("거래내역 조회가 실패하면 → 나가지 않았다고 단정하지 않고 REQUESTED로 둔다")
        void lookupFailure_shouldNeedReview() {
            given(ssafyDemandDepositClient.inquireTransactionHistoryList(
                    anyString(), anyString(), anyString(), anyString(), anyString(), anyString()))
                    .willThrow(new CustomBusinessException(ErrorCode.INVALID_INPUT_VALUE, "SSAFY 거래 내역 목록 조회 요청 실패"));

            TransferReconciliationService.Result result = service.reconcile(SCHEDULE_SEQ);

            assertThat(result).isEqualTo(TransferReconciliationService.Result.NEEDS_REVIEW);
            assertThat(schedule.getStatus()).isEqualTo("REQUESTED");
            then(repaymentScheduleRepository).should(never()).releaseTransferClaim(any(), anyString());
        }
    }

    // ──────────────────────────────────────────────

    @Nested
    @DisplayName("거래 판정")
    class Matching {

        @Test
        @DisplayName("다른 회차의 식별자(BL123)는 이 회차(BL12)의 출금으로 보지 않는다")
        void longerSeq_shouldNotMatch() {
            givenHistory(row("BL123", "500000"));

            assertThat(service.reconcile(SCHEDULE_SEQ)).isEqualTo(TransferReconciliationService.Result.RELEASED);
        }

        @Test
        @DisplayName("금액이 다르면 이 회차의 출금으로 보지 않는다")
        void differentAmount_shouldNotMatch() {
            givenHistory(row("BL12", "400000"));

            assertThat(service.reconcile(SCHEDULE_SEQ)).isEqualTo(TransferReconciliationService.Result.RELEASED);
        }

        @Test
        @DisplayName("은행이 적요 앞뒤에 문구를 붙이거나 금액에 부호가 붙어도 찾는다")
        void bankDecoratedSummary_shouldMatch() {
            givenHistory(row("(출금) BL12 이체", "-500000"));

            assertThat(service.reconcile(SCHEDULE_SEQ)).isEqualTo(TransferReconciliationService.Result.CONFIRMED_PAID);
        }

        @Test
        @DisplayName("이전 형식의 적요(BL12-1 1회차 ...)도 찾는다")
        void legacySummary_shouldMatch() {
            givenHistory(row("BL12-1 1회차 C-20260325-TEST01", "500000"));

            assertThat(service.reconcile(SCHEDULE_SEQ)).isEqualTo(TransferReconciliationService.Result.CONFIRMED_PAID);
        }

        @Test
        @DisplayName("채무자 출금 계좌를 요청일부터 오늘까지 조회한다")
        void lookup_shouldQueryDebtorAccountFromRequestedDate() {
            givenHistory(row("BL12", "500000"));

            service.reconcile(SCHEDULE_SEQ);

            then(ssafyDemandDepositClient).should().inquireTransactionHistoryList(
                    eq("debtor-key"),
                    eq(DEBTOR_ACCOUNT_NO),
                    eq(REQUESTED_AT.toLocalDate().format(java.time.format.DateTimeFormatter.BASIC_ISO_DATE)),
                    eq(LocalDate.now().format(java.time.format.DateTimeFormatter.BASIC_ISO_DATE)),
                    eq("A"),
                    eq("ASC"));
        }
    }

    // ──────────────────────────────────────────────

    @Nested
    @DisplayName("정산하지 않는 경우")
    class NotReconciled {

        @Test
        @DisplayName("그 사이 상태가 바뀌어 REQUESTED가 아니면 → SKIPPED, 조회하지 않는다")
        void notRequested_shouldSkip() {
            ReflectionTestUtils.setField(schedule, "status", "PAID");

            assertThat(service.reconcile(SCHEDULE_SEQ)).isEqualTo(TransferReconciliationService.Result.SKIPPED);
            then(ssafyDemandDepositClient).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("출금 계좌를 알 수 없으면 → 조회하지 않고 수동 확인")
        void noRegistration_shouldNeedReview() {
            given(registrationRepository.findActiveWithAccounts(CONTRACT_SEQ)).willReturn(Optional.empty());

            assertThat(service.reconcile(SCHEDULE_SEQ)).isEqualTo(TransferReconciliationService.Result.NEEDS_REVIEW);
            then(ssafyDemandDepositClient).shouldHaveNoInteractions();
            assertThat(schedule.getStatus()).isEqualTo("REQUESTED");
        }

        @Test
        @DisplayName("정산 대상은 요청한 지 10분 이상 지난 회차로 한정한다")
        void findTargets_shouldExcludeRecentRequests() {
            LocalDateTime now = LocalDateTime.of(2026, 3, 25, 14, 0);
            given(repaymentScheduleRepository.findStaleRequestedScheduleSeqs(now.minusMinutes(10)))
                    .willReturn(List.of(SCHEDULE_SEQ));

            assertThat(service.findTargets(now)).containsExactly(SCHEDULE_SEQ);
        }
    }

    // ──────────────────────────────────────────────

    @Nested
    @DisplayName("복귀 상태 결정")
    class RestoreStatus {

        @Test
        @DisplayName("기한 당일·이전이 아니면 NONPAID, 기한이 지났으면 OVERDUE")
        void restoreStatusOf() {
            LocalDate today = LocalDate.of(2026, 3, 25);

            assertThat(TransferReconciliationService.restoreStatusOf(today.plusDays(3), today)).isEqualTo("NONPAID");
            assertThat(TransferReconciliationService.restoreStatusOf(today, today)).isEqualTo("NONPAID");
            assertThat(TransferReconciliationService.restoreStatusOf(today.minusDays(1), today)).isEqualTo("OVERDUE");
        }
    }

    // ──────────────────────────────────────────────

    @Nested
    @DisplayName("운영자 확인")
    class Review {

        private final LocalDateTime now = REQUESTED_AT.plusHours(1);

        @Test
        @DisplayName("출금을 확인했으면 PAID로 확정하고, 근거를 상환 이벤트에 남긴다")
        void withdrawn_shouldConfirmPaidWithNote() {
            TransferReconciliationService.Result result =
                    service.resolveByReview(SCHEDULE_SEQ, true, "거래내역 출금 1건 확인", now);

            assertThat(result).isEqualTo(TransferReconciliationService.Result.CONFIRMED_PAID);
            assertThat(schedule.getStatus()).isEqualTo("PAID");
            ArgumentCaptor<RepaymentEvent> captor = ArgumentCaptor.forClass(RepaymentEvent.class);
            then(repaymentEventRepository).should().save(captor.capture());
            assertThat(captor.getValue().getNote()).isEqualTo("운영자 확인: 거래내역 출금 1건 확인");
        }

        @Test
        @DisplayName("출금이 없었으면 이체 전 상태로 되돌려 다시 이체되게 한다")
        void notWithdrawn_shouldRelease() {
            TransferReconciliationService.Result result =
                    service.resolveByReview(SCHEDULE_SEQ, false, "거래내역에 출금 없음", now);

            assertThat(result).isEqualTo(TransferReconciliationService.Result.RELEASED);
            assertThat(schedule.getStatus()).isEqualTo("NONPAID");
            then(repaymentEventRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("그 사이 다른 경로가 확정한 회차는 건드리지 않는다")
        void alreadyResolved_shouldSkip() {
            ReflectionTestUtils.setField(schedule, "status", "PAID");

            assertThat(service.resolveByReview(SCHEDULE_SEQ, false, "확인", now))
                    .isEqualTo(TransferReconciliationService.Result.SKIPPED);
            assertThat(schedule.getStatus()).isEqualTo("PAID");
            then(repaymentScheduleRepository).should(never()).releaseTransferClaim(any(), anyString());
        }

        @Test
        @DisplayName("요청한 지 10분이 지나지 않았으면 은행이 아직 처리 중일 수 있어 거절한다")
        void tooRecent_shouldReject() {
            LocalDateTime justAfter = REQUESTED_AT.plusMinutes(5);

            assertThatThrownBy(() -> service.resolveByReview(SCHEDULE_SEQ, false, "출금 없음", justAfter))
                    .isInstanceOf(CustomBusinessException.class);
            assertThat(schedule.getStatus()).isEqualTo("REQUESTED");
        }

        @Test
        @DisplayName("확인 근거가 없으면 거절한다")
        void blankNote_shouldReject() {
            assertThatThrownBy(() -> service.resolveByReview(SCHEDULE_SEQ, true, " ", now))
                    .isInstanceOf(CustomBusinessException.class);
            assertThat(schedule.getStatus()).isEqualTo("REQUESTED");
        }
    }

    // ──────────────────────────────────────────────

    @Nested
    @DisplayName("상태 변경 이력")
    class History {

        @Test
        @DisplayName("정산이 출금을 찾으면 → 정산 확정으로 남긴다")
        void reconcilePaid_shouldRecord() {
            givenHistory(row("BL12", "500000"));

            service.reconcile(SCHEDULE_SEQ);

            assertThat(historyRecorder.entries).containsExactly(
                    new RecordingHistoryRecorder.Entry(SCHEDULE_SEQ, "REQUESTED", "PAID",
                            RepaymentScheduleChangeReason.RECONCILED_PAID, null, "이체 결과 정산 확인", true));
        }

        @Test
        @DisplayName("정산이 출금이 없다고 판정하면 → 되돌린 상태와 함께 남긴다")
        void reconcileNotWithdrawn_shouldRecordRestoredStatus() {
            ReflectionTestUtils.setField(schedule, "dueDt", LocalDate.now().minusDays(1));
            givenHistory();

            service.reconcile(SCHEDULE_SEQ);

            assertThat(historyRecorder.entries).containsExactly(
                    new RecordingHistoryRecorder.Entry(SCHEDULE_SEQ, "REQUESTED", "OVERDUE",
                            RepaymentScheduleChangeReason.RECONCILED_NOT_WITHDRAWN, null,
                            "거래내역에 일치하는 출금 없음", true));
        }

        @Test
        @DisplayName("판단하지 못하면 → 상태가 그대로라 이력을 남기지 않는다")
        void needsReview_shouldNotRecord() {
            givenHistory(row("BL12", "500000"), row("BL12", "500000"));

            service.reconcile(SCHEDULE_SEQ);

            assertThat(historyRecorder.entries).isEmpty();
        }

        @Test
        @DisplayName("이체 결과 반영이 먼저 확정했으면 → 이력을 다시 남기지 않는다")
        void confirmedFirst_shouldNotRecord() {
            givenHistory(row("BL12", "500000"));
            willReturn(0).given(repaymentScheduleRepository).markPaidIfRequested(any(), any());

            service.reconcile(SCHEDULE_SEQ);

            assertThat(historyRecorder.entries).isEmpty();
        }

        @Test
        @DisplayName("운영자가 확인하면 → 확인 근거를 이력에 남긴다")
        void review_shouldRecordNote() {
            LocalDateTime now = REQUESTED_AT.plusHours(1);

            service.resolveByReview(SCHEDULE_SEQ, true, "거래내역 출금 1건 확인", now);
            ReflectionTestUtils.setField(schedule, "status", "REQUESTED");
            service.resolveByReview(SCHEDULE_SEQ, false, "거래내역에 출금 없음", now);

            assertThat(historyRecorder.entries)
                    .extracting(RecordingHistoryRecorder.Entry::reason, RecordingHistoryRecorder.Entry::detail)
                    .containsExactly(
                            org.assertj.core.groups.Tuple.tuple(
                                    RepaymentScheduleChangeReason.REVIEWED_PAID, "운영자 확인: 거래내역 출금 1건 확인"),
                            org.assertj.core.groups.Tuple.tuple(
                                    RepaymentScheduleChangeReason.REVIEWED_NOT_WITHDRAWN, "운영자 확인: 거래내역에 출금 없음"));
        }
    }
}
