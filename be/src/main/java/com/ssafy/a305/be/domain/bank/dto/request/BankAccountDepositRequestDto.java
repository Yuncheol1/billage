package com.ssafy.a305.be.domain.bank.dto.request;

/**
 * 연동 계좌 입금 요청.
 *
 * accountNo를 주면 allowlist에 포함된 계좌에만 입금할 수 있다.
 * accountNo를 생략하면(또는 빈 값이면) 로그인 사용자의 ACTIVE 계좌에 입금한다.
 *
 * @param transactionBalance 입금 금액(숫자 문자열)
 * @param accountNo          입금 대상 계좌번호(선택)
 */
public record BankAccountDepositRequestDto(
        String accountNo,
        String transactionBalance
) {
}
