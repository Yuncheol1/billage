package com.ssafy.a305.be.domain.bank.service;

import com.ssafy.a305.be.domain.account.entity.BankAccount;
import com.ssafy.a305.be.domain.account.repository.BankAccountRepository;
import com.ssafy.a305.be.domain.auth.config.SsafyApiProperties;
import com.ssafy.a305.be.domain.bank.config.BankDepositAllowlistProperties;
import com.ssafy.a305.be.domain.bank.client.SsafyDemandDepositClient;
import com.ssafy.a305.be.domain.bank.client.dto.response.SsafyCreateDemandDepositAccountResponseDto;
import com.ssafy.a305.be.domain.bank.client.dto.response.SsafyDemandDepositAccountRecDto;
import com.ssafy.a305.be.domain.bank.client.dto.response.SsafyDemandDepositTransactionRecDto;
import com.ssafy.a305.be.domain.bank.client.dto.response.SsafyInquireTransactionHistoryListRecDto;
import com.ssafy.a305.be.domain.bank.client.dto.response.SsafyUpdateDemandDepositAccountDepositResponseDto;
import com.ssafy.a305.be.domain.bank.dto.request.BankAccountDepositRequestDto;
import com.ssafy.a305.be.domain.bank.dto.response.BankAccountListItemResponseDto;
import com.ssafy.a305.be.domain.bank.dto.response.BankAccountDetailResponseDto;
import com.ssafy.a305.be.domain.bank.dto.response.BankAccountDepositResponseDto;
import com.ssafy.a305.be.domain.bank.dto.response.BankTransactionHistoryListResponseDto;
import com.ssafy.a305.be.domain.bank.dto.response.BankTransactionItemResponseDto;
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

import java.util.Arrays;
import java.util.Collections;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BankAccountService {

    private static final String PROVIDER_SSAFY = "SSAFY";
    private static final Pattern YYYYMMDD_PATTERN = Pattern.compile("\\d{8}");
    private static final Pattern MONEY_PATTERN = Pattern.compile("\\d+");
    private static final String DEPOSIT_SUMMARY = "(수시입출금) : 입금";

    private final UserRepository userRepository;
    private final BankAccountRepository bankAccountRepository;
    private final SsafyDemandDepositClient ssafyDemandDepositClient;
    private final SsafyApiProperties ssafyApiProperties;
    private final BankDepositAllowlistProperties bankDepositAllowlistProperties;

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
    public BankAccountDetailResponseDto getLinkedDemandDepositAccountDetail(Integer userSeq) {
        SsafyLinkedAccount ctx = requireSsafyLinkedActiveAccount(userSeq);

        SsafyDemandDepositAccountRecDto rec = ssafyDemandDepositClient.inquireDemandDepositAccount(
                ctx.user().getSsafyUserKey(),
                ctx.bankAccount().getAccountNo()
        );

        if (!ctx.bankAccount().getAccountNo().equals(rec.accountNo())) {
            throw new CustomBusinessException(ErrorCode.INVALID_INPUT_VALUE, "SSAFY 계좌 단건 조회 결과 계좌번호가 일치하지 않습니다.");
        }

        return new BankAccountDetailResponseDto(
                rec.bankName(),
                rec.accountTypeName(),
                ctx.bankAccount().getAccountNo(),
                rec.accountBalance()
        );
    }

    @Transactional(readOnly = true)
    public BankTransactionHistoryListResponseDto listLinkedAccountTransactionHistory(
            Integer userSeq,
            String startDate,
            String endDate,
            String transactionType,
            String orderByType
    ) {
        if (!hasText(startDate) || !hasText(endDate)) {
            throw new CustomBusinessException(ErrorCode.INVALID_INPUT_VALUE, "조회 시작일·종료일(startDate, endDate)은 필수입니다.");
        }
        validateYyyyMmDd(startDate, "startDate");
        validateYyyyMmDd(endDate, "endDate");

        String resolvedTransactionType = resolveTransactionType(transactionType);
        String resolvedOrderByType = resolveOrderByType(orderByType);

        SsafyLinkedAccount ctx = requireSsafyLinkedActiveAccount(userSeq);

        SsafyInquireTransactionHistoryListRecDto rec = ssafyDemandDepositClient.inquireTransactionHistoryList(
                ctx.user().getSsafyUserKey(),
                ctx.bankAccount().getAccountNo(),
                startDate,
                endDate,
                resolvedTransactionType,
                resolvedOrderByType
        );

        String totalCount = rec != null && hasText(rec.totalCount()) ? rec.totalCount() : "0";
        List<SsafyDemandDepositTransactionRecDto> rows =
                rec != null && rec.list() != null ? rec.list() : List.of();

        return new BankTransactionHistoryListResponseDto(
                totalCount,
                rows.stream().map(this::toTransactionItem).toList()
        );
    }

    @Transactional(readOnly = true)
    public BankTransactionItemResponseDto getLinkedAccountTransactionHistory(
            Integer userSeq,
            String transactionUniqueNo
    ) {
        if (!hasText(transactionUniqueNo)) {
            throw new CustomBusinessException(ErrorCode.INVALID_INPUT_VALUE, "거래고유번호(transactionUniqueNo)가 없어 거래 내역을 조회할 수 없습니다.");
        }

        SsafyLinkedAccount ctx = requireSsafyLinkedActiveAccount(userSeq);

        SsafyDemandDepositTransactionRecDto rec = ssafyDemandDepositClient.inquireTransactionHistory(
                ctx.user().getSsafyUserKey(),
                ctx.bankAccount().getAccountNo(),
                transactionUniqueNo
        );

        if (!transactionUniqueNo.equals(rec.transactionUniqueNo())) {
            throw new CustomBusinessException(ErrorCode.INVALID_INPUT_VALUE, "SSAFY 거래 내역 조회 결과 거래고유번호가 일치하지 않습니다.");
        }

        return toTransactionItem(rec);
    }

    @Transactional
    public BankAccountDepositResponseDto depositToLinkedAccount(
            Integer userSeq,
            BankAccountDepositRequestDto request
    ) {
        String transactionBalance = request == null ? null : request.transactionBalance();
        String requestAccountNo = request == null ? null : request.accountNo();
        validateDepositAmount(transactionBalance);

        String targetAccountNo = hasText(requestAccountNo)
                ? requestAccountNo
                : requireSsafyLinkedActiveAccount(userSeq).bankAccount().getAccountNo();

        // allowlist에 포함된 계좌만 "지정 입금"을 허용한다.
        if (hasText(requestAccountNo) && !isDepositAllowlisted(targetAccountNo)) {
            throw new CustomBusinessException(ErrorCode.INVALID_INPUT_VALUE, "허용되지 않은 입금 대상 계좌입니다.");
        }

        BankAccount targetBankAccount = bankAccountRepository
                .findByAccountNo(targetAccountNo)
                .orElseThrow(() -> new CustomBusinessException(ErrorCode.BANK_ACCOUNT_NOT_FOUND));

        if (targetBankAccount.getStatus() != BankAccountStatus.ACTIVE) {
            throw new CustomBusinessException(ErrorCode.BANK_ACCOUNT_NOT_FOUND);
        }
        if (targetBankAccount.getUser() == null || !hasText(targetBankAccount.getUser().getSsafyUserKey())) {
            throw new CustomBusinessException(ErrorCode.INVALID_INPUT_VALUE, "SSAFY userKey가 없어 입금할 수 없습니다.");
        }

        SsafyUpdateDemandDepositAccountDepositResponseDto.Rec rec =
                ssafyDemandDepositClient.updateDemandDepositAccountDeposit(
                        targetBankAccount.getUser().getSsafyUserKey(),
                        targetBankAccount.getAccountNo(),
                        transactionBalance,
                        DEPOSIT_SUMMARY
                );

        return new BankAccountDepositResponseDto(
                rec.transactionUniqueNo(),
                rec.transactionDate()
        );
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

    private SsafyLinkedAccount requireSsafyLinkedActiveAccount(Integer userSeq) {
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

        return new SsafyLinkedAccount(user, account);
    }

    private BankTransactionItemResponseDto toTransactionItem(SsafyDemandDepositTransactionRecDto rec) {
        return new BankTransactionItemResponseDto(
                rec.transactionUniqueNo(),
                rec.transactionDate(),
                rec.transactionTime(),
                rec.transactionType(),
                rec.transactionTypeName(),
                rec.transactionAccountNo(),
                rec.transactionBalance(),
                rec.transactionAfterBalance(),
                rec.transactionSummary(),
                rec.transactionMemo()
        );
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

    private void validateYyyyMmDd(String value, String fieldName) {
        if (!YYYYMMDD_PATTERN.matcher(value).matches()) {
            throw new CustomBusinessException(
                    ErrorCode.INVALID_INPUT_VALUE,
                    "%s는 yyyyMMdd 형식이어야 합니다.".formatted(fieldName)
            );
        }
    }

    private String resolveTransactionType(String value) {
        String normalized = hasText(value) ? value.trim().toUpperCase() : "A";
        if (!"A".equals(normalized) && !"1".equals(normalized) && !"2".equals(normalized)) {
            throw new CustomBusinessException(
                    ErrorCode.INVALID_INPUT_VALUE,
                    "transactionType은 A, 1, 2 중 하나여야 합니다."
            );
        }
        return normalized;
    }

    private String resolveOrderByType(String value) {
        String normalized = hasText(value) ? value.trim().toUpperCase() : "ASC";
        if (!"ASC".equals(normalized) && !"DESC".equals(normalized)) {
            throw new CustomBusinessException(
                    ErrorCode.INVALID_INPUT_VALUE,
                    "orderByType은 ASC 또는 DESC여야 합니다."
            );
        }
        return normalized;
    }

    private void validateDepositAmount(String value) {
        if (!hasText(value)) {
            throw new CustomBusinessException(ErrorCode.INVALID_INPUT_VALUE, "입금 금액(transactionBalance)은 필수입니다.");
        }
        String normalized = value.trim();
        if (!MONEY_PATTERN.matcher(normalized).matches()) {
            throw new CustomBusinessException(ErrorCode.INVALID_INPUT_VALUE, "입금 금액은 숫자만 입력할 수 있습니다.");
        }
        if ("0".equals(normalized)) {
            throw new CustomBusinessException(ErrorCode.INVALID_INPUT_VALUE, "입금 금액은 0보다 커야 합니다.");
        }
    }

    private boolean isDepositAllowlisted(String accountNo) {
        Set<String> allowlist = resolveDepositAllowlistAccountNos();
        return allowlist.contains(accountNo);
    }

    private Set<String> resolveDepositAllowlistAccountNos() {
        String raw = bankDepositAllowlistProperties.getDepositAllowlistAccountNos();
        if (!hasText(raw)) {
            return Collections.emptySet();
        }
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(this::hasText)
                .collect(Collectors.toSet());
    }

    private record SsafyLinkedAccount(User user, BankAccount bankAccount) {
    }
}
