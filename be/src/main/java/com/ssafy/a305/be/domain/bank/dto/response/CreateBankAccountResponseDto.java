package com.ssafy.a305.be.domain.bank.dto.response;

import com.ssafy.a305.be.domain.account.entity.BankAccount;

import java.time.LocalDateTime;

/**
 * 우리 서버의 계좌 생성 API 응답 DTO.
 *
 * @param accountSeq           내부 DB 계좌 일련번호 (bank_account.account_seq)
 * @param bankCode             은행 코드 (SSAFY 응답 기준)
 * @param accountNo             계좌번호 원문
 * @param accountTypeUniqueNo  개설에 사용한 상품 고유번호 (SSAFY accountTypeUniqueNo)
 * @param isPrimary            해당 사용자의 대표(첫) 활성 계좌 여부
 * @param status               계좌 상태 enum 이름 (예: ACTIVE)
 * @param linkedDttm           우리 서비스에 연동(저장)된 시각
 */
public record CreateBankAccountResponseDto(
        Integer accountSeq,
        String bankCode,
        String accountNo,
        String accountTypeUniqueNo,
        Boolean isPrimary,
        String status,
        LocalDateTime linkedDttm
) {
    public static CreateBankAccountResponseDto from(BankAccount bankAccount) {
        return new CreateBankAccountResponseDto(
                bankAccount.getAccountSeq(),
                bankAccount.getBankCode(),
                bankAccount.getAccountNo(),
                bankAccount.getAccountTypeUniqueNo(),
                bankAccount.getIsPrimary(),
                bankAccount.getStatus().name(),
                bankAccount.getLinkedDttm()
        );
    }
}
