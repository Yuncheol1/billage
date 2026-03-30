package com.ssafy.a305.be.domain.bank.client.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.ssafy.a305.be.domain.bank.client.dto.request.header.SsafyApiHeaderDto;

/**
 * SSAFY 수시입출금 거래 내역 단건 조회(inquireTransactionHistory) 요청 바디.
 */
public record SsafyInquireTransactionHistoryRequestDto(
        @JsonProperty("Header")
        SsafyApiHeaderDto header,
        String accountNo,
        String transactionUniqueNo
) {
}
