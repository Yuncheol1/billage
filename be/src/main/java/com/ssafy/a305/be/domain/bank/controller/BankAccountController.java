package com.ssafy.a305.be.domain.bank.controller;

import com.ssafy.a305.be.domain.auth.security.AuthenticatedUser;
import com.ssafy.a305.be.domain.bank.dto.response.BankAccountListItemResponseDto;
import com.ssafy.a305.be.domain.bank.dto.response.BankMeResponseDto;
import com.ssafy.a305.be.domain.bank.dto.response.CreateBankAccountResponseDto;
import com.ssafy.a305.be.domain.bank.service.BankAccountService;
import com.ssafy.a305.be.global.exception.CustomBusinessException;
import com.ssafy.a305.be.global.exception.dto.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
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
