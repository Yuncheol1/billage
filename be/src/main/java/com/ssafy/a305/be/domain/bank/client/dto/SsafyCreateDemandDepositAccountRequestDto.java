package com.ssafy.a305.be.domain.bank.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record SsafyCreateDemandDepositAccountRequestDto(
        @JsonProperty("Header")
        Header header,
        String accountTypeUniqueNo
) {
    public record Header(
            String apiName,
            String transmissionDate,
            String transmissionTime,
            String institutionCode,
            String fintechAppNo,
            String apiServiceCode,
            String institutionTransactionUniqueNo,
            String apiKey,
            String userKey
    ) {
    }
}
