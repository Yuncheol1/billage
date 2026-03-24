package com.ssafy.a305.be.domain.bank.client.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.ssafy.a305.be.domain.bank.client.dto.response.header.SsafyApiResponseHeaderDto;

/** 수시입출금 계좌 잔액 조회 응답 DTO */
public record SsafyInquireDemandDepositAccountBalanceResponseDto(
        @JsonProperty("Header")
        SsafyApiResponseHeaderDto header,
        @JsonProperty("REC")
        Rec rec
) {
    public record Rec(
            String bankCode,
            String accountNo,
            String accountBalance,
            String accountCreatedDate,
            String accountExpiryDate,
            String lastTransactionDate,
            String currency
    ) {
    }
}
