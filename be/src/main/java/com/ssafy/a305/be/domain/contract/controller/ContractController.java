package com.ssafy.a305.be.domain.contract.controller;

import com.ssafy.a305.be.domain.contract.dto.request.ContractDraftRequestDto;
import com.ssafy.a305.be.domain.contract.dto.response.ContractDraftResponseDto;
import com.ssafy.a305.be.domain.contract.dto.response.ContractListResponseDto;
import com.ssafy.a305.be.domain.contract.service.ContractService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/contracts")
@RequiredArgsConstructor
public class ContractController {

    private final ContractService contractService;
    @PostMapping
    public ResponseEntity<Map<String, Object>> createContractDraft(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            @Valid @RequestBody ContractDraftRequestDto requestDto) {

        // 임시 하드코딩
        Integer creditorUserSeq = 1;

        ContractDraftResponseDto responseDto = contractService.createDraft(creditorUserSeq, requestDto);

        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "status", HttpStatus.CREATED.value(),
                "message", "계약 초안이 성공적으로 생성되었습니다.",
                "data", responseDto
        ));
    }
}
