package com.ssafy.a305.be.domain.bank.dto.response;

/**
 * SSAFY 수시입출금 계좌 목록 조회 결과를 클라이언트에 내려주기 위한 항목 DTO.
 * 계좌번호는 마스킹된 값만 포함한다.
 *
 * @param bankCode            은행 코드 (예: 001)
 * @param bankName            은행명
 * @param userName            SSAFY 회원명
 * @param accountNoMasked     마스킹된 계좌번호 (16자리는 {@code XXXX-****-****-XXXX}, 그 외 8자 초과는 {@code XXXX-****-XXXX})
 * @param accountName         상품/계좌 명칭
 * @param accountTypeCode     계좌 유형 코드
 * @param accountTypeName     계좌 유형명 (예: 수시입출금)
 * @param accountCreatedDate  계좌 개설일 (yyyyMMdd 등 SSAFY 문자열)
 * @param accountExpiryDate   계좌 만기일
 * @param dailyTransferLimit  1일 이체 한도
 * @param oneTimeTransferLimit 1회 이체 한도
 * @param accountBalance      잔액 (문자열)
 * @param lastTransactionDate 마지막 거래일 (없으면 빈 문자열 등)
 * @param currency            통화 코드 (예: KRW)
 * @param accountSeq          우리 DB 계좌 일련번호. 아직 서비스에 연동되지 않은 SSAFY 계좌면 {@code null}
 * @param isPrimary           우리 DB 기준 대표 계좌 여부. 미연동이면 {@code false}
 */
public record BankAccountListItemResponseDto(
        String bankCode,
        String bankName,
        String userName,
        String accountNoMasked,
        String accountName,
        String accountTypeCode,
        String accountTypeName,
        String accountCreatedDate,
        String accountExpiryDate,
        String dailyTransferLimit,
        String oneTimeTransferLimit,
        String accountBalance,
        String lastTransactionDate,
        String currency,
        Integer accountSeq,
        Boolean isPrimary
) {
}
