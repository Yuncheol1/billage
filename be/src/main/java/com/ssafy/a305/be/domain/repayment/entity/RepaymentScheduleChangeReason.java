package com.ssafy.a305.be.domain.repayment.entity;

/**
 * 회차 상태가 바뀐 이유. {@link RepaymentScheduleHistory}에 남긴다.
 *
 * <p>상태만으로는 "왜 미납으로 돌아왔는지"를 알 수 없다. 은행이 거절했는지, 정산에서 출금이 없다고
 * 판정했는지, 운영자가 확인했는지를 이 값으로 구분한다.</p>
 */
public enum RepaymentScheduleChangeReason {

    /** 자동이체가 회차를 선점했다. */
    AUTO_TRANSFER_REQUESTED,
    /** 채무자의 수동 이체가 회차를 선점했다. */
    MANUAL_TRANSFER_REQUESTED,
    /** 은행이 이체 성공을 확인해 줬다. */
    TRANSFER_SUCCEEDED,
    /** 은행이 이체를 거절해 이체 전 상태로 되돌렸다. */
    TRANSFER_REJECTED,
    /** 이체를 보냈지만 결과를 모른다. 상태는 REQUESTED 그대로다. */
    TRANSFER_OUTCOME_UNKNOWN,
    /** 정산에서 거래내역의 출금을 찾아 확정했다. */
    RECONCILED_PAID,
    /** 정산에서 출금이 없다고 판정해 되돌렸다. */
    RECONCILED_NOT_WITHDRAWN,
    /** 운영자가 거래내역을 확인해 확정했다. */
    REVIEWED_PAID,
    /** 운영자가 출금이 없음을 확인해 되돌렸다. */
    REVIEWED_NOT_WITHDRAWN,
    /** 채권자가 직접 받았다고 처리했다. */
    CREDITOR_MARKED_PAID,
    /** 납부 기한이 지나 연체로 바뀌었다. */
    OVERDUE
}
