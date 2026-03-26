package com.ssafy.a305.be.domain.repayment.controller;

import com.ssafy.a305.be.domain.auth.security.AuthenticatedUser;
import com.ssafy.a305.be.domain.repayment.dto.UpcomingScheduleResDto;
import com.ssafy.a305.be.domain.repayment.dto.response.RepaymentSchedulePaidResponseDto;
import com.ssafy.a305.be.domain.repayment.dto.response.RepaymentScheduleTransferResponseDto;
import com.ssafy.a305.be.domain.repayment.service.RepaymentManualTransferService;
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
import org.springframework.web.bind.annotation.RestController;
import com.ssafy.a305.be.domain.repayment.dto.RepaymentScheduleListResDto;
import com.ssafy.a305.be.domain.repayment.service.RepaymentScheduleService;
import lombok.RequiredArgsConstructor;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;
import java.util.Map;

@Tag(name = "상환 스케줄 API", description = "계약에 해당하는 상환 스케줄을 처리하는 API")
@RestController
@RequestMapping("/api/v1/repayment")
@RequiredArgsConstructor
@RequestMapping("/api/v1/contracts/{contractSeq}/repayment-schedules")
public class RepaymentScheduleController {
    private final RepaymentScheduleService repaymentScheduleService;

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
    @Operation(summary = "상환 스케줄 전체 조회", description = "로그인한 사용자가 참여 중인 특정 계약의 전체 상환 스케줄을 오름차순으로 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "상환 스케줄 전체 조회 성공"),
            @ApiResponse(responseCode = "403", description = "해당 계약의 당사자가 아님"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 계약")
    })
    @GetMapping("/{contractSeq}/schedules")
    public ResponseEntity<Map<String, Object>> getRepaymentSchedules(
            @AuthenticationPrincipal AuthenticatedUser authUser,
            @Parameter(description = "계약 식별자(PK)") @PathVariable Integer contractSeq) {
        Integer userSeq = authUser.userSeq();
        RepaymentScheduleListResDto result = repaymentScheduleService.getRepaymentSchedules(contractSeq, userSeq);
        return ResponseEntity.ok(Map.of(
                "status", HttpStatus.OK.value(),
                "message", "상환 스케줄 전체 조회 성공",
                "data", result
        ));
    }

    @Operation(summary = "다가오는 상환 일정 상위 3개 조회", description = "로그인한 사용자가 참여 중인 ACTIVE 상태의 계약 중 가장 기한이 임박한 일정 3개를 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공")
    })
    @GetMapping("/schedules/upcoming")
    public ResponseEntity<Map<String, Object>> getUpcomingSchedules(
            @AuthenticationPrincipal AuthenticatedUser authUser) {
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
        Integer userSeq = authUser.userSeq();
        List<UpcomingScheduleResDto> result = repaymentScheduleService.getUpcomingSchedules(userSeq);

        return ResponseEntity.ok(successResponse(HttpStatus.OK, "상환 회차 수동 완료 처리 성공", data));
        return ResponseEntity.ok(Map.of(
                "status", HttpStatus.OK.value(),
                "message", "다가오는 상환 일정 조회 성공",
                "data", result
        ));
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


}