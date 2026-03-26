package com.ssafy.a305.be.domain.bank.dto.response;

/**
 * 연동 계좌 거래 내역 한 건 (목록·단건 공통).
 * <p>
 * {@code transactionSummary}는 SSAFY 값 그대로(예: 계약번호 {@code C-...}).
 * {@code transactionBalance}는 화면용으로 절댓값 문자열.
 */
public record BankTransactionItemResponseDto(
        String transactionUniqueNo,
        String transactionDate,
        String transactionTime,
        String transactionType,
        String transactionTypeName,
        String transactionAccountNo,
        String transactionBalance,
        String transactionAfterBalance,
        String transactionSummary,
        String transactionMemo,
        String counterpartName
) {
}
