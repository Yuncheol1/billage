package com.ssafy.a305.be.domain.bank.client;

import com.ssafy.a305.be.domain.auth.config.SsafyApiProperties;
import com.ssafy.a305.be.domain.bank.client.dto.SsafyCreateDemandDepositAccountRequestDto;
import com.ssafy.a305.be.domain.bank.client.dto.SsafyCreateDemandDepositAccountResponseDto;
import com.ssafy.a305.be.global.exception.CustomBusinessException;
import com.ssafy.a305.be.global.exception.dto.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

@Component
@RequiredArgsConstructor
public class SsafyDemandDepositClient {

    private static final String API_NAME = "createDemandDepositAccount";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HHmmss");
    private static final DateTimeFormatter TX_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final RestClient.Builder restClientBuilder;
    private final SsafyApiProperties ssafyApiProperties;

    public SsafyCreateDemandDepositAccountResponseDto.Rec createDemandDepositAccount(String userKey, String accountTypeUniqueNo) {
        validateConfiguration();

        LocalDateTime now = LocalDateTime.now();
        SsafyCreateDemandDepositAccountRequestDto request = new SsafyCreateDemandDepositAccountRequestDto(
                new SsafyCreateDemandDepositAccountRequestDto.Header(
                        API_NAME,
                        DATE_FORMATTER.format(now),
                        TIME_FORMATTER.format(now),
                        ssafyApiProperties.getInstitutionCode(),
                        ssafyApiProperties.getFintechAppNo(),
                        API_NAME,
                        generateInstitutionTransactionUniqueNo(now),
                        ssafyApiProperties.getApiKey(),
                        userKey
                ),
                accountTypeUniqueNo
        );

        try {
            SsafyCreateDemandDepositAccountResponseDto response = restClientBuilder.build()
                    .post()
                    .uri(buildUrl(ssafyApiProperties.getCreateDemandDepositAccountPath()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(SsafyCreateDemandDepositAccountResponseDto.class);

            if (response == null || response.header() == null || response.rec() == null) {
                throw new CustomBusinessException(ErrorCode.INVALID_INPUT_VALUE, "SSAFY 계좌 생성 응답이 비어 있습니다.");
            }

            if (!"H0000".equals(response.header().responseCode())) {
                throw new CustomBusinessException(
                        ErrorCode.INVALID_INPUT_VALUE,
                        "SSAFY 계좌 생성 실패(code=%s, message=%s)"
                                .formatted(response.header().responseCode(), response.header().responseMessage())
                );
            }

            return response.rec();
        } catch (RestClientResponseException e) {
            throw new CustomBusinessException(
                    ErrorCode.INVALID_INPUT_VALUE,
                    "SSAFY 계좌 생성 요청 실패(status=%s, response=%s)"
                            .formatted(e.getStatusCode(), e.getResponseBodyAsString())
            );
        } catch (RestClientException e) {
            throw new CustomBusinessException(
                    ErrorCode.INVALID_INPUT_VALUE,
                    "SSAFY 계좌 생성 요청 실패(cause=%s)".formatted(e.getMessage())
            );
        }
    }

    private void validateConfiguration() {
        if (!hasText(ssafyApiProperties.getApiKey())) {
            throw new CustomBusinessException(ErrorCode.AUTH_CONFIG_INVALID, "SSAFY_API_KEY is required.");
        }
        if (!hasText(ssafyApiProperties.getInstitutionCode())) {
            throw new CustomBusinessException(ErrorCode.AUTH_CONFIG_INVALID, "SSAFY_INSTITUTION_CODE is required.");
        }
        if (!hasText(ssafyApiProperties.getFintechAppNo())) {
            throw new CustomBusinessException(ErrorCode.AUTH_CONFIG_INVALID, "SSAFY_FINTECH_APP_NO is required.");
        }
        if (!hasText(ssafyApiProperties.getCreateDemandDepositAccountPath())) {
            throw new CustomBusinessException(ErrorCode.AUTH_CONFIG_INVALID, "SSAFY_CREATE_DEMAND_DEPOSIT_ACCOUNT_PATH is required.");
        }
    }

    private String buildUrl(String path) {
        String baseUrl = Optional.ofNullable(ssafyApiProperties.getBaseUrl()).orElse("").trim();
        String targetPath = Optional.ofNullable(path).orElse("").trim();
        if (baseUrl.isBlank() || targetPath.isBlank()) {
            throw new CustomBusinessException(ErrorCode.AUTH_CONFIG_INVALID, "SSAFY API 설정이 올바르지 않습니다.");
        }
        return baseUrl + targetPath;
    }

    private String generateInstitutionTransactionUniqueNo(LocalDateTime now) {
        int random = ThreadLocalRandom.current().nextInt(100000, 1_000_000);
        return TX_FORMATTER.format(now) + random;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
