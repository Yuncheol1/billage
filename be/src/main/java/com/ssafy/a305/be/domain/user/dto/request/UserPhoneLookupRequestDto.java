package com.ssafy.a305.be.domain.user.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

public record UserPhoneLookupRequestDto(
        @NotBlank(message = "phone is required.")
        String phone,
        @NotBlank(message = "realName is required.")
        String realName
) {
}
