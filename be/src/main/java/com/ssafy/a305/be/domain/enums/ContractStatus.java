package com.ssafy.a305.be.domain.enums;

/**
 * contract.status - 계약 생명주기
 */
public enum ContractStatus {
    DRAFT,        // 초안
    NEGOTIATING,  // 협의 중
    SIGNED,       // 서명 완료
    ACTIVE,       // 진행 중
    OVERDUE,      // 연체
    CLOSED,       // 종료
    DISPUTED      // 분쟁
}
