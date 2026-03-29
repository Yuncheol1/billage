package com.ssafy.a305.be.domain.contract.dto.request;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ContractSignatureRequestDto(
        @NotBlank(message = "서명 방식은 필수입니다.")
        String method,

        String deviceIdHash,

        Boolean biometricVerified,

        String userAgent,

        String publicKeyPem,

        @NotNull(message = "signaturePayload는 필수입니다.")
        JsonNode signaturePayload
) {
}
