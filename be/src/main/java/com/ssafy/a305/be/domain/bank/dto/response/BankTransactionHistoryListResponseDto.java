package com.ssafy.a305.be.domain.bank.dto.response;

import java.util.List;

/**
 * 연동 계좌 거래 내역 목록 (SSAFY totalCount + 목록).
 */
public record BankTransactionHistoryListResponseDto(
        String totalCount,
        List<BankTransactionItemResponseDto> transactions
) {
}
