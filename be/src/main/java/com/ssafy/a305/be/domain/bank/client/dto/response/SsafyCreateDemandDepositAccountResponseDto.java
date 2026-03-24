package com.ssafy.a305.be.domain.bank.client.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.ssafy.a305.be.domain.bank.client.dto.response.header.SsafyApiResponseHeaderDto;

/** 수시입출금 계좌 생성 응답 DTO */
public record SsafyCreateDemandDepositAccountResponseDto(
        @JsonProperty("Header")
        SsafyApiResponseHeaderDto header,
        @JsonProperty("REC")
        Rec rec
) {
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
