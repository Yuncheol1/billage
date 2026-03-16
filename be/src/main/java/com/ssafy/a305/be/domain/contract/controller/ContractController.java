package com.ssafy.a305.be.domain.contract.controller;

import com.ssafy.a305.be.domain.contract.dto.request.ContractDraftRequestDto;
import com.ssafy.a305.be.domain.contract.dto.response.ContractDraftResponseDto;
import com.ssafy.a305.be.domain.contract.service.ContractService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/contracts")
@RequiredArgsConstructor
public class ContractController {

    private final ContractService contractService;

    @PostMapping("/draft")
    public ResponseEntity<Map<String, Object>> createContractDraft(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            @Valid @RequestBody ContractDraftRequestDto requestDto) {

        // 실제 환경에서는 SecurityContext나 인터셉터/ArgumentResolver 등에서 
        // JWT 검증 후 회원 PK(User Seq)를 꺼내옵니다.
        // 현재는 구현 예시로 임의의 creditorUserSeq(1L)를 주입한다고 가정합니다.
        Integer creditorUserSeq = 1; 

        ContractDraftResponseDto responseDto = contractService.createDraft(creditorUserSeq, requestDto);

        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "status", HttpStatus.CREATED.value(),
                "message", "계약 초안이 성공적으로 생성되었습니다.",
                "data", responseDto
        ));
    }
}
