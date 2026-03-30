package com.ssafy.a305.be.domain.bank.dto.response;

/**
 * 로그인 사용자의 우리 서비스 연동 계좌(활성) 존재 여부.
 *
 * @param hasBankAccount {@code true}이면 ACTIVE 상태의 bank_account 행이 최소 1건 존재
 */
public record BankMeResponseDto(boolean hasBankAccount) {
}
