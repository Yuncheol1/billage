package com.ssafy.a305.be.global.external.ssafy;

import com.ssafy.a305.be.global.exception.TransferFailException;
import com.ssafy.a305.be.global.exception.TransferOutcomeUnknownException;
import com.ssafy.a305.be.global.external.ssafy.dto.SsafyTransferResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withBadRequest;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * 이체 응답 분류 테스트. 확실한 거절만 실패, 나머지는 결과 불명
 */
class SsafyTransferApiClientTest {

    private static final String URL = "http://bank.test/transfer";

    private SsafyTransferApiClient client;
    private MockRestServiceServer server;

    @BeforeEach
    void setUp() {
        client = new SsafyTransferApiClient();
        ReflectionTestUtils.setField(client, "baseUrl", "http://bank.test");
        ReflectionTestUtils.setField(client, "transferPath", "/transfer");
        ReflectionTestUtils.setField(client, "apiKey", "test-key");

        RestTemplate restTemplate = (RestTemplate) ReflectionTestUtils.getField(client, "restTemplate");
        server = MockRestServiceServer.bindTo(restTemplate).build();
    }

    private SsafyTransferResponse call() {
        return client.transfer("0012345678901234", "0098765432109876",
                new BigDecimal("500000"), "BL1-1 1회차", "user-key");
    }

    private static String responseWithCode(String code) {
        return "{\"Header\":{\"responseCode\":\"" + code + "\",\"responseMessage\":\"msg\"},\"REC\":[]}";
    }

    private void respondWithCode(String code) {
        server.expect(requestTo(URL)).andExpect(method(POST))
                .andRespond(withSuccess(responseWithCode(code), MediaType.APPLICATION_JSON));
    }

    // ──────────────────────────────────────────────

    @Nested
    @DisplayName("성공")
    class Success {

        @Test
        @DisplayName("H0000이면 응답을 그대로 돌려준다")
        void success_shouldReturnResponse() {
            respondWithCode("H0000");

            SsafyTransferResponse response = call();

            assertThat(response.isSuccess()).isTrue();
        }
    }

    // ──────────────────────────────────────────────

    @Nested
    @DisplayName("확정 실패 — 은행이 처리하지 않고 거절했음이 확실한 경우")
    class DefiniteFailure {

        @ParameterizedTest(name = "{0}")
        @ValueSource(strings = {"A1003", "A1011", "A1014", "A1016", "A1017", "A1018", "Q1001"})
        @DisplayName("문서화된 거절 코드는 → TransferFailException이며 결과 불명이 아니다")
        void documentedRejection_shouldBeDefiniteFailure(String code) {
            respondWithCode(code);

            assertThatThrownBy(() -> call())
                    .isInstanceOf(TransferFailException.class)
                    .isNotInstanceOf(TransferOutcomeUnknownException.class)
                    .satisfies(e -> assertThat(((TransferFailException) e).getResponseCode()).isEqualTo(code));
        }

        @Test
        @DisplayName("HTTP 4xx는 요청이 받아들여지지 않은 것이므로 → 확정 실패")
        void http4xx_shouldBeDefiniteFailure() {
            server.expect(requestTo(URL)).andRespond(withBadRequest());

            assertThatThrownBy(() -> call())
                    .isInstanceOf(TransferFailException.class)
                    .isNotInstanceOf(TransferOutcomeUnknownException.class);
        }

        @Test
        @DisplayName("연결 거부는 요청이 도달하지 못한 것이므로 → 확정 실패")
        void connectionRefused_shouldBeDefiniteFailure() {
            server.expect(requestTo(URL)).andRespond(request -> {
                throw new ConnectException("Connection refused");
            });

            assertThatThrownBy(() -> call())
                    .isInstanceOf(TransferFailException.class)
                    .isNotInstanceOf(TransferOutcomeUnknownException.class);
        }

        @Test
        @DisplayName("호스트를 찾을 수 없어도 요청이 도달하지 못한 것이므로 → 확정 실패")
        void unknownHost_shouldBeDefiniteFailure() {
            server.expect(requestTo(URL)).andRespond(request -> {
                throw new UnknownHostException("bank.test");
            });

            assertThatThrownBy(() -> call())
                    .isInstanceOf(TransferFailException.class)
                    .isNotInstanceOf(TransferOutcomeUnknownException.class);
        }

        @Test
        @DisplayName("URL 설정이 비어 있으면 요청을 보내지 않으므로 → 확정 실패")
        void invalidConfig_shouldBeDefiniteFailure() {
            ReflectionTestUtils.setField(client, "baseUrl", "");

            assertThatThrownBy(() -> call())
                    .isInstanceOf(TransferFailException.class)
                    .isNotInstanceOf(TransferOutcomeUnknownException.class)
                    .hasMessageContaining("CONFIG_INVALID");
        }
    }

    // ──────────────────────────────────────────────

    @Nested
    @DisplayName("결과 불명 — 돈이 나갔을 수 있는 경우")
    class UnknownOutcome {

        @ParameterizedTest(name = "{0}")
        @ValueSource(strings = {"Z9999", "H1000", "Q1000", "A1010", "A5004"})
        @DisplayName("분류하지 못한 응답코드는 실패로 단정하지 않고 → 결과 불명")
        void unclassifiedCode_shouldBeUnknown(String code) {
            respondWithCode(code);

            assertThatThrownBy(() -> call()).isInstanceOf(TransferOutcomeUnknownException.class);
        }

        @Test
        @DisplayName("읽기 타임아웃은 은행이 이미 처리했을 수 있으므로 → 결과 불명")
        void readTimeout_shouldBeUnknown() {
            server.expect(requestTo(URL)).andRespond(request -> {
                throw new SocketTimeoutException("Read timed out");
            });

            assertThatThrownBy(() -> call()).isInstanceOf(TransferOutcomeUnknownException.class);
        }

        @Test
        @DisplayName("응답 본문이 비어 있으면 → 결과 불명")
        void emptyBody_shouldBeUnknown() {
            server.expect(requestTo(URL)).andRespond(withSuccess("", MediaType.APPLICATION_JSON));

            assertThatThrownBy(() -> call()).isInstanceOf(TransferOutcomeUnknownException.class);
        }

        @Test
        @DisplayName("응답에 Header가 없으면 → 결과 불명")
        void missingHeader_shouldBeUnknown() {
            server.expect(requestTo(URL)).andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

            assertThatThrownBy(() -> call()).isInstanceOf(TransferOutcomeUnknownException.class);
        }

        @Test
        @DisplayName("HTTP 5xx는 은행 내부 오류라 처리 여부를 알 수 없으므로 → 결과 불명")
        void http5xx_shouldBeUnknown() {
            server.expect(requestTo(URL)).andRespond(withServerError());

            assertThatThrownBy(() -> call()).isInstanceOf(TransferOutcomeUnknownException.class);
        }

        @Test
        @DisplayName("결과 불명은 TransferFailException이기도 해서 기존 예외 처리 경로를 그대로 탄다")
        void unknown_shouldRemainTransferFailException() {
            respondWithCode("Z9999");

            assertThatThrownBy(() -> call()).isInstanceOf(TransferFailException.class);
        }
    }

    // ──────────────────────────────────────────────

    @Nested
    @DisplayName("응답코드 추출")
    class ResponseCode {

        @Test
        @DisplayName("SSAFY_TRANSFER_FAILED|{code}|{message} 형식에서 코드를 꺼낸다")
        void shouldExtractCode() {
            TransferFailException e = new TransferFailException("SSAFY_TRANSFER_FAILED|A1014|잔액 부족");

            assertThat(e.getResponseCode()).isEqualTo("A1014");
        }

        @Test
        @DisplayName("형식이 다르면 null이다")
        void shouldReturnNullForOtherFormats() {
            assertThat(new TransferFailException("연결 실패").getResponseCode()).isNull();
            assertThat(new TransferFailException("SSAFY_TRANSFER_FAILED|").getResponseCode()).isNull();
        }
    }
}
