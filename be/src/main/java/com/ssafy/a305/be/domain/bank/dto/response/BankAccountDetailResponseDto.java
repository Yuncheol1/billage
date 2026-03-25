package com.ssafy.a305.be.domain.bank.dto.response;

/**
 * 연동 계좌 단건 조회(화면 표시용) 응답 DTO.
 * 프론트에서 필요한 필드만 슬림하게 구성한다.
 *
 * @param bankName         은행명
 * @param accountTypeName  계좌 유형명(예: 수시입출금)
 * @param accountNo        계좌번호(원문)
 * @param accountBalance   잔액
 */
public record BankAccountDetailResponseDto(
        String bankName,
        String accountTypeName,
        String accountNo,
        String accountBalance
) {
}

