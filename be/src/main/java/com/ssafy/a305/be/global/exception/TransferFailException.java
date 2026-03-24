package com.ssafy.a305.be.global.exception;

/**
 * SSAFY 이체 API 호출 실패 시 발생하는 예외.
 * Spring Batch FaultTolerant에서 Skip 대상으로 등록됩니다.
 */
public class TransferFailException extends RuntimeException {

    public TransferFailException(String message) {
        super(message);
    }

    public TransferFailException(String message, Throwable cause) {
        super(message, cause);
    }
}
