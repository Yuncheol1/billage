package com.ssafy.a305.be.domain.bank.dto.request;

/**
 * 연동 계좌 입금 요청.
 *
 * @param transactionBalance 입금 금액(숫자 문자열)
 */
public record BankAccountDepositRequestDto(
        String transactionBalance
) {
}
