package com.ssafy.a305.be.domain.bank.controller;

import com.ssafy.a305.be.domain.auth.security.AuthenticatedUser;
import com.ssafy.a305.be.domain.bank.dto.response.CreateBankAccountResponseDto;
import com.ssafy.a305.be.domain.bank.service.BankAccountService;
import com.ssafy.a305.be.global.exception.CustomBusinessException;
import com.ssafy.a305.be.global.exception.dto.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/bank/accounts")
public class BankAccountController {

    private final BankAccountService bankAccountService;

    @PostMapping
    public ResponseEntity<Map<String, Object>> createBankAccount(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
        Integer userSeq = requireAuthenticatedUser(authenticatedUser).userSeq();
        CreateBankAccountResponseDto responseDto = bankAccountService.createBankAccount(userSeq);
        return ResponseEntity.status(HttpStatus.CREATED).body(successResponse("계좌 생성 성공", responseDto));
    }

    private AuthenticatedUser requireAuthenticatedUser(AuthenticatedUser authenticatedUser) {
        if (authenticatedUser == null) {
            throw new CustomBusinessException(ErrorCode.AUTHENTICATION_REQUIRED);
        }
        return authenticatedUser;
    }

    private Map<String, Object> successResponse(String message, Object data) {
        return Map.of(
                "status", HttpStatus.OK.value(),
                "message", message,
                "data", data
        );
    }
}
