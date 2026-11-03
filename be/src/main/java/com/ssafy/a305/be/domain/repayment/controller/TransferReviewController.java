package com.ssafy.a305.be.domain.repayment.controller;

import com.ssafy.a305.be.domain.repayment.dto.request.TransferReviewRequestDto;
import com.ssafy.a305.be.domain.repayment.dto.response.UnresolvedTransferResponseDto;
import com.ssafy.a305.be.domain.repayment.service.TransferReconciliationService;
import com.ssafy.a305.be.global.exception.CustomBusinessException;
import com.ssafy.a305.be.global.exception.dto.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 미해결 이체 운영자 확인 API. JWT 대신 X-Ops-API-Key로 인증하고, 키가 없으면 전부 거절한다.
 */
@RestController
@RequestMapping("/api/v1/ops/transfers")
public class TransferReviewController {

    private static final String OPS_KEY_HEADER = "X-Ops-API-Key";

    private final TransferReconciliationService transferReconciliationService;
    private final byte[] opsApiKey;

    public TransferReviewController(TransferReconciliationService transferReconciliationService,
                                    @Value("${ops.api-key:}") String opsApiKey) {
        this.transferReconciliationService = transferReconciliationService;
        this.opsApiKey = opsApiKey.getBytes(StandardCharsets.UTF_8);
    }

    @GetMapping("/unresolved")
    public ResponseEntity<Map<String, Object>> findUnresolved(
            @RequestHeader(value = OPS_KEY_HEADER, required = false) String apiKey
    ) {
        requireOpsKey(apiKey);
        List<UnresolvedTransferResponseDto> data = transferReconciliationService.findUnresolved(LocalDateTime.now());
        return ResponseEntity.ok(successResponse("미확정 이체 조회 성공", data));
    }

    @PostMapping("/{scheduleSeq}/review")
    public ResponseEntity<Map<String, Object>> review(
            @RequestHeader(value = OPS_KEY_HEADER, required = false) String apiKey,
            @PathVariable Integer scheduleSeq,
            @RequestBody TransferReviewRequestDto request
    ) {
        requireOpsKey(apiKey);
        if (request == null || request.withdrawn() == null) {
            throw new CustomBusinessException(ErrorCode.INVALID_INPUT_VALUE, "출금 확인 여부(withdrawn)를 입력해야 합니다.");
        }
        TransferReconciliationService.Result result = transferReconciliationService.resolveByReview(
                scheduleSeq, request.withdrawn(), request.note(), LocalDateTime.now());
        return ResponseEntity.ok(successResponse("이체 결과 확인 처리 성공",
                Map.of("scheduleSeq", scheduleSeq, "result", result.name())));
    }

    // 타이밍 공격을 막기 위해 상수 시간 비교
    private void requireOpsKey(String apiKey) {
        if (opsApiKey.length == 0 || apiKey == null
                || !MessageDigest.isEqual(opsApiKey, apiKey.getBytes(StandardCharsets.UTF_8))) {
            throw new CustomBusinessException(ErrorCode.ACCESS_DENIED);
        }
    }

    private Map<String, Object> successResponse(String message, Object data) {
        return Map.of(
                "status", HttpStatus.OK.value(),
                "message", message,
                "data", data
        );
    }
}
