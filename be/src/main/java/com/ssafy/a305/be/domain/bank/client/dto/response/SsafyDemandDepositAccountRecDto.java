package com.ssafy.a305.be.domain.bank.client.dto.response;

/**
 * SSAFY 수시입출금 계좌 목록/단건 조회 응답의 {@code REC} 본문 (공통 필드).
 *
 * @param bankCode             은행 코드
 * @param bankName             은행명
 * @param userName             회원명
 * @param accountNo            계좌번호 (전체)
 * @param accountName          상품/계좌 명칭
 * @param accountTypeCode      계좌 유형 코드
 * @param accountTypeName      계좌 유형명
 * @param accountCreatedDate   개설일
 * @param accountExpiryDate    만기일
 * @param dailyTransferLimit   1일 이체 한도
 * @param oneTimeTransferLimit 1회 이체 한도
 * @param accountBalance       잔액
 * @param lastTransactionDate  최종 거래일
 * @param currency             통화 코드
 */
public record SsafyDemandDepositAccountRecDto(
        String bankCode,
        String bankName,
        String userName,
        String accountNo,
        String accountName,
        String accountTypeCode,
        String accountTypeName,
        String accountCreatedDate,
        String accountExpiryDate,
        String dailyTransferLimit,
        String oneTimeTransferLimit,
        String accountBalance,
        String lastTransactionDate,
        String currency
) {
}
