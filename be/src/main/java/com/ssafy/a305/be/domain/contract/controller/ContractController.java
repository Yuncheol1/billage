package com.ssafy.a305.be.domain.contract.controller;

import com.ssafy.a305.be.domain.contract.dto.request.ClauseRequestDto;
import com.ssafy.a305.be.domain.contract.dto.request.ContractDraftRequestDto;
import com.ssafy.a305.be.domain.contract.dto.response.ContractDetailResponseDto;
import com.ssafy.a305.be.domain.contract.dto.response.ContractDraftResponseDto;
import com.ssafy.a305.be.domain.contract.dto.response.ContractListResponseDto;
import com.ssafy.a305.be.domain.contract.service.ContractService;
import jakarta.validation.Valid;

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

    @PostMapping("/{contractSeq}/clauses")
    public ResponseEntity<Map<String, Object>> addAiClauses(
            @AuthenticationPrincipal AuthenticatedUser authUser,
            @PathVariable Integer contractSeq,
            @Valid @RequestBody ClauseRequestDto requestDto) {

        Integer userSeq = authUser.userSeq();
        contractService.addAiClauses(contractSeq, requestDto.getClauseContent(), userSeq);

        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "status", HttpStatus.CREATED.value(),
                "message", "특약사항이 성공적으로 저장되었습니다."));
    }

    @PatchMapping("/{contractSeq}/clauses/{clauseSeq}")
    public ResponseEntity<Map<String, Object>> updateClause(
            @PathVariable Integer contractSeq,
            @PathVariable Integer clauseSeq,
            @Valid @RequestBody ClauseRequestDto requestDto) {

        contractService.updateClause(contractSeq, clauseSeq, requestDto, tempUserSeq);

        return ResponseEntity.ok(Map.of(
                "status", HttpStatus.OK.value(),
                "message", "특약이 성공적으로 수정되었습니다."));
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

}
