package com.ssafy.a305.be.domain.bank.client.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.ssafy.a305.be.domain.bank.client.dto.request.header.SsafyApiHeaderDto;

/**
 * SSAFY 수시입출금 거래 내역 목록 조회(inquireTransactionHistoryList) 요청 바디.
 */
public record SsafyInquireTransactionHistoryListRequestDto(
        @JsonProperty("Header")
        SsafyApiHeaderDto header,
        String accountNo,
        String startDate,
        String endDate,
        String transactionType,
        String orderByType
) {
}
