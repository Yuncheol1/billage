package com.ssafy.a305.be.domain.bank.controller;

import com.ssafy.a305.be.domain.auth.security.AuthenticatedUser;
import com.ssafy.a305.be.domain.bank.dto.response.BankAccountListItemResponseDto;
import com.ssafy.a305.be.domain.bank.dto.response.BankMeResponseDto;
import com.ssafy.a305.be.domain.bank.dto.response.BankTransactionHistoryListResponseDto;
import com.ssafy.a305.be.domain.bank.dto.response.BankTransactionItemResponseDto;
import com.ssafy.a305.be.domain.bank.dto.response.CreateBankAccountResponseDto;
import com.ssafy.a305.be.domain.bank.service.BankAccountService;
import com.ssafy.a305.be.global.exception.CustomBusinessException;
import com.ssafy.a305.be.global.exception.dto.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/bank")
public class BankAccountController {

    private final BankAccountService bankAccountService;

    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> getBankMe(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
        Integer userSeq = requireAuthenticatedUser(authenticatedUser).userSeq();
        BankMeResponseDto data = bankAccountService.getBankMe(userSeq);
        return ResponseEntity.ok(successResponse(HttpStatus.OK, "계좌 연동 여부 조회 성공", data));
    }

    @GetMapping("/accounts")
    public ResponseEntity<Map<String, Object>> listDemandDepositAccounts(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
        Integer userSeq = requireAuthenticatedUser(authenticatedUser).userSeq();
        List<BankAccountListItemResponseDto> list = bankAccountService.listDemandDepositAccounts(userSeq);
        return ResponseEntity.ok(successResponse(HttpStatus.OK, "계좌 목록 조회 성공", list));
    }

    /**
     * 로그인 사용자의 연동(ACTIVE) 계좌 1건에 대해 SSAFY 수시입출금 계좌 단건 조회(inquireDemandDepositAccount) 결과를 반환한다.
     */
    @GetMapping("/accounts/detail")
    public ResponseEntity<Map<String, Object>> getLinkedDemandDepositAccountDetail(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
        Integer userSeq = requireAuthenticatedUser(authenticatedUser).userSeq();
        BankAccountListItemResponseDto data = bankAccountService.getLinkedDemandDepositAccountDetail(userSeq);
        return ResponseEntity.ok(successResponse(HttpStatus.OK, "계좌 단건 조회 성공", data));
    }

    /**
     * 연동(ACTIVE) 계좌 1건 기준 SSAFY 거래 내역 목록(inquireTransactionHistoryList).
     */
    @GetMapping("/accounts/transactions")
    public ResponseEntity<Map<String, Object>> listLinkedAccountTransactions(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @RequestParam String startDate,
            @RequestParam String endDate,
            @RequestParam(required = false) String transactionType,
            @RequestParam(required = false) String orderByType) {
        Integer userSeq = requireAuthenticatedUser(authenticatedUser).userSeq();
        BankTransactionHistoryListResponseDto data = bankAccountService.listLinkedAccountTransactionHistory(
                userSeq, startDate, endDate, transactionType, orderByType);
        return ResponseEntity.ok(successResponse(HttpStatus.OK, "거래 내역 목록 조회 성공", data));
    }

    /**
     * 연동(ACTIVE) 계좌 1건 기준 SSAFY 거래 내역 단건(inquireTransactionHistory).
     */
    @GetMapping("/accounts/transactions/{transactionUniqueNo}")
    public ResponseEntity<Map<String, Object>> getLinkedAccountTransaction(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @PathVariable String transactionUniqueNo) {
        Integer userSeq = requireAuthenticatedUser(authenticatedUser).userSeq();
        BankTransactionItemResponseDto data =
                bankAccountService.getLinkedAccountTransactionHistory(userSeq, transactionUniqueNo);
        return ResponseEntity.ok(successResponse(HttpStatus.OK, "거래 내역 단건 조회 성공", data));
    }

    @PostMapping("/accounts")
    public ResponseEntity<Map<String, Object>> createBankAccount(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
        Integer userSeq = requireAuthenticatedUser(authenticatedUser).userSeq();
        CreateBankAccountResponseDto responseDto = bankAccountService.createBankAccount(userSeq);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(successResponse(HttpStatus.CREATED, "계좌 생성 성공", responseDto));
    }

    private AuthenticatedUser requireAuthenticatedUser(AuthenticatedUser authenticatedUser) {
        if (authenticatedUser == null) {
            throw new CustomBusinessException(ErrorCode.AUTHENTICATION_REQUIRED);
        }
        return authenticatedUser;
    }

    private Map<String, Object> successResponse(HttpStatus status, String message, Object data) {
        return Map.of(
                "status", status.value(),
                "message", message,
                "data", data
        );
    }
}
