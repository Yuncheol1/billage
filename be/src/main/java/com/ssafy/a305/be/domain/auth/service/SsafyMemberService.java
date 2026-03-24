package com.ssafy.a305.be.domain.auth.service;

import com.ssafy.a305.be.domain.auth.config.SsafyApiProperties;
import com.ssafy.a305.be.global.exception.CustomBusinessException;
import com.ssafy.a305.be.global.exception.dto.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class SsafyMemberService {

    private final RestClient.Builder restClientBuilder;
    private final SsafyApiProperties ssafyApiProperties;

    public String getOrCreateUserKey(String email) {
        String userKey = searchUserKey(email);
        if (hasText(userKey)) {
            return userKey;
        }
        return createUserKey(email);
    }

    private String searchUserKey(String email) {
        try {
            Map<?, ?> response = restClientBuilder.build()
                    .post()
                    .uri(buildUrl(ssafyApiProperties.getMemberSearchPath()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(buildRequestBody(email))
                    .retrieve()
                    .body(Map.class);

            return extractUserKey(response);
        } catch (RestClientException e) {
            log.info("SSAFY userKey search failed. email={}, cause={}", email, e.getMessage());
            return null;
        }
    }

    private String createUserKey(String email) {
        try {
            Map<?, ?> response = restClientBuilder.build()
                    .post()
                    .uri(buildUrl(ssafyApiProperties.getMemberCreatePath()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(buildRequestBody(email))
                    .retrieve()
                    .body(Map.class);

            String userKey = extractUserKey(response);
            if (!hasText(userKey)) {
                throw new CustomBusinessException(ErrorCode.INVALID_INPUT_VALUE, "SSAFY userKey 발급 응답이 올바르지 않습니다.");
            }
            return userKey;
        } catch (RestClientException e) {
            throw new CustomBusinessException(
                    ErrorCode.INVALID_INPUT_VALUE,
                    "SSAFY userKey 발급에 실패했습니다. cause=%s".formatted(e.getMessage())
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

    private Map<String, String> buildRequestBody(String email) {
        if (!hasText(email)) {
            throw new CustomBusinessException(ErrorCode.INVALID_INPUT_VALUE, "이메일이 없어 SSAFY userKey를 생성할 수 없습니다.");
        }
        if (!hasText(ssafyApiProperties.getApiKey())) {
            throw new CustomBusinessException(ErrorCode.AUTH_CONFIG_INVALID, "SSAFY_API_KEY is required.");
        }

        Map<String, String> request = new HashMap<>();
        request.put("apiKey", ssafyApiProperties.getApiKey());
        request.put("userId", email);
        return request;
    }

    private String extractUserKey(Map<?, ?> response) {
        if (response == null || response.isEmpty()) {
            return null;
        }

        Object direct = response.get("userKey");
        if (direct instanceof String directValue && hasText(directValue)) {
            return directValue;
        }

        Object rec = response.get("REC");
        if (rec instanceof Map<?, ?> recMap) {
            Object nested = recMap.get("userKey");
            if (nested instanceof String nestedValue && hasText(nestedValue)) {
                return nestedValue;
            }
        }

        return null;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
