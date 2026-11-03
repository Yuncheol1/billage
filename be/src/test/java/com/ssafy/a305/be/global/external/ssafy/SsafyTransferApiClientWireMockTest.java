package com.ssafy.a305.be.global.external.ssafy;

import com.github.tomakehurst.wiremock.http.Fault;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.ssafy.a305.be.global.exception.TransferFailException;
import com.ssafy.a305.be.global.exception.TransferOutcomeUnknownException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.net.ServerSocket;
import java.time.Duration;

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

/**
 * 가짜 은행 서버(WireMock)로 실제 HTTP 응답 분류를 확인하는 테스트
 */
class SsafyTransferApiClientWireMockTest {

    private static final String PATH = "/transfer";

    @RegisterExtension
    static WireMockExtension bank = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    private SsafyTransferApiClient client;

    @BeforeEach
    void setUp() {
        client = clientFor(bank.baseUrl());
    }

    private static SsafyTransferApiClient clientFor(String baseUrl) {
        SsafyTransferApiClient client = new SsafyTransferApiClient();
        ReflectionTestUtils.setField(client, "baseUrl", baseUrl);
        ReflectionTestUtils.setField(client, "transferPath", PATH);
        ReflectionTestUtils.setField(client, "apiKey", "test-key");
        return client;
    }

    private void call() {
        client.transfer("0012345678901234", "0098765432109876",
                new BigDecimal("500000"), "BL12", "user-key");
    }

    private static String responseWithCode(String code) {
        return "{\"Header\":{\"responseCode\":\"" + code + "\",\"responseMessage\":\"msg\"},\"REC\":[]}";
    }

    @Test
    @DisplayName("은행에 출금·입금 계좌, 소수점을 버린 금액, 회차 식별자가 담긴 적요를 보낸다")
    void request_shouldCarryAccountsAmountAndMemo() {
        bank.stubFor(post(PATH).willReturn(okJson(responseWithCode("H0000"))));

        client.transfer("0012345678901234", "0098765432109876",
                new BigDecimal("500000.70"), "BL12", "user-key");

        bank.verify(postRequestedFor(urlEqualTo(PATH))
                .withHeader("Content-Type", equalTo("application/json"))
                .withRequestBody(matchingJsonPath("$.Header.apiName", equalTo("updateDemandDepositAccountTransfer")))
                .withRequestBody(matchingJsonPath("$.Header.userKey", equalTo("user-key")))
                .withRequestBody(matchingJsonPath("$.withdrawalAccountNo", equalTo("0012345678901234")))
                .withRequestBody(matchingJsonPath("$.depositAccountNo", equalTo("0098765432109876")))
                .withRequestBody(matchingJsonPath("$.transactionBalance", equalTo("500000")))
                .withRequestBody(matchingJsonPath("$.withdrawalTransactionSummary", equalTo("BL12")))
                .withRequestBody(matchingJsonPath("$.depositTransactionSummary", equalTo("BL12"))));
    }

    @Test
    @DisplayName("은행이 읽기 타임아웃보다 늦게 답하면 무한정 기다리지 않고 → 결과 불명")
    void slowerThanReadTimeout_shouldBeUnknownWithoutHanging() {
        bank.stubFor(post(PATH).willReturn(okJson(responseWithCode("H0000")).withFixedDelay(11_000)));

        // 읽기 타임아웃(10초)이 실제로 걸리지 않으면 11초 뒤 성공 응답을 받아 이 단언이 깨진다.
        assertTimeoutPreemptively(Duration.ofSeconds(11), () ->
                assertThatThrownBy(this::call).isInstanceOf(TransferOutcomeUnknownException.class));
        // 응답이 늦었을 뿐 요청은 은행에 도달했다. 그래서 실패가 아니라 결과 불명이다.
        bank.verify(1, postRequestedFor(urlEqualTo(PATH)));
    }

    @Test
    @DisplayName("은행 서버가 꺼져 있어 연결이 거부되면 요청이 도달하지 않은 것이므로 → 확정 실패")
    void connectionRefused_shouldBeDefiniteFailure() throws Exception {
        int closedPort;
        try (ServerSocket socket = new ServerSocket(0)) {
            closedPort = socket.getLocalPort();
        }
        client = clientFor("http://localhost:" + closedPort);

        assertThatThrownBy(this::call)
                .isInstanceOf(TransferFailException.class)
                .isNotInstanceOf(TransferOutcomeUnknownException.class);
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(value = Fault.class, names = {
            "CONNECTION_RESET_BY_PEER", "EMPTY_RESPONSE", "MALFORMED_RESPONSE_CHUNK", "RANDOM_DATA_THEN_CLOSE"})
    @DisplayName("요청을 받은 뒤 연결이 끊기거나 응답이 깨지면 은행이 처리했을 수 있으므로 → 결과 불명")
    void brokenAfterRequest_shouldBeUnknown(Fault fault) {
        bank.stubFor(post(PATH).willReturn(aResponse().withFault(fault)));

        assertThatThrownBy(this::call).isInstanceOf(TransferOutcomeUnknownException.class);
        bank.verify(1, postRequestedFor(urlEqualTo(PATH)));
    }

    @Test
    @DisplayName("HTTP 503도 은행 내부 사정이라 처리 여부를 알 수 없으므로 → 결과 불명")
    void serviceUnavailable_shouldBeUnknown() {
        bank.stubFor(post(PATH).willReturn(aResponse().withStatus(503)));

        assertThatThrownBy(this::call).isInstanceOf(TransferOutcomeUnknownException.class);
    }

    @Test
    @DisplayName("잔액 부족(A1014)은 은행이 거절한 것이 확실하므로 → 확정 실패")
    void insufficientBalance_shouldBeDefiniteFailure() {
        bank.stubFor(post(PATH).willReturn(okJson(responseWithCode("A1014"))));

        assertThatThrownBy(this::call)
                .isInstanceOf(TransferFailException.class)
                .isNotInstanceOf(TransferOutcomeUnknownException.class);
    }

    @Test
    @DisplayName("한 번 결과 불명이 나도 클라이언트가 스스로 재전송하지 않는다")
    void unknown_shouldNotBeRetriedByClient() {
        bank.stubFor(post(PATH).willReturn(aResponse().withFault(Fault.CONNECTION_RESET_BY_PEER)));

        assertThatThrownBy(this::call).isInstanceOf(TransferOutcomeUnknownException.class);

        // 재전송은 곧 이중 출금일 수 있다. 은행이 받은 요청은 정확히 한 번이어야 한다.
        assertThat(bank.findAll(postRequestedFor(urlEqualTo(PATH)))).hasSize(1);
    }
}
