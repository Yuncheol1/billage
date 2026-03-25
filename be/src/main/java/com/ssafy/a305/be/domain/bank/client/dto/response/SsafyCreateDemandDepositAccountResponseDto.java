package com.ssafy.a305.be.domain.bank.client.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.ssafy.a305.be.domain.bank.client.dto.response.header.SsafyApiResponseHeaderDto;

/**
 * SSAFY 수시입출금 계좌 생성 응답 바디.
 *
 * @param header 응답 Header
 * @param rec    생성된 계좌 정보(REC)
 */
public record SsafyCreateDemandDepositAccountResponseDto(
        @JsonProperty("Header")
        SsafyApiResponseHeaderDto header,
        @JsonProperty("REC")
        Rec rec
) {
    /**
     * @param bankCode  은행 코드
     * @param accountNo 개설된 계좌번호
     * @param currency  통화 정보
     */
    public record Rec(
            String bankCode,
            String accountNo,
            Currency currency
    ) {
    }

    /**
     * @param currency     통화 코드
     * @param currencyName 통화명
     */
    public record Currency(
            String currency,
            String currencyName
    ) {
    }
}
