package com.ssafy.a305.be.global.external.ssafy.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

/**
 * SSAFY 계좌 이체 요청 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SsafyTransferRequest {

    @JsonProperty("Header")
    private SsafyApiHeader header;

    /** 입금계좌번호 (채권자 계좌) */
    @JsonProperty("depositAccountNo")
    private String depositAccountNo;

    /** 거래금액 */
    @JsonProperty("transactionBalance")
    private String transactionBalance;

    /** 출금계좌번호 (채무자 계좌) */
    @JsonProperty("withdrawalAccountNo")
    private String withdrawalAccountNo;

    /** 거래 요약내용 (입금계좌) */
    @JsonProperty("depositTransactionSummary")
    private String depositTransactionSummary;

    /** 거래 요약내용 (출금계좌) */
    @JsonProperty("withdrawalTransactionSummary")
    private String withdrawalTransactionSummary;
}
