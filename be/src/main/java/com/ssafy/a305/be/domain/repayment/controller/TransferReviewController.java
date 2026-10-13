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
 * 정산이 자동으로 판단하지 못한 이체를 운영자가 확인·확정하는 API.
 *
 * <p>사용자용 API가 아니므로 JWT 대신 {@code X-Ops-API-Key} 헤더로 인증한다.
 * 키가 설정되지 않은 환경에서는 모든 요청을 거절한다.</p>
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

    /** 키 미설정이면 항상 거절하고, 비교는 길이·내용 노출을 막기 위해 상수 시간으로 한다. */
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
