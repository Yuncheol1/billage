package com.ssafy.a305.be.domain.bank.client.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.ssafy.a305.be.domain.bank.client.dto.response.header.SsafyApiResponseHeaderDto;

/**
 * SSAFY 수시입출금 거래 내역 목록 조회 응답 바디.
 */
public record SsafyInquireTransactionHistoryListResponseDto(
        @JsonProperty("Header")
        SsafyApiResponseHeaderDto header,
        @JsonProperty("REC")
        SsafyInquireTransactionHistoryListRecDto rec
) {
}
