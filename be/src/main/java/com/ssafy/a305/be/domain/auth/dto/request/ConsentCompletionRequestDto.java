package com.ssafy.a305.be.domain.auth.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record ConsentCompletionRequestDto(
        @NotEmpty(message = "consents는 비어 있을 수 없습니다.")
        List<@Valid ConsentItem> consents
) {
    public record ConsentItem(
            @NotNull(message = "termsSeq는 필수입니다.")
            Integer termsSeq,
            @NotNull(message = "agreed는 필수입니다.")
            Boolean agreed
    ) {
    }
}
