package com.ssafy.a305.be.global.exception;

import com.ssafy.a305.be.global.exception.dto.ErrorCode;
import com.ssafy.a305.be.global.exception.dto.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CustomBusinessException.class)
    public ResponseEntity<ErrorResponse> handleCustomBusinessException(CustomBusinessException e) {
        log.warn("CustomBusinessException: {}", e.getMessage());
        return ResponseEntity
                .status(e.getErrorCode().getStatus())
                .body(ErrorResponse.builder()
                        .status(e.getErrorCode().getStatus().value())
                        .error(e.getErrorCode().name())
                        .message(e.getMessage())
                        .timestamp(LocalDateTime.now())
                        .build());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException e) {
        String errorMessage = e.getBindingResult().getAllErrors().get(0).getDefaultMessage();
        log.warn("ValidationException: {}", errorMessage);

        return ResponseEntity
                .status(ErrorCode.INVALID_INPUT_VALUE.getStatus())
                .body(ErrorResponse.builder()
                        .status(ErrorCode.INVALID_INPUT_VALUE.getStatus().value())
                        .error(ErrorCode.INVALID_INPUT_VALUE.name())
                        .message(errorMessage)
                        .timestamp(LocalDateTime.now())
                        .build());
    }

    @ExceptionHandler(TransferFailException.class)
    public ResponseEntity<ErrorResponse> handleTransferFailException(TransferFailException e) {
        String raw = e.getMessage();
        String responseCode = "Q1000";
        String responseMessage = raw != null ? raw : "이체 처리 중 오류가 발생했습니다.";

        if (raw != null && raw.startsWith("SSAFY_TRANSFER_FAILED|")) {
            String[] parts = raw.split("\\|", 3);
            if (parts.length >= 2 && !parts[1].isBlank()) {
                responseCode = parts[1];
            }
            if (parts.length >= 3 && !parts[2].isBlank()) {
                responseMessage = parts[2];
            }
        }

        HttpStatus status = resolveTransferHttpStatus(responseCode);
        String message = resolveTransferMessage(responseCode, responseMessage);
        log.warn("TransferFailException: code={}, message={}", responseCode, message);

        return ResponseEntity
                .status(status)
                .body(ErrorResponse.builder()
                        .status(status.value())
                        .error(responseCode)
                        .message(message)
                        .timestamp(LocalDateTime.now())
                        .build());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpectedException(Exception e) {
        log.error("UnexpectedException", e);
        return ResponseEntity
                .internalServerError()
                .body(ErrorResponse.builder()
                        .status(500)
                        .error("INTERNAL_SERVER_ERROR")
                        .message(e.getMessage() != null ? e.getMessage() : "서버 내부 오류가 발생했습니다.")
                        .timestamp(LocalDateTime.now())
                        .build());
    }

    private HttpStatus resolveTransferHttpStatus(String responseCode) {
        return switch (responseCode) {
            case "A1014", "A1016", "A1017", "A1011", "A1003", "A1018", "A1010", "A1023", "A1019", "A5004",
                 "Q1001" -> HttpStatus.BAD_REQUEST;
            case "H1008", "H1009", "H1004", "H1005", "H1006" -> HttpStatus.UNAUTHORIZED;
            case "H1000", "H1001", "H1002", "H1003", "H1010", "H1007", "Q1000" -> HttpStatus.BAD_GATEWAY;
            default -> HttpStatus.BAD_GATEWAY;
        };
    }

    private String resolveTransferMessage(String responseCode, String fallbackMessage) {
        return switch (responseCode) {
            case "A1014" -> "계좌 잔액이 부족하여 이체에 실패했습니다.";
            case "A1016" -> "1회 이체 한도를 초과했습니다.";
            case "A1017" -> "1일 이체 한도를 초과했습니다.";
            case "A1011" -> "거래 금액이 유효하지 않습니다.";
            case "A1003" -> "계좌번호가 유효하지 않습니다.";
            case "A1018" -> "거래요약내용 길이가 초과되었습니다.";
            case "Q1001" -> "요청 본문 형식이 올바르지 않습니다.";
            default -> fallbackMessage != null && !fallbackMessage.isBlank()
                    ? fallbackMessage
                    : "이체 처리 중 오류가 발생했습니다.";
        };
    }
}
