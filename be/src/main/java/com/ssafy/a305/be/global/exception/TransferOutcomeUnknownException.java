package com.ssafy.a305.be.global.exception;

/**
 * 이체 결과를 알 수 없음 (응답 타임아웃, 본문 없음, 모르는 응답 코드 등)
 * 돈이 나갔을 수 있으므로 실패로 보고 재시도하면 안 된다.
 * 기존 예외 처리 경로를 타도록 TransferFailException을 상속한다.
 */
public class TransferOutcomeUnknownException extends TransferFailException {

    public TransferOutcomeUnknownException(String message) {
        super(message);
    }

    public TransferOutcomeUnknownException(String message, Throwable cause) {
        super(message, cause);
    }
}
