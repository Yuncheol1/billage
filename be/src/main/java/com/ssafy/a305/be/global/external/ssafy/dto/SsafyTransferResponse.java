package com.ssafy.a305.be.global.external.ssafy.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.List;

/**
 * SSAFY 계좌 이체 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SsafyTransferResponse {

    @JsonProperty("Header")
    private SsafyApiHeader header;

    @JsonProperty("REC")
    private List<TransactionRecord> rec;

    public boolean isSuccess() {
        return header != null && "H0000".equals(header.getResponseCode());
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TransactionRecord {

        @JsonProperty("transactionUniqueNo")
        private String transactionUniqueNo;

        @JsonProperty("accountNo")
        private String accountNo;

        @JsonProperty("transactionDate")
        private String transactionDate;

        @JsonProperty("transactionType")
        private String transactionType;

        @JsonProperty("transactionTypeName")
        private String transactionTypeName;

        @JsonProperty("transactionAccountNo")
        private String transactionAccountNo;
    }
}
