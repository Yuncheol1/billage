package com.ssafy.a305.be.domain.bank.dto.response;

/**
 * 연동 계좌 입금 결과.
 *
 * @param transactionUniqueNo SSAFY 거래고유번호
 * @param transactionDate     거래일자(yyyyMMdd)
 */
public record BankAccountDepositResponseDto(
        String transactionUniqueNo,
        String transactionDate
) {
}
