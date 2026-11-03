package com.ssafy.a305.be.global.exception;

/**
 * SSAFY 이체 API 호출 실패 시 발생하는 예외
 */
public class TransferFailException extends RuntimeException {

    private static final String FAILED_PREFIX = "SSAFY_TRANSFER_FAILED|";

    // 은행 응답 코드. 연결 실패 등 코드가 없으면 null
    public String getResponseCode() {
        String message = getMessage();
        if (message == null || !message.startsWith(FAILED_PREFIX)) {
            return null;
        }
        String[] parts = message.split("\\|", 3);
        return parts.length >= 2 && !parts[1].isBlank() ? parts[1] : null;
    }

    public TransferFailException(String message) {
        super(message);
    }

    public TransferFailException(String message, Throwable cause) {
        super(message, cause);
    }
}
