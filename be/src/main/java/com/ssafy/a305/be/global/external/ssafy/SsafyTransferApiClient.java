package com.ssafy.a305.be.global.external.ssafy;

import com.ssafy.a305.be.global.external.ssafy.dto.SsafyApiHeader;
import com.ssafy.a305.be.global.external.ssafy.dto.SsafyTransferRequest;
import com.ssafy.a305.be.global.external.ssafy.dto.SsafyTransferResponse;
import com.ssafy.a305.be.global.exception.TransferFailException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
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

    /** 연결 수립까지 기다리는 시간. 연결 자체는 빠르므로 짧게 잡는다. */
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(3);

    /**
     * 응답을 기다리는 시간.
     *
     * <p><b>이체 요청 락의 TTL(30초)보다 반드시 짧아야 합니다.</b> 호출이 락보다 오래 매달리면
     * 락이 먼저 풀려 같은 회차에 다른 요청이 진입할 수 있습니다. 기본값인 무제한 대기로는
     * 그 조건을 만족할 수 없습니다.</p>
     *
     * @see com.ssafy.a305.be.domain.repayment.service.RepaymentTransferRequestGuard
     */
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(10);

    public SsafyTransferApiClient() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout((int) CONNECT_TIMEOUT.toMillis());
        factory.setReadTimeout((int) READ_TIMEOUT.toMillis());
        this.restTemplate = new RestTemplate(factory);
    }

    /**
     * 계좌 이체를 요청합니다.
     *
     * @param withdrawalAccountNo 출금계좌번호 (채무자)
     * @param depositAccountNo    입금계좌번호 (채권자)
     * @param amount              이체 금액
     * @param memo                거래 요약 메모
     * @param userKey             SSAFY 사용자 키
     * @return 이체 응답
     * @throws TransferFailException 이체 실패 시
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

            if (body == null || !body.isSuccess()) {
                String responseCode = (body != null && body.getHeader() != null)
                        ? body.getHeader().getResponseCode()
                        : "Q1000";
                String errorMsg = (body != null && body.getHeader() != null)
                        ? body.getHeader().getResponseMessage()
                        : "응답 없음";
                log.error("[SSAFY 이체] 실패 - code={}, 사유={}", responseCode, errorMsg);
                throw new TransferFailException("SSAFY_TRANSFER_FAILED|" + responseCode + "|" + errorMsg);
            }

            log.info("[SSAFY 이체] 성공 - 출금:{} → 입금:{}", withdrawalAccountNo, depositAccountNo);
            return body;

        } catch (RestClientException e) {
            log.error("[SSAFY 이체] 네트워크 오류 - {}", e.getMessage(), e);
            throw new TransferFailException("SSAFY 이체 API 통신 오류: " + e.getMessage());
        }
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
