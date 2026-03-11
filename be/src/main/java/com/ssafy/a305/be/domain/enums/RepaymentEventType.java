package com.ssafy.a305.be.domain.enums;

/**
 * repayment_event.event_type
 */
public enum RepaymentEventType {
    CASH,            // 현금
    MANUAL_TRANSFER, // 수동이체
    AUTO_TRANSFER,   // 자동이체감지
    WAIVER           // 면제
}
