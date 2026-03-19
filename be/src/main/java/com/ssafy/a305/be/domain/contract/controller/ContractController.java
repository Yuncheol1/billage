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
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/contracts")
@RequiredArgsConstructor
public class ContractController {
    private final Integer tempUserSeq = 100;// 임시 하드코딩
    private final ContractService contractService;

    @PostMapping
    public ResponseEntity<Map<String, Object>> createContractDraft(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            @Valid @RequestBody ContractDraftRequestDto requestDto) {

        ContractDraftResponseDto responseDto = contractService.createDraft(tempUserSeq, requestDto);

        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "status", HttpStatus.CREATED.value(),
                "message", "계약 초안이 성공적으로 생성되었습니다.",
                "data", responseDto));
    }

    @PostMapping("/{contractSeq}/clauses")
    public ResponseEntity<Map<String, Object>> addAiClauses(
            @PathVariable Integer contractSeq,
            @Valid @RequestBody ClauseRequestDto requestDto) {

        contractService.addAiClauses(contractSeq, requestDto.getClauseContent(), tempUserSeq);

        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "status", HttpStatus.CREATED.value(),
                "message", "특약사항이 성공적으로 저장되었습니다."));
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getContracts(
            @RequestParam(required = false, defaultValue = "ALL") String role,
            @RequestParam(required = false) String status) {

        Integer userSeq = tempUserSeq;

        List<ContractListResponseDto> result = contractService.getContracts(userSeq, role, status);

        return ResponseEntity.ok(Map.of(
                "status", HttpStatus.OK.value(),
                "message", "조회 성공",
                "data", result));
    }
    @GetMapping("/{contractSeq}")
    public ResponseEntity<Map<String, Object>> getContractDetail(
            @PathVariable Integer contractSeq) {
        Integer userSeq = tempUserSeq; // Controller 상단에 이미 선언된 임시 유저 사용
        ContractDetailResponseDto result = contractService.getContractDetail(contractSeq, userSeq);
        return ResponseEntity.ok(Map.of(
                "status", HttpStatus.OK.value(),
                "message", "계약 상세 조회 성공",
                "data", result));
    }
}
