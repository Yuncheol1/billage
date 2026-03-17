package com.ssafy.a305.be.domain.auth.client;

import com.ssafy.a305.be.domain.auth.client.dto.KakaoTokenResponse;
import com.ssafy.a305.be.domain.auth.client.dto.KakaoUserInfoResponse;
import com.ssafy.a305.be.domain.auth.config.KakaoOAuthProperties;
import com.ssafy.a305.be.global.exception.CustomBusinessException;
import com.ssafy.a305.be.global.exception.dto.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Component
@Slf4j
@RequiredArgsConstructor
public class KakaoOAuthClient {

    private final RestClient.Builder restClientBuilder;
    private final KakaoOAuthProperties kakaoOAuthProperties;

    public KakaoTokenResponse getToken(String authorizationCode, String redirectUri) {
        validateConfiguration();

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("grant_type", "authorization_code");
        formData.add("client_id", kakaoOAuthProperties.getClientId());
        formData.add("redirect_uri", redirectUri);
        formData.add("code", authorizationCode);

        if (hasText(kakaoOAuthProperties.getClientSecret())) {
            formData.add("client_secret", kakaoOAuthProperties.getClientSecret());
        }

        log.info("Kakao token exchange request: clientId={}, redirectUri={}, hasClientSecret={}",
                kakaoOAuthProperties.getClientId(),
                redirectUri,
                hasText(kakaoOAuthProperties.getClientSecret()));

        try {
            KakaoTokenResponse response = restClientBuilder.build()
                    .post()
                    .uri(kakaoOAuthProperties.getTokenUri())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(formData)
                    .retrieve()
                    .body(KakaoTokenResponse.class);

            if (response == null || !hasText(response.accessToken()) || !hasText(response.refreshToken())) {
                throw new CustomBusinessException(ErrorCode.KAKAO_AUTH_FAILED);
            }

            return response;
        } catch (RestClientResponseException e) {
            throw new CustomBusinessException(
                    ErrorCode.KAKAO_AUTH_FAILED,
                    "Kakao token exchange failed. redirectUri=%s, status=%s, response=%s"
                            .formatted(redirectUri, e.getStatusCode(), e.getResponseBodyAsString())
            );
        } catch (RestClientException e) {
            throw new CustomBusinessException(
                    ErrorCode.KAKAO_AUTH_FAILED,
                    "Kakao token exchange failed. redirectUri=%s, cause=%s"
                            .formatted(redirectUri, e.getMessage())
            );
        }
    }

    public KakaoUserInfoResponse getUserInfo(String accessToken) {
        try {
            KakaoUserInfoResponse response = restClientBuilder.build()
                    .get()
                    .uri(kakaoOAuthProperties.getUserInfoUri())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(KakaoUserInfoResponse.class);

            if (response == null || response.id() == null) {
                throw new CustomBusinessException(ErrorCode.KAKAO_USER_INFO_FAILED);
            }

            return response;
        } catch (RestClientException e) {
            throw new CustomBusinessException(ErrorCode.KAKAO_USER_INFO_FAILED);
        }
    }

    private void validateConfiguration() {
        if (!hasText(kakaoOAuthProperties.getClientId())) {
            throw new CustomBusinessException(ErrorCode.AUTH_CONFIG_INVALID, "KAKAO_CLIENT_ID is required.");
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
