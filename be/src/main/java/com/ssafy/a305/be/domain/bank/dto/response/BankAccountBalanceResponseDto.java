package com.ssafy.a305.be.domain.bank.dto.response;

/**
 * 연동 계좌의 SSAFY 잔액 조회 결과 (우리 API 응답).
 *
 * @param accountSeq          우리 DB 계좌 일련번호
 * @param accountNo            원문 계좌번호 (DB 저장값)
 * @param bankCode            은행 코드 (SSAFY 잔액 응답)
 * @param accountBalance      잔액 (문자열)
 * @param accountCreatedDate  개설일 (SSAFY)
 * @param accountExpiryDate   만기일 (SSAFY)
 * @param lastTransactionDate 최종 거래일 (SSAFY)
 * @param currency            통화 코드
 */
public record BankAccountBalanceResponseDto(
        Integer accountSeq,
        String accountNo,
        String bankCode,
        String accountBalance,
        String accountCreatedDate,
        String accountExpiryDate,
        String lastTransactionDate,
        String currency
) {
}
