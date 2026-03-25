package com.ssafy.a305.be.domain.bank.dto.response;

/**
 * 연동 계좌 거래 내역 한 건 (SSAFY REC와 동일 필드).
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
        String transactionMemo
) {
}
