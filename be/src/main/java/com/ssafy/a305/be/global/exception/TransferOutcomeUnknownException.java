package com.ssafy.a305.be.global.exception;

/**
 * 이체 요청은 보냈지만 <b>결과를 알 수 없는</b> 경우.
 *
 * <p>응답 타임아웃, 응답 본문 누락, 분류하지 못한 응답코드가 여기에 속한다. 은행이 요청을
 * 처리했을 수도 있으므로 "실패"로 단정하면 안 된다. 실패로 보고 재시도하면 이미 나간 돈이
 * 한 번 더 나간다.</p>
 *
 * <p>{@link TransferFailException}과 달리 <b>돈이 나가지 않았다는 보장이 없다.</b>
 * 호출자는 시도를 미해결로 남기고 그 회차의 새 이체를 막아야 한다.
 * 기존 예외 처리 경로(응답 변환 등)를 그대로 타도록 {@code TransferFailException}을 상속한다.</p>
 */
public class TransferOutcomeUnknownException extends TransferFailException {

    public TransferOutcomeUnknownException(String message) {
        super(message);
    }

    public TransferOutcomeUnknownException(String message, Throwable cause) {
        super(message, cause);
    }
}
