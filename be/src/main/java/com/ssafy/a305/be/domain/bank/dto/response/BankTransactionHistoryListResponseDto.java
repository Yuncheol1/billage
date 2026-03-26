package com.ssafy.a305.be.domain.bank.dto.response;

import java.util.List;

/**
 * 연동 계좌 거래 내역 목록 (SSAFY inquireTransactionHistoryList와 대응).
 *
 * @param totalCount   조건에 맞는 전체 건수(SSAFY 문자열)
 * @param transactions 페이지/조회 결과에 포함된 거래 한 건 목록
 */
public record BankTransactionHistoryListResponseDto(
        String totalCount,
        List<BankTransactionItemResponseDto> transactions
) {
}
