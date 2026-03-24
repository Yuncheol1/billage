package com.ssafy.a305.be.domain.bank.client.dto.response;

/** 계좌 목록/단건 조회에서 공통으로 사용하는 계좌 본문(REC) DTO */
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
