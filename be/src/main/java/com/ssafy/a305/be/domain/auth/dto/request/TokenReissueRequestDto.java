package com.ssafy.a305.be.domain.auth.dto.request;

import jakarta.validation.constraints.NotBlank;

public record TokenReissueRequestDto(
        @NotBlank(message = "refreshToken은 필수입니다.")
        String refreshToken
) {
}
