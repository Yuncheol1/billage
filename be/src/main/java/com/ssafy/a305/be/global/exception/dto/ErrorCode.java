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
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "잘못된 입력값입니다."),
    KAKAO_AUTH_FAILED(HttpStatus.BAD_REQUEST, "카카오 인증에 실패했습니다."),
    KAKAO_USER_INFO_FAILED(HttpStatus.BAD_REQUEST, "카카오 사용자 정보를 조회할 수 없습니다."),
    LOGIN_NOT_ALLOWED(HttpStatus.FORBIDDEN, "로그인할 수 없는 사용자 상태입니다."),
    AUTHENTICATION_REQUIRED(HttpStatus.UNAUTHORIZED, "인증이 필요합니다."),
    INVALID_ACCESS_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 액세스 토큰입니다."),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 리프레시 토큰입니다."),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),
    AUTH_CONFIG_INVALID(HttpStatus.INTERNAL_SERVER_ERROR, "인증 설정이 올바르지 않습니다.");

    private final HttpStatus status;
    private final String message;
}
