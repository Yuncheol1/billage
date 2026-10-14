package com.ssafy.a305.be.domain.repayment.service;

import com.github.tomakehurst.wiremock.http.Fault;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.ssafy.a305.be.domain.account.entity.BankAccount;
import com.ssafy.a305.be.domain.auth.config.SsafyApiProperties;
import com.ssafy.a305.be.domain.bank.client.SsafyApiHeaderFactory;
import com.ssafy.a305.be.domain.bank.client.SsafyDemandDepositClient;
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
import com.ssafy.a305.be.global.external.ssafy.SsafyTransferApiClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Optional;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;

/**
 * 은행 장애 시나리오 테스트. 이체부터 정산까지 한 회차가 겪는 흐름을 처음부터 끝까지 따라간다.
 *
 * <p>은행만 가짜다. 이체 API와 거래내역 조회 API를 가짜 은행 서버(WireMock) 하나가 흉내 내고,
 * 우리 쪽은 {@link AutoTransferService}·{@link TransferReconciliationService}와 두 HTTP
 * 클라이언트를 <b>실제 운영 코드 그대로</b> 쓴다. DB는 저장소 목이 조건부 UPDATE를 흉내 낸다.</p>
 *
 * <p>확인하려는 것은 하나다. <b>은행이 무슨 장애를 내도 같은 회차에 출금이 두 번 나가지 않고,
 * 확신할 수 없는 경우에는 사람에게 넘긴다.</b></p>
 */
@ExtendWith(MockitoExtension.class)
class BankOutageScenarioTest {

    private static final Integer SCHEDULE_SEQ = 1;
    private static final Integer CONTRACT_SEQ = 100;
    private static final String TRANSFER_PATH = "/transfer";
    private static final String HISTORY_PATH = "/history";
    private static final String DEBTOR_ACCOUNT_NO = "0012345678901234";
    private static final String AMOUNT = "500000";

    @RegisterExtension
    static WireMockExtension bank = WireMockExtension.newInstance()
            // JDK HTTP 클라이언트의 HTTP/2 업그레이드 시도를 가짜 서버가 끊지 않도록 HTTP/1.1로만 받는다
            .options(wireMockConfig().dynamicPort().http2PlainDisabled(true))
            .build();

    @Mock
    private RepaymentScheduleRepository repaymentScheduleRepository;
    @Mock
    private AutoTransferRegistrationRepository registrationRepository;
    @Mock
    private RepaymentEventRepository repaymentEventRepository;
    @Mock
    private NotificationCommandService notificationCommandService;

    private RecordingHistoryRecorder historyRecorder;
    private AutoTransferService autoTransferService;
    private TransferReconciliationService reconciliationService;
    private RepaymentSchedule schedule;

    @BeforeEach
    void setUp() {
        SsafyTransferApiClient transferClient = new SsafyTransferApiClient();
        ReflectionTestUtils.setField(transferClient, "baseUrl", bank.baseUrl());
        ReflectionTestUtils.setField(transferClient, "transferPath", TRANSFER_PATH);
        ReflectionTestUtils.setField(transferClient, "apiKey", "test-key");

        SsafyApiProperties properties = new SsafyApiProperties();
        properties.setBaseUrl(bank.baseUrl());
        properties.setApiKey("test-key");
        properties.setInstitutionCode("00100");
        properties.setFintechAppNo("001");
        properties.setInquireTransactionHistoryListPath(HISTORY_PATH);
        SsafyDemandDepositClient historyClient = new SsafyDemandDepositClient(
                RestClient.builder(), properties, new SsafyApiHeaderFactory(properties));

        TransactionTemplate transactionTemplate = new TransactionTemplate(new RecordingTransactionManager());
        historyRecorder = new RecordingHistoryRecorder();
        autoTransferService = new AutoTransferService(repaymentScheduleRepository, registrationRepository,
                repaymentEventRepository, notificationCommandService, transferClient, transactionTemplate,
                historyRecorder);
        reconciliationService = new TransferReconciliationService(repaymentScheduleRepository,
                registrationRepository, repaymentEventRepository, notificationCommandService, historyClient,
                transactionTemplate, historyRecorder);

        givenScheduleDueToday();
        givenDatabaseBehavesLikeConditionalUpdates();
    }

    private void givenScheduleDueToday() {
        User debtor = User.builder().userSeq(1).ssafyUserKey("debtor-key").state("ACTIVE").build();
        User creditor = User.builder().userSeq(2).ssafyUserKey("creditor-key").state("ACTIVE").build();
        Contract contract = Contract.builder()
                .contractSeq(CONTRACT_SEQ).contractId("C-20260325-TEST01")
                .creditor(creditor).debtor(debtor)
                .principalAmount(new BigDecimal("1000000")).status("ACTIVE")
                .startDt(LocalDate.of(2026, 1, 1)).endDt(LocalDate.of(2027, 1, 1))
                .build();
        schedule = RepaymentSchedule.builder()
                .scheduleSeq(SCHEDULE_SEQ).contract(contract).installmentNo(1)
                .dueDt(LocalDate.now()).totalDue(new BigDecimal(AMOUNT)).status("NONPAID")
                .build();
        contract.addRepaymentSchedule(schedule);
        // 남은 회차가 있어 계약은 종료되지 않는다
        contract.addRepaymentSchedule(RepaymentSchedule.builder()
                .scheduleSeq(2).contract(contract).installmentNo(2)
                .totalDue(new BigDecimal(AMOUNT)).status("NONPAID").build());

        lenient().when(repaymentScheduleRepository.findByScheduleSeqWithContractUsers(SCHEDULE_SEQ))
                .thenReturn(Optional.of(schedule));
        lenient().when(registrationRepository.findActiveWithAccounts(CONTRACT_SEQ)).thenReturn(Optional.of(
                AutoTransferRegistration.builder().registrationSeq(1).contract(contract)
                        .debtorAccount(BankAccount.builder().accountSeq(10).accountNo(DEBTOR_ACCOUNT_NO).build())
                        .creditorAccount(BankAccount.builder().accountSeq(20).accountNo("0098765432109876").build())
                        .amount(new BigDecimal(AMOUNT)).transferDay(15).isActive(true).build()));
    }

    /** 선점·확정·복귀 UPDATE를 "기대한 상태일 때만 바꾸고 바꾼 건수를 돌려주는" DB처럼 흉내 낸다. */
    private void givenDatabaseBehavesLikeConditionalUpdates() {
        lenient().when(repaymentScheduleRepository.claimForTransfer(any(), anyString(), any()))
                .thenAnswer(inv -> {
                    if (!inv.getArgument(1).equals(schedule.getStatus())) {
                        return 0;
                    }
                    ReflectionTestUtils.setField(schedule, "status", "REQUESTED");
                    ReflectionTestUtils.setField(schedule, "transferRequestedDttm", inv.getArgument(2));
                    return 1;
                });
        lenient().when(repaymentScheduleRepository.markPaidIfRequested(any(), any()))
                .thenAnswer(inv -> {
                    if (!"REQUESTED".equals(schedule.getStatus())) {
                        return 0;
                    }
                    ReflectionTestUtils.setField(schedule, "status", "PAID");
                    ReflectionTestUtils.setField(schedule, "paidDttm", inv.getArgument(1));
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
    }

    // ──── 가짜 은행 ────

    private static String transferResponse(String code) {
        return "{\"Header\":{\"responseCode\":\"" + code + "\",\"responseMessage\":\"msg\"},\"REC\":[]}";
    }

    private void bankTransferSucceeds() {
        bank.stubFor(post(TRANSFER_PATH).willReturn(okJson(transferResponse("H0000"))));
    }

    /** 채무자 계좌 거래내역. 각 원소는 {적요, 금액}이다. */
    private void bankHistoryShows(String[]... withdrawals) {
        StringBuilder list = new StringBuilder();
        for (int i = 0; i < withdrawals.length; i++) {
            if (i > 0) {
                list.append(',');
            }
            list.append("{\"transactionUniqueNo\":\"").append(i + 1)
                    .append("\",\"transactionType\":\"2\",\"transactionTypeName\":\"출금(이체)\"")
                    .append(",\"transactionSummary\":\"").append(withdrawals[i][0])
                    .append("\",\"transactionBalance\":\"").append(withdrawals[i][1]).append("\"}");
        }
        bank.stubFor(post(HISTORY_PATH).willReturn(okJson(
                "{\"Header\":{\"responseCode\":\"H0000\",\"responseMessage\":\"정상처리 되었습니다.\"},"
                        + "\"REC\":{\"totalCount\":\"" + withdrawals.length + "\",\"list\":[" + list + "]}}")));
    }

    private int transferRequestsReceived() {
        return bank.findAll(postRequestedFor(urlEqualTo(TRANSFER_PATH))).size();
    }

    // ──────────────────────────────────────────────

    @Test
    @DisplayName("응답이 늦어 타임아웃 → 처리 중으로 남아 재이체가 막히고 → 거래내역에 출금이 있어 납부 완료")
    void timeout_thenWithdrawalFound_shouldBePaidWithSingleWithdrawal() {
        // 은행은 이체를 처리했지만 응답이 읽기 타임아웃보다 늦게 온다
        bank.stubFor(post(TRANSFER_PATH).willReturn(okJson(transferResponse("H0000")).withFixedDelay(11_000)));

        assertThat(autoTransferService.execute(SCHEDULE_SEQ)).isEqualTo(AutoTransferService.Result.UNRESOLVED);
        assertThat(schedule.getStatus()).isEqualTo("REQUESTED");

        // 다음 이체 시간이 와도 결과를 모르는 회차에는 이체를 다시 보내지 않는다
        bankTransferSucceeds();
        assertThat(autoTransferService.execute(SCHEDULE_SEQ)).isEqualTo(AutoTransferService.Result.BLOCKED);

        bankHistoryShows(new String[]{"BL1", "-" + AMOUNT});
        assertThat(reconciliationService.reconcile(SCHEDULE_SEQ))
                .isEqualTo(TransferReconciliationService.Result.CONFIRMED_PAID);

        assertThat(schedule.getStatus()).isEqualTo("PAID");
        assertThat(transferRequestsReceived()).isEqualTo(1);
        then(repaymentEventRepository).should().save(any(RepaymentEvent.class));
        assertThat(historyRecorder.reasons()).containsExactly(
                RepaymentScheduleChangeReason.AUTO_TRANSFER_REQUESTED,
                RepaymentScheduleChangeReason.TRANSFER_OUTCOME_UNKNOWN,
                RepaymentScheduleChangeReason.RECONCILED_PAID);
        // 정산은 이체를 보낸 출금 계좌의 거래내역을 본다
        bank.verify(postRequestedFor(urlEqualTo(HISTORY_PATH))
                .withRequestBody(matchingJsonPath("$.accountNo", equalTo(DEBTOR_ACCOUNT_NO))));
    }

    @Test
    @DisplayName("연결이 끊겨 결과 불명 → 거래내역에 출금이 없어 미납으로 복구 → 다음 이체 시간에 다시 이체해 납부 완료")
    void connectionReset_thenNoWithdrawal_shouldRestoreAndRetryOnce() {
        bank.stubFor(post(TRANSFER_PATH).willReturn(aResponse().withFault(Fault.CONNECTION_RESET_BY_PEER)));

        assertThat(autoTransferService.execute(SCHEDULE_SEQ)).isEqualTo(AutoTransferService.Result.UNRESOLVED);

        bankHistoryShows();
        assertThat(reconciliationService.reconcile(SCHEDULE_SEQ))
                .isEqualTo(TransferReconciliationService.Result.RELEASED);
        assertThat(schedule.getStatus()).isEqualTo("NONPAID");

        bankTransferSucceeds();
        assertThat(autoTransferService.execute(SCHEDULE_SEQ)).isEqualTo(AutoTransferService.Result.SUCCEEDED);

        assertThat(schedule.getStatus()).isEqualTo("PAID");
        // 첫 요청은 은행이 처리하지 않았다는 것을 확인한 뒤에야 두 번째 요청을 보냈다
        assertThat(transferRequestsReceived()).isEqualTo(2);
        assertThat(historyRecorder.reasons()).containsExactly(
                RepaymentScheduleChangeReason.AUTO_TRANSFER_REQUESTED,
                RepaymentScheduleChangeReason.TRANSFER_OUTCOME_UNKNOWN,
                RepaymentScheduleChangeReason.RECONCILED_NOT_WITHDRAWN,
                RepaymentScheduleChangeReason.AUTO_TRANSFER_REQUESTED,
                RepaymentScheduleChangeReason.TRANSFER_SUCCEEDED);
    }

    @Test
    @DisplayName("거래내역에 이 회차의 출금이 2건 → 이미 이중 출금일 수 있어 자동으로 판단하지 않고 사람에게 넘긴다")
    void twoMatchingWithdrawals_shouldNeedReview() {
        bank.stubFor(post(TRANSFER_PATH).willReturn(aResponse().withStatus(503)));
        assertThat(autoTransferService.execute(SCHEDULE_SEQ)).isEqualTo(AutoTransferService.Result.UNRESOLVED);

        bankHistoryShows(new String[]{"BL1", AMOUNT}, new String[]{"BL1", AMOUNT});

        assertThat(reconciliationService.reconcile(SCHEDULE_SEQ))
                .isEqualTo(TransferReconciliationService.Result.NEEDS_REVIEW);
        assertThat(schedule.getStatus()).isEqualTo("REQUESTED");
        then(repaymentEventRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("거래내역 조회마저 실패 → 출금 여부를 모르므로 처리 중으로 둔 채 사람에게 넘긴다")
    void historyLookupFails_shouldNeedReviewAndKeepBlocking() {
        bank.stubFor(post(TRANSFER_PATH).willReturn(aResponse().withFault(Fault.EMPTY_RESPONSE)));
        assertThat(autoTransferService.execute(SCHEDULE_SEQ)).isEqualTo(AutoTransferService.Result.UNRESOLVED);

        bank.stubFor(post(HISTORY_PATH).willReturn(aResponse().withStatus(500)));

        assertThat(reconciliationService.reconcile(SCHEDULE_SEQ))
                .isEqualTo(TransferReconciliationService.Result.NEEDS_REVIEW);
        assertThat(schedule.getStatus()).isEqualTo("REQUESTED");

        bankTransferSucceeds();
        assertThat(autoTransferService.execute(SCHEDULE_SEQ)).isEqualTo(AutoTransferService.Result.BLOCKED);
        assertThat(transferRequestsReceived()).isEqualTo(1);
    }

    @Test
    @DisplayName("거래내역 조회가 응답하지 않으면 무한정 기다리지 않고 포기 → 처리 중으로 둔 채 사람에게 넘긴다")
    void historyLookupHangs_shouldGiveUpAndKeepBlocking() {
        bank.stubFor(post(TRANSFER_PATH).willReturn(aResponse().withFault(Fault.CONNECTION_RESET_BY_PEER)));
        assertThat(autoTransferService.execute(SCHEDULE_SEQ)).isEqualTo(AutoTransferService.Result.UNRESOLVED);

        // 은행은 출금을 처리했지만 조회 응답이 조회 타임아웃(5초)보다 늦게 온다
        bank.stubFor(post(HISTORY_PATH).willReturn(okJson(
                "{\"Header\":{\"responseCode\":\"H0000\",\"responseMessage\":\"ok\"},"
                        + "\"REC\":{\"totalCount\":\"1\",\"list\":[{\"transactionSummary\":\"BL1\","
                        + "\"transactionBalance\":\"" + AMOUNT + "\"}]}}").withFixedDelay(8_000)));

        // 조회가 매달리면 같은 스레드에서 뒤이어 돌 자동이체가 전부 밀린다. 타임아웃이 없으면 8초 뒤 응답을 받아 깨진다.
        TransferReconciliationService.Result result = assertTimeoutPreemptively(Duration.ofSeconds(7),
                () -> reconciliationService.reconcile(SCHEDULE_SEQ));

        // 늦은 응답을 못 봤을 뿐이다. 출금이 없다고 단정해 되돌리지 않는다.
        assertThat(result).isEqualTo(TransferReconciliationService.Result.NEEDS_REVIEW);
        assertThat(schedule.getStatus()).isEqualTo("REQUESTED");
    }

    @Test
    @DisplayName("거래내역에 다른 회차(BL12)나 금액이 다른 출금만 있으면 이 회차의 것으로 오인하지 않고 미납으로 복구")
    void onlyOtherWithdrawals_shouldNotBeMistakenForThisSchedule() {
        bank.stubFor(post(TRANSFER_PATH).willReturn(aResponse().withFault(Fault.CONNECTION_RESET_BY_PEER)));
        assertThat(autoTransferService.execute(SCHEDULE_SEQ)).isEqualTo(AutoTransferService.Result.UNRESOLVED);

        bankHistoryShows(new String[]{"BL12", AMOUNT}, new String[]{"BL1", "499999"});

        assertThat(reconciliationService.reconcile(SCHEDULE_SEQ))
                .isEqualTo(TransferReconciliationService.Result.RELEASED);
        assertThat(schedule.getStatus()).isEqualTo("NONPAID");
    }

    @Test
    @DisplayName("잔액 부족으로 거절 → 정산을 기다리지 않고 바로 미납으로 되돌려 다음 이체 시간에 다시 시도")
    void rejected_shouldRestoreImmediately() {
        bank.stubFor(post(TRANSFER_PATH).willReturn(okJson(transferResponse("A1014"))));

        assertThatThrownBy(() -> autoTransferService.execute(SCHEDULE_SEQ))
                .isInstanceOf(TransferFailException.class);

        assertThat(schedule.getStatus()).isEqualTo("NONPAID");
        assertThat(historyRecorder.reasons()).containsExactly(
                RepaymentScheduleChangeReason.AUTO_TRANSFER_REQUESTED,
                RepaymentScheduleChangeReason.TRANSFER_REJECTED);
        bank.verify(0, postRequestedFor(urlEqualTo(HISTORY_PATH)));
    }
}
