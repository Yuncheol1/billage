package com.ssafy.a305.be.domain.user.dto.request;

import jakarta.validation.constraints.NotBlank;

public record UserPhoneLookupRequestDto(
        @NotBlank(message = "phone is required.")
        String phone
) {
}
