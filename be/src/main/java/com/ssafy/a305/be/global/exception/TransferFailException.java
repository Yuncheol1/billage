package com.ssafy.a305.be.global.exception;

/**
 * SSAFY 이체 API 호출 실패 시 발생하는 예외.
 *
 * <p>자동이체 스케줄러는 회차마다 이 예외를 잡아 해당 건만 실패로 기록하고
 * 다음 회차로 진행합니다. 즉 한 건의 실패가 나머지 건에 전파되지 않습니다.</p>
 */
public class TransferFailException extends RuntimeException {

    public TransferFailException(String message) {
        super(message);
    }

    public TransferFailException(String message, Throwable cause) {
        super(message, cause);
    }
}
