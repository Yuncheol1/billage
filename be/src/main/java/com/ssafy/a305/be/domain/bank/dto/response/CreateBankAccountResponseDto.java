package com.ssafy.a305.be.domain.bank.dto.response;

import com.ssafy.a305.be.domain.account.entity.BankAccount;

import java.time.LocalDateTime;

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
