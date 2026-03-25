package com.ssafy.a305.be.domain.bank.service;

import com.ssafy.a305.be.domain.account.entity.BankAccount;
import com.ssafy.a305.be.domain.account.repository.BankAccountRepository;
import com.ssafy.a305.be.domain.auth.config.SsafyApiProperties;
import com.ssafy.a305.be.domain.bank.client.SsafyDemandDepositClient;
import com.ssafy.a305.be.domain.bank.client.dto.response.SsafyCreateDemandDepositAccountResponseDto;
import com.ssafy.a305.be.domain.bank.client.dto.response.SsafyDemandDepositAccountRecDto;
import com.ssafy.a305.be.domain.bank.dto.response.BankAccountListItemResponseDto;
import com.ssafy.a305.be.domain.bank.dto.response.BankMeResponseDto;
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
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BankAccountService {

    private static final String PROVIDER_SSAFY = "SSAFY";

    private final UserRepository userRepository;
    private final BankAccountRepository bankAccountRepository;
    private final SsafyDemandDepositClient ssafyDemandDepositClient;
    private final SsafyApiProperties ssafyApiProperties;

    @Transactional(readOnly = true)
    public BankMeResponseDto getBankMe(Integer userSeq) {
        if (!userRepository.existsById(userSeq)) {
            throw new CustomBusinessException(ErrorCode.USER_NOT_FOUND);
        }
        boolean hasBankAccount = bankAccountRepository.existsByUserUserSeqAndStatus(userSeq, BankAccountStatus.ACTIVE);
        return new BankMeResponseDto(hasBankAccount);
    }

    @Transactional(readOnly = true)
    public List<BankAccountListItemResponseDto> listDemandDepositAccounts(Integer userSeq) {
        User user = userRepository.findById(userSeq)
                .orElseThrow(() -> new CustomBusinessException(ErrorCode.USER_NOT_FOUND));

        if (!hasText(user.getSsafyUserKey())) {
            throw new CustomBusinessException(ErrorCode.INVALID_INPUT_VALUE, "SSAFY userKey가 없어 계좌 목록을 조회할 수 없습니다.");
        }

        Map<String, BankAccount> linkedByAccountNo = bankAccountRepository.findByUserUserSeq(userSeq).stream()
                .filter(ba -> hasText(ba.getAccountNo()))
                .collect(Collectors.toMap(BankAccount::getAccountNo, Function.identity(), (a, b) -> a));

        return ssafyDemandDepositClient.inquireDemandDepositAccountList(user.getSsafyUserKey()).stream()
                .map(rec -> toListItem(rec, linkedByAccountNo.get(rec.accountNo())))
                .toList();
    }

    /**
     * 연동된 ACTIVE 계좌 1건을 기준으로 SSAFY 계좌 단건 조회 API를 호출한다.
     */
    @Transactional(readOnly = true)
    public BankAccountListItemResponseDto getLinkedDemandDepositAccountDetail(Integer userSeq) {
        User user = userRepository.findById(userSeq)
                .orElseThrow(() -> new CustomBusinessException(ErrorCode.USER_NOT_FOUND));

        if (!hasText(user.getSsafyUserKey())) {
            throw new CustomBusinessException(ErrorCode.INVALID_INPUT_VALUE, "SSAFY userKey가 없어 계좌를 조회할 수 없습니다.");
        }

        BankAccount account = bankAccountRepository
                .findFirstByUserUserSeqAndStatusOrderByAccountSeqAsc(userSeq, BankAccountStatus.ACTIVE)
                .orElseThrow(() -> new CustomBusinessException(ErrorCode.BANK_ACCOUNT_NOT_FOUND));

        if (!hasText(account.getAccountNo())) {
            throw new CustomBusinessException(ErrorCode.INVALID_INPUT_VALUE, "계좌번호가 없어 계좌를 조회할 수 없습니다.");
        }

        SsafyDemandDepositAccountRecDto rec = ssafyDemandDepositClient.inquireDemandDepositAccount(
                user.getSsafyUserKey(),
                account.getAccountNo()
        );

        if (!account.getAccountNo().equals(rec.accountNo())) {
            throw new CustomBusinessException(ErrorCode.INVALID_INPUT_VALUE, "SSAFY 계좌 단건 조회 결과 계좌번호가 일치하지 않습니다.");
        }

        return toListItem(rec, account);
    }

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

        String productUniqueNo = ssafyApiProperties.getDemandDepositAccountTypeUniqueNo();
        if (bankAccountRepository.existsByUserUserSeqAndAccountTypeUniqueNoAndStatus(
                userSeq, productUniqueNo, BankAccountStatus.ACTIVE)) {
            throw new CustomBusinessException(ErrorCode.BANK_ACCOUNT_PRODUCT_ALREADY_ACTIVE);
        }

        SsafyCreateDemandDepositAccountResponseDto.Rec rec = ssafyDemandDepositClient.createDemandDepositAccount(
                user.getSsafyUserKey(),
                productUniqueNo
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
                    .accountTypeUniqueNo(productUniqueNo)
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

    private BankAccountListItemResponseDto toListItem(SsafyDemandDepositAccountRecDto rec, BankAccount linked) {
        Integer accountSeq = linked != null ? linked.getAccountSeq() : null;
        boolean isPrimary = linked != null && Boolean.TRUE.equals(linked.getIsPrimary());

        return new BankAccountListItemResponseDto(
                rec.bankCode(),
                rec.bankName(),
                rec.userName(),
                rec.accountNo(),
                rec.accountName(),
                rec.accountTypeCode(),
                rec.accountTypeName(),
                rec.accountCreatedDate(),
                rec.accountExpiryDate(),
                rec.dailyTransferLimit(),
                rec.oneTimeTransferLimit(),
                rec.accountBalance(),
                rec.lastTransactionDate(),
                rec.currency(),
                accountSeq,
                isPrimary
        );
    }

    private String maskAccountNo(String accountNo) {
        if (!hasText(accountNo)) {
            return accountNo;
        }
        int length = accountNo.length();
        if (length <= 8) {
            return accountNo;
        }

        if (length == 16) {
            return "%s-****-****-%s".formatted(
                    accountNo.substring(0, 4),
                    accountNo.substring(12, 16)
            );
        }

        String prefix = accountNo.substring(0, 4);
        String suffix = accountNo.substring(length - 4);
        return "%s-****-%s".formatted(prefix, suffix);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
