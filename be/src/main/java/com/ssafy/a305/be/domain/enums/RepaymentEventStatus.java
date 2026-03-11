package com.ssafy.a305.be.domain.enums;

/**
 * repayment_event.status
 */
public enum RepaymentEventStatus {
    REQUESTED,      // 요청됨
    CONFIRMED,      // 채권자 확인완료
    REJECTED,       // 거절
    AUTO_CONFIRMED  // 자동이체감지로 자동확정
}
