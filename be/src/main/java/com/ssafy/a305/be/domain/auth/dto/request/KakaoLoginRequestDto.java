package com.ssafy.a305.be.domain.auth.dto.request;

import jakarta.validation.constraints.NotBlank;

public record KakaoLoginRequestDto(
        @NotBlank(message = "authorizationCode는 필수입니다.")
        String authorizationCode,

        @NotBlank(message = "redirectUri는 필수입니다.")
        String redirectUri
) {
}
