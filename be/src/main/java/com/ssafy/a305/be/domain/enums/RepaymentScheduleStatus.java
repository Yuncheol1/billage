package com.ssafy.a305.be.domain.enums;

/**
 * repayment_schedule.status
 */
public enum RepaymentScheduleStatus {
    PENDING,  // 미납
    PAID,     // 납부완료
    WAIVED,   // 면제
    OVERDUE   // 연체
}
