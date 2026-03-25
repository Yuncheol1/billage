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
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
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

    private static final String TRANSFER_URL =
            "https://finopenapi.ssafy.io/ssafy/api/v1/edu/demandDeposit/updateDemandDepositAccountTransfer";

    private static final String API_NAME = "updateDemandDepositAccountTransfer";

    private final RestTemplate restTemplate;

    @Value("${ssafy.api.api-key:}")
    private String apiKey;

    public SsafyTransferApiClient() {
        this.restTemplate = new RestTemplate();
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
                    TRANSFER_URL,
                    HttpMethod.POST,
                    entity,
                    SsafyTransferResponse.class
            );

            SsafyTransferResponse body = response.getBody();

            if (body == null || !body.isSuccess()) {
                String errorMsg = (body != null && body.getHeader() != null)
                        ? body.getHeader().getResponseMessage()
                        : "응답 없음";
                log.error("[SSAFY 이체] 실패 - 사유: {}", errorMsg);
                throw new TransferFailException("SSAFY 이체 API 실패: " + errorMsg);
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
}
