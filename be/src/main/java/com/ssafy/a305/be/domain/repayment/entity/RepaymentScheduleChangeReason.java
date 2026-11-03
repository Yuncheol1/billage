package com.ssafy.a305.be.domain.repayment.entity;

/**
 * 회차 상태 변경 사유
 */
public enum RepaymentScheduleChangeReason {
    AUTO_TRANSFER_REQUESTED,   // 자동이체 선점
    MANUAL_TRANSFER_REQUESTED, // 수동 이체 선점
    TRANSFER_SUCCEEDED,
    TRANSFER_REJECTED,         // 은행 거절 → 되돌림
    TRANSFER_OUTCOME_UNKNOWN,  // 결과 불명, 상태는 REQUESTED 그대로
    RECONCILED_PAID,           // 정산: 출금 확인
    RECONCILED_NOT_WITHDRAWN,  // 정산: 출금 없음 → 되돌림
    REVIEWED_PAID,             // 운영자: 출금 확인
    REVIEWED_NOT_WITHDRAWN,    // 운영자: 출금 없음 → 되돌림
    CREDITOR_MARKED_PAID,      // 채권자 수동 완료
    OVERDUE
}
