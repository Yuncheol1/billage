package com.ssafy.a305.be.domain.auth.controller;

import com.ssafy.a305.be.domain.auth.config.KakaoOAuthProperties;
import com.ssafy.a305.be.domain.auth.dto.request.ConsentCompletionRequestDto;
import com.ssafy.a305.be.domain.auth.dto.request.DevTokenIssueRequestDto;
import com.ssafy.a305.be.domain.auth.dto.request.KakaoLoginRequestDto;
import com.ssafy.a305.be.domain.auth.dto.request.LogoutRequestDto;
import com.ssafy.a305.be.domain.auth.dto.request.TokenReissueRequestDto;
import com.ssafy.a305.be.domain.auth.dto.response.ConsentCompletionResponseDto;
import com.ssafy.a305.be.domain.auth.dto.response.ConsentListResponseDto;
import com.ssafy.a305.be.domain.auth.dto.response.DevTokenIssueResponseDto;
import com.ssafy.a305.be.domain.auth.dto.response.KakaoLoginResponseDto;
import com.ssafy.a305.be.domain.auth.dto.response.TokenReissueResponseDto;
import com.ssafy.a305.be.domain.auth.security.AuthenticatedUser;
import com.ssafy.a305.be.domain.auth.service.AuthService;
import com.ssafy.a305.be.global.exception.CustomBusinessException;
import com.ssafy.a305.be.global.exception.dto.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@Slf4j
@RequiredArgsConstructor
public class AuthController {

    private static final String KAKAO_AUTHORIZE_URI = "https://kauth.kakao.com/oauth/authorize";
    private static final String APP_CALLBACK_URI = "billage://auth/callback";

    private final AuthService authService;
    private final KakaoOAuthProperties kakaoOAuthProperties;

    @GetMapping("/kakao/authorize")
    public ResponseEntity<Void> redirectToKakaoAuthorize() {
        validateKakaoClientId();

        UriComponentsBuilder authorizeUriBuilder = UriComponentsBuilder.fromUriString(KAKAO_AUTHORIZE_URI)
                .queryParam("response_type", "code")
                .queryParam("client_id", kakaoOAuthProperties.getClientId())
                .queryParam("redirect_uri", buildKakaoCallbackUri());

        if (StringUtils.hasText(kakaoOAuthProperties.getScope())) {
            authorizeUriBuilder.queryParam("scope", kakaoOAuthProperties.getScope());
        }

        URI authorizeUri = authorizeUriBuilder
                .build()
                .encode()
                .toUri();

        log.info("Kakao authorize redirect: clientId={}, redirectUri={}, scope={}, location={}",
                kakaoOAuthProperties.getClientId(),
                buildKakaoCallbackUri(),
                kakaoOAuthProperties.getScope(),
                authorizeUri);

        return ResponseEntity.status(HttpStatus.FOUND)
                .location(authorizeUri)
                .build();
    }

    @PostMapping("/kakao/login")
    public ResponseEntity<Map<String, Object>> loginWithKakao(@Valid @RequestBody KakaoLoginRequestDto requestDto) {
        KakaoLoginResponseDto responseDto = authService.loginWithKakao(requestDto);
        return ResponseEntity.ok(successResponse("Login successful", responseDto));
    }

    @GetMapping("/kakao/callback")
    public ResponseEntity<Void> loginWithKakaoCallback(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String error,
            @RequestParam(name = "error_description", required = false) String errorDescription,
            HttpServletRequest request) {
        if (error != null) {
            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(buildAppErrorRedirectUri(error, errorDescription))
                    .build();
        }

        if (code == null || code.isBlank()) {
            throw new CustomBusinessException(ErrorCode.INVALID_INPUT_VALUE, "authorization code is missing.");
        }

        KakaoLoginResponseDto responseDto = authService.loginWithKakao(
                new KakaoLoginRequestDto(code, request.getRequestURL().toString())
        );

        return ResponseEntity.status(HttpStatus.FOUND)
                .location(buildAppSuccessRedirectUri(responseDto))
                .build();
    }

    @PostMapping("/reissue")
    public ResponseEntity<Map<String, Object>> reissue(@Valid @RequestBody TokenReissueRequestDto requestDto) {
        TokenReissueResponseDto responseDto = authService.reissue(requestDto);
        return ResponseEntity.ok(successResponse("Token reissued", responseDto));
    }

    @PostMapping("/dev-token")
    public ResponseEntity<Map<String, Object>> issueDevToken(
            @Valid @RequestBody DevTokenIssueRequestDto requestDto,
            HttpServletRequest request) {
        validateLocalRequest(request);
        DevTokenIssueResponseDto responseDto = authService.issueDevToken(requestDto.userSeq());
        return ResponseEntity.ok(successResponse("Dev token issued", responseDto));
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> logout(@Valid @RequestBody LogoutRequestDto requestDto) {
        authService.logout(requestDto);

        Map<String, Object> responseBody = new LinkedHashMap<>();
        responseBody.put("status", HttpStatus.OK.value());
        responseBody.put("message", "Logged out");
        responseBody.put("data", null);
        return ResponseEntity.ok(responseBody);
    }

    @PostMapping("/consents")
    public ResponseEntity<Map<String, Object>> completeConsents(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @Valid @RequestBody ConsentCompletionRequestDto requestDto) {
        ConsentCompletionResponseDto responseDto = authService.completeConsents(authenticatedUser.userSeq(), requestDto);
        return ResponseEntity.ok(successResponse("Consents completed", responseDto));
    }

    @GetMapping("/consents")
    public ResponseEntity<Map<String, Object>> getConsents(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
        ConsentListResponseDto responseDto = authService.getConsents(authenticatedUser.userSeq());
        return ResponseEntity.ok(successResponse("Consent list fetched", responseDto));
    }

    private void validateKakaoClientId() {
        if (kakaoOAuthProperties.getClientId() == null || kakaoOAuthProperties.getClientId().isBlank()) {
            throw new CustomBusinessException(ErrorCode.AUTH_CONFIG_INVALID, "KAKAO_CLIENT_ID is required.");
        }
    }

    private void validateLocalRequest(HttpServletRequest request) {
        String remoteAddr = request.getRemoteAddr();
        if (!"127.0.0.1".equals(remoteAddr)
                && !"0:0:0:0:0:0:0:1".equals(remoteAddr)
                && !"::1".equals(remoteAddr)) {
            throw new CustomBusinessException(ErrorCode.ACCESS_DENIED, "개발용 토큰 발급 API는 로컬 환경에서만 사용할 수 있습니다.");
        }
    }

    private String buildKakaoCallbackUri() {
        return ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/v1/auth/kakao/callback")
                .build()
                .toUriString();
    }

    private URI buildAppErrorRedirectUri(String error, String errorDescription) {
        return UriComponentsBuilder.fromUriString(APP_CALLBACK_URI)
                .queryParam("error", error)
                .queryParam("error_description", errorDescription)
                .build()
                .encode()
                .toUri();
    }

    private URI buildAppSuccessRedirectUri(KakaoLoginResponseDto responseDto) {
        return UriComponentsBuilder.fromUriString(APP_CALLBACK_URI)
                .queryParam("accessToken", responseDto.getAccessToken())
                .queryParam("refreshToken", responseDto.getRefreshToken())
                .queryParam("accessTokenExpiresIn", responseDto.getAccessTokenExpiresIn())
                .queryParam("refreshTokenExpiresIn", responseDto.getRefreshTokenExpiresIn())
                .queryParam("isNewUser", responseDto.isNewUser())
                .queryParam("userSeq", responseDto.getUserSeq())
                .queryParam("userState", responseDto.getUserState())
                .build()
                .encode()
                .toUri();
    }

    private Map<String, Object> successResponse(String message, Object data) {
        return Map.of(
                "status", HttpStatus.OK.value(),
                "message", message,
                "data", data
        );
    }
}
