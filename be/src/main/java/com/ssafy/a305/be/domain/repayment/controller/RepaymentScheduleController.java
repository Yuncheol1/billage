package com.ssafy.a305.be.domain.repayment.controller;

import com.ssafy.a305.be.domain.auth.security.AuthenticatedUser;
import com.ssafy.a305.be.domain.repayment.dto.response.RepaymentSchedulePaidResponseDto;
import com.ssafy.a305.be.domain.repayment.dto.response.RepaymentScheduleTransferResponseDto;
import com.ssafy.a305.be.domain.repayment.service.RepaymentManualTransferService;
import com.ssafy.a305.be.global.exception.CustomBusinessException;
import com.ssafy.a305.be.global.exception.dto.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/contracts/{contractSeq}/repayment-schedules")
public class RepaymentScheduleController {

    private final RepaymentManualTransferService repaymentManualTransferService;

    @PostMapping("/{scheduleSeq}/transfer")
    public ResponseEntity<Map<String, Object>> transferRepaymentSchedule(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @PathVariable Integer contractSeq,
            @PathVariable Integer scheduleSeq
    ) {
        Integer userSeq = requireAuthenticatedUser(authenticatedUser).userSeq();

        RepaymentScheduleTransferResponseDto data = repaymentManualTransferService.transferRepaymentSchedule(
                contractSeq,
                scheduleSeq,
                userSeq
        );

        return ResponseEntity.ok(successResponse(HttpStatus.OK, "상환 회차 수동 이체 성공", data));
    }

    @PostMapping("/{scheduleSeq}/paid")
    public ResponseEntity<Map<String, Object>> markRepaymentSchedulePaid(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @PathVariable Integer contractSeq,
            @PathVariable Integer scheduleSeq
    ) {
        Integer userSeq = requireAuthenticatedUser(authenticatedUser).userSeq();

        RepaymentSchedulePaidResponseDto data = repaymentManualTransferService.markRepaymentScheduleManualPaid(
                contractSeq,
                scheduleSeq,
                userSeq
        );

        return ResponseEntity.ok(successResponse(HttpStatus.OK, "상환 회차 수동 완료 처리 성공", data));
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

