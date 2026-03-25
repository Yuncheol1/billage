package com.ssafy.a305.be.domain.bank.client.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * SSAFY 수시입출금 거래 내역 REC 한 건 (목록 원소 및 단건 조회 본문).
 */
public record SsafyDemandDepositTransactionRecDto(
        @JsonProperty("transactionUniqueNo")
        String transactionUniqueNo,
        @JsonProperty("transactionDate")
        String transactionDate,
        @JsonProperty("transactionTime")
        String transactionTime,
        @JsonProperty("transactionType")
        String transactionType,
        @JsonProperty("transactionTypeName")
        String transactionTypeName,
        @JsonProperty("transactionAccountNo")
        String transactionAccountNo,
        @JsonProperty("transactionBalance")
        String transactionBalance,
        @JsonProperty("transactionAfterBalance")
        String transactionAfterBalance,
        @JsonProperty("transactionSummary")
        String transactionSummary,
        @JsonProperty("transactionMemo")
        String transactionMemo
) {
}
