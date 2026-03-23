package com.ssafy.a305.be.domain.auth.dto.request;

import jakarta.validation.constraints.NotNull;

public record DevTokenIssueRequestDto(
        @NotNull(message = "userSeq는 필수입니다.")
        Integer userSeq
) {
}
