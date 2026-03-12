package com.ssafy.a305.be.global.exception.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    INVALID_DATE(HttpStatus.BAD_REQUEST, "종료일은 시작일보다 미래여야 합니다."),
    EXCEED_LEGAL_INTEREST_RATE(HttpStatus.BAD_REQUEST, "법정 최고 이자율(연 20%)을 초과할 수 없습니다."),
    SELF_CONTRACT_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "본인과의 계약은 불가능합니다."),
    INACTIVE_USER_TARGET(HttpStatus.BAD_REQUEST, "상대방이 탈퇴했거나 정지된 회원입니다."),
    INVALID_REPAYMENT_LOGIC(HttpStatus.BAD_REQUEST, "상환 방식 및 횟수에 모순이 있습니다."),
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "잘못된 입력값입니다.");

    private final HttpStatus status;
    private final String message;
}
