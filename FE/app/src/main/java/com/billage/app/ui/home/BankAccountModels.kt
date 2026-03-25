package com.billage.app.ui.home

import kotlinx.serialization.Serializable

// ─────────────────────────────────────────
// BE API 응답 모델
// ─────────────────────────────────────────

@Serializable
data class BankAccountResponse(
    val accountSeq: Int,
    val bankCode: String,
    val accountNo: String,
    val isPrimary: Boolean,
    val linkedDttm: String,
    val status: String
)

@Serializable
data class BankAccountInfo(
    val accountSeq: Int? = null,
    val bankCode: String = "",
    val bankName: String = "",
    val accountName: String = "",           // 계좌명 (예: "빌리지 수시입출금")
    val accountTypeName: String = "",       // 계좌 종류 (예: "수시입출금")
    val accountNo: String = "",              // "0014982189036058" 형태
    val accountBalance: String = "0",       // BE가 String으로 반환
    val currency: String = "KRW",
    val accountCreatedDate: String = "",    // "20260325" 형태
    val accountExpiryDate: String = "",     // "20310325" 형태
    val dailyTransferLimit: String = "",    // 일 이체 한도
    val oneTimeTransferLimit: String = "",  // 1회 이체 한도
    val isPrimary: Boolean = false,
    val status: String = ""
) {
    val accountBalanceLong: Long get() = accountBalance.toLongOrNull() ?: 0L
}

// ─────────────────────────────────────────
// 거래 내역 — BE 미구현, 목데이터용 모델
// ─────────────────────────────────────────

data class Transaction(
    val transactionSeq: Int,
    val date: String,
    val time: String,
    val description: String,
    val amount: Long,
    val type: TransactionType,
    val memo: String? = null,
    val accountNumber: String = ""  // 거래 계좌번호 (마스킹 포함)
)

enum class TransactionType { DEPOSIT, WITHDRAW }

val mockTransactions = listOf(
    Transaction(1, "2026.03.24", "14:32", "김철수 상환", 300000, TransactionType.DEPOSIT, "3월 상환금", "110-123-*****"),
    Transaction(2, "2026.03.23", "09:15", "이영희 대출", 1000000, TransactionType.WITHDRAW, null, "220-456-*****"),
    Transaction(3, "2026.03.22", "18:47", "박민수 상환", 500000, TransactionType.DEPOSIT, null, "330-789-*****")
)
