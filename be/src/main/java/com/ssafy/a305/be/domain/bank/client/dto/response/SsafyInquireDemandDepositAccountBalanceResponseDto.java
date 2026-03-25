package com.ssafy.a305.be.domain.bank.client.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.ssafy.a305.be.domain.bank.client.dto.response.header.SsafyApiResponseHeaderDto;

/**
 * SSAFY 수시입출금 계좌 잔액 조회 응답 바디.
 *
 * @param header 응답 Header
 * @param rec    잔액 및 계좌 요약 REC
 */
public record SsafyInquireDemandDepositAccountBalanceResponseDto(
        @JsonProperty("Header")
        SsafyApiResponseHeaderDto header,
        @JsonProperty("REC")
        Rec rec
) {
    /**
     * @param bankCode             은행 코드
     * @param accountNo            계좌번호
     * @param accountBalance       잔액
     * @param accountCreatedDate   개설일
     * @param accountExpiryDate    만기일
     * @param lastTransactionDate  최종 거래일
     * @param currency             통화 코드
     */
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
