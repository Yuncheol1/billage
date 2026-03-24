package com.ssafy.a305.be.domain.bank.dto.response;

import com.ssafy.a305.be.domain.account.entity.BankAccount;

import java.time.LocalDateTime;

/** 우리 서버의 계좌 생성 API 응답 DTO */
public record CreateBankAccountResponseDto(
        Integer accountSeq,
        String bankCode,
        String accountNoMasked,
        String accountTypeUniqueNo,
        Boolean isPrimary,
        String status,
        LocalDateTime linkedDttm
) {
    public static CreateBankAccountResponseDto from(BankAccount bankAccount) {
        return new CreateBankAccountResponseDto(
                bankAccount.getAccountSeq(),
                bankAccount.getBankCode(),
                bankAccount.getAccountNoMasked(),
                bankAccount.getAccountTypeUniqueNo(),
                bankAccount.getIsPrimary(),
                bankAccount.getStatus().name(),
                bankAccount.getLinkedDttm()
        );
    }
}
