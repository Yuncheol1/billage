package com.ssafy.a305.be.domain.bank.service;

import com.ssafy.a305.be.domain.account.entity.BankAccount;
import com.ssafy.a305.be.domain.account.repository.BankAccountRepository;
import com.ssafy.a305.be.domain.auth.config.SsafyApiProperties;
import com.ssafy.a305.be.domain.bank.client.SsafyDemandDepositClient;
import com.ssafy.a305.be.domain.bank.client.dto.SsafyCreateDemandDepositAccountResponseDto;
import com.ssafy.a305.be.domain.bank.dto.response.CreateBankAccountResponseDto;
import com.ssafy.a305.be.domain.enums.BankAccountStatus;
import com.ssafy.a305.be.domain.user.entity.User;
import com.ssafy.a305.be.domain.user.repository.UserRepository;
import com.ssafy.a305.be.global.exception.CustomBusinessException;
import com.ssafy.a305.be.global.exception.dto.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class BankAccountService {

    private static final String PROVIDER_SSAFY = "SSAFY";

    private final UserRepository userRepository;
    private final BankAccountRepository bankAccountRepository;
    private final SsafyDemandDepositClient ssafyDemandDepositClient;
    private final SsafyApiProperties ssafyApiProperties;

    @Transactional
    public CreateBankAccountResponseDto createBankAccount(Integer userSeq) {
        User user = userRepository.findById(userSeq)
                .orElseThrow(() -> new CustomBusinessException(ErrorCode.USER_NOT_FOUND));

        if (!hasText(user.getSsafyUserKey())) {
            throw new CustomBusinessException(ErrorCode.INVALID_INPUT_VALUE, "SSAFY userKey가 없어 계좌를 생성할 수 없습니다.");
        }
        if (!hasText(ssafyApiProperties.getDemandDepositAccountTypeUniqueNo())) {
            throw new CustomBusinessException(ErrorCode.AUTH_CONFIG_INVALID, "SSAFY_DEMAND_DEPOSIT_ACCOUNT_TYPE_UNIQUE_NO is required.");
        }

        SsafyCreateDemandDepositAccountResponseDto.Rec rec = ssafyDemandDepositClient.createDemandDepositAccount(
                user.getSsafyUserKey(),
                ssafyApiProperties.getDemandDepositAccountTypeUniqueNo()
        );

        if (bankAccountRepository.existsByAccountNo(rec.accountNo())) {
            throw new CustomBusinessException(ErrorCode.INVALID_INPUT_VALUE, "이미 등록된 계좌번호입니다.");
        }

        boolean isPrimary = !bankAccountRepository.existsByUserUserSeqAndStatus(userSeq, BankAccountStatus.ACTIVE);

        BankAccount bankAccount;
        try {
            bankAccount = bankAccountRepository.save(BankAccount.builder()
                    .user(user)
                    .provider(PROVIDER_SSAFY)
                    .bankCode(rec.bankCode())
                    .accountTypeUniqueNo(ssafyApiProperties.getDemandDepositAccountTypeUniqueNo())
                    .accountNo(rec.accountNo())
                    .accountNoMasked(maskAccountNo(rec.accountNo()))
                    .isPrimary(isPrimary)
                    .linkedDttm(LocalDateTime.now())
                    .status(BankAccountStatus.ACTIVE)
                    .build());
        } catch (DataIntegrityViolationException e) {
            throw new CustomBusinessException(ErrorCode.INVALID_INPUT_VALUE, "이미 등록된 계좌번호입니다.");
        }

        return CreateBankAccountResponseDto.from(bankAccount);
    }

    private String maskAccountNo(String accountNo) {
        if (!hasText(accountNo)) {
            return accountNo;
        }
        int length = accountNo.length();
        if (length <= 8) {
            return accountNo;
        }

        String prefix = accountNo.substring(0, 4);
        String suffix = accountNo.substring(length - 4);
        return "%s-****-%s".formatted(prefix, suffix);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
