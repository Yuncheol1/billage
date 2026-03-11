package com.ssafy.a305.be.domain.enums;

/**
 * bank_transaction.match_status
 */
public enum MatchStatus {
    UNMATCHED,       // 미매칭
    MATCHED,         // 매칭완료
    IGNORED,         // 무시
    PENDING_REVIEW   // 신뢰도 낮아 수동검토 필요
}
