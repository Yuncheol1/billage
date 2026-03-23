package com.ssafy.a305.be.domain.enums;

/**
 * bank_account.status
 */
public enum BankAccountStatus {
    /** Account created but user ownership verification is pending */
    PENDING_VERIFICATION,
    ACTIVE,
    CLOSED,
    SUSPENDED
}
