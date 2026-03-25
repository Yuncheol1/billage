package com.ssafy.a305.be.domain.bank.client;

import com.ssafy.a305.be.domain.auth.config.SsafyApiProperties;
import com.ssafy.a305.be.domain.bank.client.dto.request.SsafyCreateDemandDepositAccountRequestDto;
import com.ssafy.a305.be.domain.bank.client.dto.request.SsafyInquireDemandDepositAccountListRequestDto;
import com.ssafy.a305.be.domain.bank.client.dto.request.SsafyInquireDemandDepositAccountRequestDto;
import com.ssafy.a305.be.domain.bank.client.dto.response.SsafyCreateDemandDepositAccountResponseDto;
import com.ssafy.a305.be.domain.bank.client.dto.response.SsafyDemandDepositAccountRecDto;
import com.ssafy.a305.be.domain.bank.client.dto.response.SsafyInquireDemandDepositAccountListResponseDto;
import com.ssafy.a305.be.domain.bank.client.dto.response.SsafyInquireDemandDepositAccountResponseDto;
import com.ssafy.a305.be.global.exception.CustomBusinessException;
import com.ssafy.a305.be.global.exception.dto.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class SsafyDemandDepositClient {

    private static final String CREATE_ACCOUNT_API_NAME = "createDemandDepositAccount";
    private static final String INQUIRE_ACCOUNT_LIST_API_NAME = "inquireDemandDepositAccountList";
    private static final String INQUIRE_ACCOUNT_API_NAME = "inquireDemandDepositAccount";

    private final RestClient.Builder restClientBuilder;
    private final SsafyApiProperties ssafyApiProperties;
    private final SsafyApiHeaderFactory ssafyApiHeaderFactory;

    public SsafyCreateDemandDepositAccountResponseDto.Rec createDemandDepositAccount(String userKey, String accountTypeUniqueNo) {
        SsafyCreateDemandDepositAccountRequestDto request = new SsafyCreateDemandDepositAccountRequestDto(
                ssafyApiHeaderFactory.create(CREATE_ACCOUNT_API_NAME, userKey),
                accountTypeUniqueNo
        );
        SsafyCreateDemandDepositAccountResponseDto response = post(
                request,
                ssafyApiProperties.getCreateDemandDepositAccountPath(),
                SsafyCreateDemandDepositAccountResponseDto.class,
                "계좌 생성"
        );
        if (response.rec() == null) {
            throw new CustomBusinessException(ErrorCode.INVALID_INPUT_VALUE, "SSAFY 계좌 생성 응답이 비어 있습니다.");
        }
        return response.rec();
    }

    public List<SsafyDemandDepositAccountRecDto> inquireDemandDepositAccountList(String userKey) {
        SsafyInquireDemandDepositAccountListRequestDto request = new SsafyInquireDemandDepositAccountListRequestDto(
                ssafyApiHeaderFactory.create(INQUIRE_ACCOUNT_LIST_API_NAME, userKey)
        );
        SsafyInquireDemandDepositAccountListResponseDto response = post(
                request,
                ssafyApiProperties.getInquireDemandDepositAccountListPath(),
                SsafyInquireDemandDepositAccountListResponseDto.class,
                "계좌 목록 조회"
        );
        return Optional.ofNullable(response.rec()).orElse(List.of());
    }

    public SsafyDemandDepositAccountRecDto inquireDemandDepositAccount(String userKey, String accountNo) {
        SsafyInquireDemandDepositAccountRequestDto request = new SsafyInquireDemandDepositAccountRequestDto(
                ssafyApiHeaderFactory.create(INQUIRE_ACCOUNT_API_NAME, userKey),
                accountNo
        );
        SsafyInquireDemandDepositAccountResponseDto response = post(
                request,
                ssafyApiProperties.getInquireDemandDepositAccountPath(),
                SsafyInquireDemandDepositAccountResponseDto.class,
                "계좌 단건 조회"
        );
        if (response.rec() == null) {
            throw new CustomBusinessException(ErrorCode.INVALID_INPUT_VALUE, "SSAFY 계좌 단건 조회 응답이 비어 있습니다.");
        }
        return response.rec();
    }

    private <T> T post(Object requestBody, String path, Class<T> responseType, String actionName) {
        validateCommonConfiguration(path, actionName);

        try {
            T response = restClientBuilder.build()
                    .post()
                    .uri(buildUrl(path))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(responseType);

            if (response == null) {
                throw new CustomBusinessException(ErrorCode.INVALID_INPUT_VALUE, "SSAFY %s 응답이 비어 있습니다.".formatted(actionName));
            }
            validateSuccessCode(response, actionName);
            return response;
        } catch (RestClientResponseException e) {
            throw new CustomBusinessException(
                    ErrorCode.INVALID_INPUT_VALUE,
                    "SSAFY %s 요청 실패(status=%s, response=%s)"
                            .formatted(actionName, e.getStatusCode(), e.getResponseBodyAsString())
            );
        } catch (RestClientException e) {
            throw new CustomBusinessException(
                    ErrorCode.INVALID_INPUT_VALUE,
                    "SSAFY %s 요청 실패(cause=%s)".formatted(actionName, e.getMessage())
            );
        }
    }

    private <T> void validateSuccessCode(T response, String actionName) {
        String responseCode;
        String responseMessage;

        if (response instanceof SsafyCreateDemandDepositAccountResponseDto dto) {
            responseCode = dto.header() == null ? null : dto.header().responseCode();
            responseMessage = dto.header() == null ? null : dto.header().responseMessage();
        } else if (response instanceof SsafyInquireDemandDepositAccountListResponseDto dto) {
            responseCode = dto.header() == null ? null : dto.header().responseCode();
            responseMessage = dto.header() == null ? null : dto.header().responseMessage();
        } else if (response instanceof SsafyInquireDemandDepositAccountResponseDto dto) {
            responseCode = dto.header() == null ? null : dto.header().responseCode();
            responseMessage = dto.header() == null ? null : dto.header().responseMessage();
        } else {
            throw new CustomBusinessException(ErrorCode.INVALID_INPUT_VALUE, "SSAFY %s 응답 포맷이 올바르지 않습니다.".formatted(actionName));
        }

        if (!"H0000".equals(responseCode)) {
            throw new CustomBusinessException(
                    ErrorCode.INVALID_INPUT_VALUE,
                    "SSAFY %s 실패(code=%s, message=%s)"
                            .formatted(actionName, responseCode, responseMessage)
            );
        }
    }

    private void validateCommonConfiguration(String path, String actionName) {
        if (!hasText(ssafyApiProperties.getApiKey())) {
            throw new CustomBusinessException(ErrorCode.AUTH_CONFIG_INVALID, "SSAFY_API_KEY is required.");
        }
        if (!hasText(ssafyApiProperties.getInstitutionCode())) {
            throw new CustomBusinessException(ErrorCode.AUTH_CONFIG_INVALID, "SSAFY_INSTITUTION_CODE is required.");
        }
        if (!hasText(ssafyApiProperties.getFintechAppNo())) {
            throw new CustomBusinessException(ErrorCode.AUTH_CONFIG_INVALID, "SSAFY_FINTECH_APP_NO is required.");
        }
        if (!hasText(path)) {
            throw new CustomBusinessException(
                    ErrorCode.AUTH_CONFIG_INVALID,
                    "SSAFY_%s_PATH is required.".formatted(actionName.replace(" ", "_").toUpperCase())
            );
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

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
