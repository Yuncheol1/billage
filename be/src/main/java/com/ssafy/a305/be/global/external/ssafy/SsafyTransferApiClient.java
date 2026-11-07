package com.ssafy.a305.be.global.external.ssafy;

import com.ssafy.a305.be.global.external.ssafy.dto.SsafyApiHeader;
import com.ssafy.a305.be.global.external.ssafy.dto.SsafyTransferRequest;
import com.ssafy.a305.be.global.external.ssafy.dto.SsafyTransferResponse;
import com.ssafy.a305.be.global.exception.TransferFailException;
import com.ssafy.a305.be.global.exception.TransferOutcomeUnknownException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.net.ConnectException;
import java.net.UnknownHostException;
import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * SSAFY 금융 API 계좌이체 클라이언트
 */
@Slf4j
@Component
public class SsafyTransferApiClient {

    private static final String API_NAME = "updateDemandDepositAccountTransfer";

    private final RestTemplate restTemplate;

    @Value("${ssafy.api.api-key:}")
    private String apiKey;

    @Value("${ssafy.api.base-url:}")
    private String baseUrl;

    @Value("${ssafy.api.update-demand-deposit-account-transfer-path:}")
    private String transferPath;

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(3);
    // 정산 대기 시간(RECONCILE_AFTER)보다 짧아야 응답 대기 중인 이체를 정산이 되돌리지 않는다.
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(10);

    // 은행이 처리하지 않았다고 확실한 코드만 넣는다. 나머지는 결과 불명으로 본다.
    private static final Set<String> DEFINITE_REJECTION_CODES = Set.of(
            "A1003",  // 계좌번호가 유효하지 않음
            "A1011",  // 거래 금액이 유효하지 않음
            "A1014",  // 잔액 부족
            "A1016",  // 1회 이체 한도 초과
            "A1017",  // 1일 이체 한도 초과
            "A1018",  // 거래요약내용 길이 초과
            "Q1001"   // 요청 본문 형식 오류
    );

    public SsafyTransferApiClient() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout((int) CONNECT_TIMEOUT.toMillis());
        factory.setReadTimeout((int) READ_TIMEOUT.toMillis());
        this.restTemplate = new RestTemplate(factory);
    }

    /**
     * 계좌 이체 요청
     * TransferFailException: 은행이 거절했거나 요청이 닿지 않은 게 확실할 때
     * TransferOutcomeUnknownException: 돈이 나갔는지 알 수 없을 때
     */
    public SsafyTransferResponse transfer(String withdrawalAccountNo,
                                          String depositAccountNo,
                                          BigDecimal amount,
                                          String memo,
                                          String userKey) {
        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("Asia/Seoul"));

        SsafyApiHeader header = SsafyApiHeader.builder()
                .apiName(API_NAME)
                .transmissionDate(now.format(DateTimeFormatter.ofPattern("yyyyMMdd")))
                .transmissionTime(now.format(DateTimeFormatter.ofPattern("HHmmss")))
                .institutionCode("00100")
                .fintechAppNo("001")
                .apiServiceCode(API_NAME)
                .institutionTransactionUniqueNo(generateUniqueNo(now))
                .apiKey(apiKey)
                .userKey(userKey)
                .build();

        SsafyTransferRequest request = SsafyTransferRequest.builder()
                .header(header)
                .withdrawalAccountNo(withdrawalAccountNo)
                .depositAccountNo(depositAccountNo)
                .transactionBalance(amount.setScale(0, java.math.RoundingMode.DOWN).toPlainString())
                .depositTransactionSummary(memo != null ? memo : "자동이체 입금")
                .withdrawalTransactionSummary(memo != null ? memo : "자동이체 출금")
                .build();

        try {
            HttpHeaders httpHeaders = new HttpHeaders();
            httpHeaders.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<SsafyTransferRequest> entity = new HttpEntity<>(request, httpHeaders);

            log.info("[SSAFY 이체] 요청 - 출금:{} → 입금:{}, 금액:{}",
                    withdrawalAccountNo, depositAccountNo, amount);

            ResponseEntity<SsafyTransferResponse> response = restTemplate.exchange(
                    buildTransferUrl(),
                    HttpMethod.POST,
                    entity,
                    SsafyTransferResponse.class
            );

            SsafyTransferResponse body = response.getBody();

            if (body == null || body.getHeader() == null) {
                log.error("[SSAFY 이체] 응답 본문 없음 - 결과 불명");
                throw new TransferOutcomeUnknownException("SSAFY_TRANSFER_UNKNOWN|NO_BODY|응답 본문 없음");
            }

            if (body.isSuccess()) {
                log.info("[SSAFY 이체] 성공 - 출금:{} → 입금:{}", withdrawalAccountNo, depositAccountNo);
                return body;
            }

            String responseCode = body.getHeader().getResponseCode();
            String errorMsg = body.getHeader().getResponseMessage();

            if (responseCode != null && DEFINITE_REJECTION_CODES.contains(responseCode)) {
                log.warn("[SSAFY 이체] 거절 - code={}, 사유={}", responseCode, errorMsg);
                throw new TransferFailException("SSAFY_TRANSFER_FAILED|" + responseCode + "|" + errorMsg);
            }

            // 모르는 코드는 결과 불명
            log.error("[SSAFY 이체] 미분류 응답코드 - code={}, 사유={}. 결과 불명으로 처리", responseCode, errorMsg);
            throw new TransferOutcomeUnknownException("SSAFY_TRANSFER_UNKNOWN|" + responseCode + "|" + errorMsg);

        } catch (TransferFailException e) {
            throw e;
        } catch (HttpClientErrorException e) {
            // 4xx: 요청이 받아들여지지 않음
            log.error("[SSAFY 이체] 요청 거부 - status={}", e.getStatusCode());
            throw new TransferFailException("SSAFY 이체 API 요청 거부: " + e.getStatusCode(), e);
        } catch (ResourceAccessException e) {
            if (isRequestNeverReachedBank(e)) {
                // 요청이 은행에 닿지 않음
                log.error("[SSAFY 이체] 연결 실패 - {}", e.getMessage());
                throw new TransferFailException("SSAFY 이체 API 연결 실패: " + e.getMessage(), e);
            }
            // 응답 타임아웃 등: 은행이 받은 뒤일 수 있다
            log.error("[SSAFY 이체] 응답 대기 중 오류 - 결과 불명. {}", e.getMessage());
            throw new TransferOutcomeUnknownException("SSAFY_TRANSFER_UNKNOWN|IO|" + e.getMessage(), e);
        } catch (RestClientException e) {
            // 5xx, 응답 해석 실패
            log.error("[SSAFY 이체] 통신 오류 - 결과 불명. {}", e.getMessage());
            throw new TransferOutcomeUnknownException("SSAFY_TRANSFER_UNKNOWN|HTTP|" + e.getMessage(), e);
        } catch (RuntimeException e) {
            // HTTP가 아닌 응답을 받으면 RestClientException이 아닌 예외가 난다. 요청은 보낸 뒤다.
            log.error("[SSAFY 이체] 예상하지 못한 오류 - 결과 불명. {}", e.toString());
            throw new TransferOutcomeUnknownException("SSAFY_TRANSFER_UNKNOWN|UNEXPECTED|" + e, e);
        }
    }

    // 연결 거부, 호스트 없음만 인정한다. 연결 타임아웃은 응답 타임아웃과 구분이 어려워 제외.
    private boolean isRequestNeverReachedBank(ResourceAccessException e) {
        Throwable cause = e.getCause();
        return cause instanceof ConnectException || cause instanceof UnknownHostException;
    }

    /**
     * 기관거래고유번호 생성 (YYYYMMDD + HHMMSS + 난수 6자리 = 20자리)
     */
    private String generateUniqueNo(ZonedDateTime now) {
        String datePart = now.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String timePart = now.format(DateTimeFormatter.ofPattern("HHmmss"));
        int random = ThreadLocalRandom.current().nextInt(100_000, 1_000_000);
        return datePart + timePart + random;
    }

    private String buildTransferUrl() {
        if (baseUrl == null || baseUrl.isBlank() || transferPath == null || transferPath.isBlank()) {
            throw new TransferFailException("SSAFY_TRANSFER_CONFIG_INVALID");
        }
        return baseUrl.replaceAll("/+$", "") + transferPath;
    }
}
