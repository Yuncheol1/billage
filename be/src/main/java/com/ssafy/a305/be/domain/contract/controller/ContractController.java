package com.ssafy.a305.be.domain.contract.controller;

import com.ssafy.a305.be.domain.contract.dto.request.ClauseRequestDto;
import com.ssafy.a305.be.domain.contract.dto.request.ContractDraftRequestDto;
import com.ssafy.a305.be.domain.contract.dto.request.ContractSignatureRequestDto;
import com.ssafy.a305.be.domain.contract.dto.request.ContractUpdateRequestDto;
import com.ssafy.a305.be.domain.contract.dto.response.ContractDetailResponseDto;
import com.ssafy.a305.be.domain.contract.dto.response.ContractDraftResponseDto;
import com.ssafy.a305.be.domain.contract.dto.response.ContractListResponseDto;
import com.ssafy.a305.be.domain.contract.dto.response.ContractSignatureResponseDto;
import com.ssafy.a305.be.domain.contract.service.ContractService;
import com.ssafy.a305.be.domain.contract.service.ContractSignatureService;
import com.ssafy.a305.be.domain.contract.entity.ContractClause;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import com.ssafy.a305.be.domain.auth.security.AuthenticatedUser;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/contracts")
@RequiredArgsConstructor
public class ContractController {
    private final ContractService contractService;
    private final ContractSignatureService contractSignatureService;

    @PostMapping
    public ResponseEntity<Map<String, Object>> createContractDraft(
            @AuthenticationPrincipal AuthenticatedUser authUser,
            @Valid @RequestBody ContractDraftRequestDto requestDto) {

        Integer userSeq = authUser.userSeq();
        ContractDraftResponseDto responseDto = contractService.createDraft(userSeq, requestDto);

        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "status", HttpStatus.CREATED.value(),
                "message", "계약 초안이 성공적으로 생성되었습니다.",
                "data", responseDto));
    }

    @Operation(summary = "AI 특약 자동 생성", description = "계약 정보와 사용자 의도를 바탕으로 AI 서버와 통신하여 법적으로 유효한 특약을 생성하고 계약에 추가합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "특약사항 성공적 저장"),
        @ApiResponse(responseCode = "400", description = "입력값 오류 또는 상태 불일치"),
        @ApiResponse(responseCode = "500", description = "AI 서버 통신 장애")
    })
    @PostMapping("/{contractSeq}/clauses")
    public ResponseEntity<Map<String, Object>> addAiClauses(
            @AuthenticationPrincipal AuthenticatedUser authUser,
            @Parameter(description = "계약 식별자(PK)") @PathVariable Integer contractSeq,
            @Parameter(description = "사용자의 자연어 요구사항(user_intent 전달용)") @Valid @RequestBody ClauseRequestDto requestDto) {

        Integer userSeq = authUser.userSeq();
        ContractClause clause = contractService.addAiClauses(contractSeq, requestDto.getClauseContent(), userSeq);

        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "status", HttpStatus.CREATED.value(),
                "message", "특약사항이 성공적으로 저장되었습니다.",
                "data", Map.of(
                        "clauseSeq", clause.getClauseSeq(),
                        "title", clause.getTitle(),
                        "content", clause.getContent()
                )));
    }

    @PatchMapping("/{contractSeq}/clauses/{clauseSeq}")
    public ResponseEntity<Map<String, Object>> updateClause(
            @AuthenticationPrincipal AuthenticatedUser authUser,
            @PathVariable Integer contractSeq,
            @PathVariable Integer clauseSeq,
            @Valid @RequestBody ClauseRequestDto requestDto) {

        Integer userSeq = authUser.userSeq();
        contractService.updateClause(contractSeq, clauseSeq, requestDto, userSeq);

        return ResponseEntity.ok(Map.of(
                "status", HttpStatus.OK.value(),
                "message", "특약이 성공적으로 수정되었습니다."));
    }

    @PutMapping("/{contractSeq}")
    public ResponseEntity<Map<String, Object>> updateContract(
            @AuthenticationPrincipal AuthenticatedUser authUser,
            @PathVariable Integer contractSeq,
            @Valid @RequestBody ContractUpdateRequestDto requestDto,
            @RequestParam(value = "roomId", required = false) String roomId) {

        Integer userSeq = authUser.userSeq();
        contractService.updateContract(contractSeq, requestDto, userSeq, roomId);

        return ResponseEntity.ok(Map.of(
                "status", HttpStatus.OK.value(),
                "message", "계약이 성공적으로 수정되었습니다."));
    }

    @DeleteMapping("/{contractSeq}/clauses/{clauseSeq}")
    public ResponseEntity<Map<String, Object>> deleteClause(
            @AuthenticationPrincipal AuthenticatedUser authUser,
            @PathVariable Integer contractSeq,
            @PathVariable Integer clauseSeq
          ) {

        Integer userSeq = authUser.userSeq();
        contractService.deleteClause(contractSeq, clauseSeq,userSeq);

        return ResponseEntity.ok(Map.of(
                "status", HttpStatus.OK.value(),
                "message", "특약이 성공적으로 삭제되었습니다."));
    }
    @GetMapping
    public ResponseEntity<Map<String, Object>> getContracts(
            @AuthenticationPrincipal AuthenticatedUser authUser,
            @RequestParam(required = false, defaultValue = "ALL") String role,
            @RequestParam(required = false) String status) {

        Integer userSeq = authUser.userSeq();

        List<ContractListResponseDto> result = contractService.getContracts(userSeq, role, status);

        return ResponseEntity.ok(Map.of(
                "status", HttpStatus.OK.value(),
                "message", "조회 성공",
                "data", result));
    }

    @GetMapping("/{contractSeq}")
    public ResponseEntity<Map<String, Object>> getContractDetail(
            @AuthenticationPrincipal AuthenticatedUser authUser,
            @PathVariable Integer contractSeq) {
        Integer userSeq = authUser.userSeq();
        ContractDetailResponseDto result = contractService.getContractDetail(contractSeq, userSeq);
        return ResponseEntity.ok(Map.of(
                "status", HttpStatus.OK.value(),
                "message", "계약 상세 조회 성공",
                "data", result));
    }

    @DeleteMapping("/{contractSeq}")
    public ResponseEntity<Map<String,Object>>deleteContract(
            @AuthenticationPrincipal AuthenticatedUser authUser,
            @PathVariable Integer contractSeq){
        Integer userSeq = authUser.userSeq();
        contractService.deleteContract(contractSeq,userSeq);
        return ResponseEntity.ok(Map.of(
                "status", HttpStatus.OK.value(),
                "message", "계약이 성공적으로 삭제되었습니다."));
    }

    @PutMapping("/{contractSeq}/accept")
    public ResponseEntity<Map<String, Object>> acceptContract(
            @AuthenticationPrincipal AuthenticatedUser authUser,
            @PathVariable Integer contractSeq,
            @RequestParam("roomId") String roomId) {
        Integer userSeq = authUser.userSeq();
        ContractDetailResponseDto result = contractService.acceptContract(contractSeq, userSeq, roomId);
        return ResponseEntity.ok(Map.of(
                "status", HttpStatus.OK.value(),
                "message", "계약 조건을 수락했습니다.",
                "data", result));
    }

    @PostMapping("/{contractSeq}/signatures")
    public ResponseEntity<Map<String, Object>> submitSignature(
            @AuthenticationPrincipal AuthenticatedUser authUser,
            @PathVariable Integer contractSeq,
            @Valid @RequestBody ContractSignatureRequestDto requestDto,
            HttpServletRequest request) {
        Integer userSeq = authUser.userSeq();
        String clientIp = extractClientIp(request);
        ContractSignatureResponseDto responseDto =
                contractSignatureService.submitSignature(contractSeq, userSeq, requestDto, clientIp);

        String message = responseDto.isContractFullySigned()
                ? "양측 서명이 완료되어 계약이 체결되었습니다."
                : "서명이 완료되었습니다.";

        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "status", HttpStatus.CREATED.value(),
                "message", message,
                "data", responseDto
        ));
    }

    private String extractClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isBlank()) {
            return xRealIp.trim();
        }

        return request.getRemoteAddr();
    }

    @PostMapping("/{contractSeq}/ai-propose-mock")
    public ResponseEntity<Map<String, Object>> createAiProposalMock(
            @AuthenticationPrincipal AuthenticatedUser authUser,
            @PathVariable Integer contractSeq) {

        Integer userSeq = authUser.userSeq();
        contractService.createAiProposalMock(contractSeq, userSeq);

        return ResponseEntity.ok(Map.of(
                "status", HttpStatus.OK.value(),
                "message", "AI 제안서(Mock)가 성공적으로 생성되었습니다."));
    }
}