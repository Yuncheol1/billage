package com.ssafy.a305.be.domain.bank.client;

import com.ssafy.a305.be.domain.auth.config.SsafyApiProperties;
import com.ssafy.a305.be.domain.bank.client.dto.request.SsafyCreateDemandDepositAccountRequestDto;
import com.ssafy.a305.be.domain.bank.client.dto.request.SsafyInquireDemandDepositAccountListRequestDto;
import com.ssafy.a305.be.domain.bank.client.dto.request.SsafyInquireDemandDepositAccountRequestDto;
import com.ssafy.a305.be.domain.bank.client.dto.request.SsafyInquireTransactionHistoryListRequestDto;
import com.ssafy.a305.be.domain.bank.client.dto.request.SsafyInquireTransactionHistoryRequestDto;
import com.ssafy.a305.be.domain.bank.client.dto.request.SsafyUpdateDemandDepositAccountDepositRequestDto;
import com.ssafy.a305.be.domain.bank.client.dto.response.SsafyCreateDemandDepositAccountResponseDto;
import com.ssafy.a305.be.domain.bank.client.dto.response.SsafyDemandDepositAccountRecDto;
import com.ssafy.a305.be.domain.bank.client.dto.response.SsafyDemandDepositTransactionRecDto;
import com.ssafy.a305.be.domain.bank.client.dto.response.SsafyInquireDemandDepositAccountListResponseDto;
import com.ssafy.a305.be.domain.bank.client.dto.response.SsafyInquireDemandDepositAccountResponseDto;
import com.ssafy.a305.be.domain.bank.client.dto.response.SsafyInquireTransactionHistoryListRecDto;
import com.ssafy.a305.be.domain.bank.client.dto.response.SsafyInquireTransactionHistoryListResponseDto;
import com.ssafy.a305.be.domain.bank.client.dto.response.SsafyInquireTransactionHistoryResponseDto;
import com.ssafy.a305.be.domain.bank.client.dto.response.SsafyUpdateDemandDepositAccountDepositResponseDto;
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
    private static final String INQUIRE_TRANSACTION_HISTORY_LIST_API_NAME = "inquireTransactionHistoryList";
    private static final String INQUIRE_TRANSACTION_HISTORY_API_NAME = "inquireTransactionHistory";
    private static final String UPDATE_ACCOUNT_DEPOSIT_API_NAME = "updateDemandDepositAccountDeposit";

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
                "계좌 생성",
                "ssafy.api.create-demand-deposit-account-path"
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
                "계좌 목록 조회",
                "ssafy.api.inquire-demand-deposit-account-list-path"
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
                "계좌 단건 조회",
                "ssafy.api.inquire-demand-deposit-account-path"
        );
        if (response.rec() == null) {
            throw new CustomBusinessException(ErrorCode.INVALID_INPUT_VALUE, "SSAFY 계좌 단건 조회 응답이 비어 있습니다.");
        }
        return response.rec();
    }

    /**
     * @return {@code REC}. 없으면 {@code null} (호출부에서 빈 목록으로 처리).
     */
    public SsafyInquireTransactionHistoryListRecDto inquireTransactionHistoryList(
            String userKey,
            String accountNo,
            String startDate,
            String endDate,
            String transactionType,
            String orderByType
    ) {
        SsafyInquireTransactionHistoryListRequestDto request = new SsafyInquireTransactionHistoryListRequestDto(
                ssafyApiHeaderFactory.create(INQUIRE_TRANSACTION_HISTORY_LIST_API_NAME, userKey),
                accountNo,
                startDate,
                endDate,
                transactionType,
                orderByType
        );
        SsafyInquireTransactionHistoryListResponseDto response = post(
                request,
                ssafyApiProperties.getInquireTransactionHistoryListPath(),
                SsafyInquireTransactionHistoryListResponseDto.class,
                "거래 내역 목록 조회",
                "ssafy.api.inquire-transaction-history-list-path"
        );
        return response.rec();
    }

    public SsafyDemandDepositTransactionRecDto inquireTransactionHistory(
            String userKey,
            String accountNo,
            String transactionUniqueNo
    ) {
        SsafyInquireTransactionHistoryRequestDto request = new SsafyInquireTransactionHistoryRequestDto(
                ssafyApiHeaderFactory.create(INQUIRE_TRANSACTION_HISTORY_API_NAME, userKey),
                accountNo,
                transactionUniqueNo
        );
        SsafyInquireTransactionHistoryResponseDto response = post(
                request,
                ssafyApiProperties.getInquireTransactionHistoryPath(),
                SsafyInquireTransactionHistoryResponseDto.class,
                "거래 내역 단건 조회",
                "ssafy.api.inquire-transaction-history-path"
        );
        if (response.rec() == null) {
            throw new CustomBusinessException(ErrorCode.INVALID_INPUT_VALUE, "SSAFY 거래 내역 단건 조회 응답이 비어 있습니다.");
        }
        return response.rec();
    }

    public SsafyUpdateDemandDepositAccountDepositResponseDto.Rec updateDemandDepositAccountDeposit(
            String userKey,
            String accountNo,
            String transactionBalance,
            String transactionSummary
    ) {
        SsafyUpdateDemandDepositAccountDepositRequestDto request = new SsafyUpdateDemandDepositAccountDepositRequestDto(
                ssafyApiHeaderFactory.create(UPDATE_ACCOUNT_DEPOSIT_API_NAME, userKey),
                accountNo,
                transactionBalance,
                transactionSummary
        );
        SsafyUpdateDemandDepositAccountDepositResponseDto response = post(
                request,
                ssafyApiProperties.getUpdateDemandDepositAccountDepositPath(),
                SsafyUpdateDemandDepositAccountDepositResponseDto.class,
                "계좌 입금",
                "ssafy.api.update-demand-deposit-account-deposit-path"
        );
        if (response.rec() == null) {
            throw new CustomBusinessException(ErrorCode.INVALID_INPUT_VALUE, "SSAFY 계좌 입금 응답이 비어 있습니다.");
        }
        return response.rec();
    }

    private <T> T post(
            Object requestBody,
            String path,
            Class<T> responseType,
            String actionName,
            String pathPropertyKey
    ) {
        validateCommonConfiguration(path, pathPropertyKey);

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
        } else if (response instanceof SsafyInquireTransactionHistoryListResponseDto dto) {
            responseCode = dto.header() == null ? null : dto.header().responseCode();
            responseMessage = dto.header() == null ? null : dto.header().responseMessage();
        } else if (response instanceof SsafyInquireTransactionHistoryResponseDto dto) {
            responseCode = dto.header() == null ? null : dto.header().responseCode();
            responseMessage = dto.header() == null ? null : dto.header().responseMessage();
        } else if (response instanceof SsafyUpdateDemandDepositAccountDepositResponseDto dto) {
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

    private void validateCommonConfiguration(String path, String pathPropertyKey) {
        if (!hasText(ssafyApiProperties.getApiKey())) {
            throw new CustomBusinessException(ErrorCode.AUTH_CONFIG_INVALID, "ssafy.api.api-key is required.");
        }
        if (!hasText(ssafyApiProperties.getInstitutionCode())) {
            throw new CustomBusinessException(ErrorCode.AUTH_CONFIG_INVALID, "ssafy.api.institution-code is required.");
        }
        if (!hasText(ssafyApiProperties.getFintechAppNo())) {
            throw new CustomBusinessException(ErrorCode.AUTH_CONFIG_INVALID, "ssafy.api.fintech-app-no is required.");
        }
        if (!hasText(path)) {
            throw new CustomBusinessException(
                    ErrorCode.AUTH_CONFIG_INVALID,
                    "%s is required.".formatted(pathPropertyKey)
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
