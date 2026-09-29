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

    /**
     * 은행이 요청을 <b>처리하지 않고 거절했다</b>는 것이 확실한 응답코드.
     *
     * <p>의미가 문서화된 코드만 넣는다. 여기에 없는 코드는 "처리 중"이나 "이미 처리됨"일 수도
     * 있으므로 실패로 단정하지 않고 결과 불명으로 다룬다. 모르는 것을 실패로 보고 재시도하면
     * 이미 나간 이체가 한 번 더 나간다.</p>
     */
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
     * 계좌 이체를 요청합니다.
     *
     * @param withdrawalAccountNo 출금계좌번호 (채무자)
     * @param depositAccountNo    입금계좌번호 (채권자)
     * @param amount              이체 금액
     * @param memo                거래 요약 메모
     * @param userKey             SSAFY 사용자 키
     * @return 이체 응답
     * @throws TransferOutcomeUnknownException 결과를 알 수 없을 때(타임아웃·본문 없음·미분류 응답코드).
     *         돈이 나갔을 수 있으므로 호출자는 실패로 단정하면 안 된다.
     * @throws TransferFailException 은행이 거절했거나 요청이 도달하지 못했음이 확실할 때
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
                // 응답은 받았지만 결과를 읽을 수 없다. 처리됐는지 알 수 없다.
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

            // 분류하지 못한 코드. 명세를 확인해 위 집합에 추가하기 전까지는 결과 불명으로 둔다.
            log.error("[SSAFY 이체] 미분류 응답코드 - code={}, 사유={}. 결과 불명으로 처리", responseCode, errorMsg);
            throw new TransferOutcomeUnknownException("SSAFY_TRANSFER_UNKNOWN|" + responseCode + "|" + errorMsg);

        } catch (TransferFailException e) {
            throw e;
        } catch (HttpClientErrorException e) {
            // 4xx — 요청이 받아들여지지 않았다. 처리되지 않은 것이 확실하다.
            log.error("[SSAFY 이체] 요청 거부 - status={}", e.getStatusCode());
            throw new TransferFailException("SSAFY 이체 API 요청 거부: " + e.getStatusCode(), e);
        } catch (ResourceAccessException e) {
            if (isRequestNeverReachedBank(e)) {
                // 연결 자체가 안 됐다. 요청이 도달하지 못했으므로 돈이 나갔을 수 없다.
                log.error("[SSAFY 이체] 연결 실패 - {}", e.getMessage());
                throw new TransferFailException("SSAFY 이체 API 연결 실패: " + e.getMessage(), e);
            }
            // 읽기 타임아웃 등. 은행이 요청을 받은 뒤일 수 있다.
            log.error("[SSAFY 이체] 응답 대기 중 오류 - 결과 불명. {}", e.getMessage());
            throw new TransferOutcomeUnknownException("SSAFY_TRANSFER_UNKNOWN|IO|" + e.getMessage(), e);
        } catch (RestClientException e) {
            // 5xx, 응답 해석 실패 등. 요청이 은행에 도달했을 수 있다.
            log.error("[SSAFY 이체] 통신 오류 - 결과 불명. {}", e.getMessage());
            throw new TransferOutcomeUnknownException("SSAFY_TRANSFER_UNKNOWN|HTTP|" + e.getMessage(), e);
        }
    }

    /**
     * 요청이 은행에 도달하지 못했음이 확실한 연결 실패인지.
     *
     * <p>연결 거부·호스트 미확인만 인정한다. 연결 타임아웃도 도달 전이지만 읽기 타임아웃과
     * 예외 타입이 같아 구분이 취약하므로, 확신할 수 없는 쪽(결과 불명)으로 둔다.</p>
     */
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
