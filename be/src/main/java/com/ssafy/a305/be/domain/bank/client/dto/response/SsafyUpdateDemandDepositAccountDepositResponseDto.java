package com.ssafy.a305.be.domain.bank.client.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.ssafy.a305.be.domain.bank.client.dto.response.header.SsafyApiResponseHeaderDto;

/**
 * SSAFY 수시입출금 입금(updateDemandDepositAccountDeposit) 응답 바디.
 */
public record SsafyUpdateDemandDepositAccountDepositResponseDto(
        @JsonProperty("Header")
        SsafyApiResponseHeaderDto header,
        @JsonProperty("REC")
        Rec rec
) {
    public record Rec(
            String transactionUniqueNo,
            String transactionDate
    ) {
    }
}
