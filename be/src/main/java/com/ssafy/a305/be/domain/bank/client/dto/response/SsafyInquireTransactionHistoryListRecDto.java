package com.ssafy.a305.be.domain.bank.client.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * SSAFY 거래 내역 목록 조회 응답의 {@code REC} (totalCount + list).
 */
public record SsafyInquireTransactionHistoryListRecDto(
        @JsonProperty("totalCount")
        String totalCount,
        @JsonProperty("list")
        List<SsafyDemandDepositTransactionRecDto> list
) {
}
