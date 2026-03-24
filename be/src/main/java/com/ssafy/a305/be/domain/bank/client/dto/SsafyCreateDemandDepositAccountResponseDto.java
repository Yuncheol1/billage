package com.ssafy.a305.be.domain.bank.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record SsafyCreateDemandDepositAccountResponseDto(
        @JsonProperty("Header")
        Header header,
        @JsonProperty("REC")
        Rec rec
) {
    public record Header(
            String responseCode,
            String responseMessage,
            String apiName,
            String transmissionDate,
            String transmissionTime,
            String institutionCode,
            String apiKey,
            String apiServiceCode,
            String institutionTransactionUniqueNo
    ) {
    }

    public record Rec(
            String bankCode,
            String accountNo,
            Currency currency
    ) {
    }

    public record Currency(
            String currency,
            String currencyName
    ) {
    }
}
