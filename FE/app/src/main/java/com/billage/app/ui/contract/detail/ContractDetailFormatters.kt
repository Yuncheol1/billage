package com.billage.app.ui.contract.detail

import androidx.compose.ui.graphics.Color
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

private val RepaymentDanger = Color(0xFFFF6B6B)
private val RepaymentInfo = Color(0xFF26C6DA)
private val RepaymentPrimary = Color(0xFF2EC4A9)
private val RepaymentSuccess = Color(0xFF4CAF50)
private val RepaymentTextSecondary = Color(0xFF7A9490)
private val RepaymentWarning = Color(0xFFFFA000)

internal fun formatAmount(amount: Double): String =
    "%,.0f".format(amount)

internal fun formatCurrency(amount: Double): String =
    "${formatAmount(amount)}원"

internal fun formatRepaymentMethodFriendly(method: String?, totalInstallments: Int?): String {
    val countSuffix = totalInstallments?.let { " 총 ${it}회" } ?: ""
    return when (method) {
        "LUMP_SUM" -> "만기일에 한 번에"
        "MONTHLY" -> "매월$countSuffix"
        "QUARTERLY" -> "3개월마다$countSuffix"
        "SEMI_ANNUAL" -> "6개월마다$countSuffix"
        "ANNUAL" -> "1년마다$countSuffix"
        else -> "-"
    }
}

internal fun calculateDday(endDt: String): String {
    return try {
        val days = ChronoUnit.DAYS.between(LocalDate.now(), LocalDate.parse(endDt))
        when {
            days > 0 -> "D-$days"
            days == 0L -> "오늘 만기"
            else -> "만기 초과"
        }
    } catch (_: Exception) {
        "-"
    }
}

internal fun formatDate(value: String?): String {
    if (value.isNullOrBlank()) return "-"
    return runCatching {
        LocalDate.parse(value).format(DateTimeFormatter.ofPattern("yyyy.MM.dd"))
    }.getOrElse { value.substringBefore("T").replace("-", ".") }
}

internal fun formatDateTime(value: String?): String {
    if (value.isNullOrBlank()) return "-"
    return runCatching {
        LocalDateTime.parse(value).format(DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm"))
    }.getOrElse { value.replace("T", " ").take(16) }
}

internal fun repaymentStatusLabel(status: String): String = when (status) {
    "NONPAID" -> "납부 예정"
    "SOMEPAID" -> "부분 납부"
    "PAID" -> "납부 완료"
    "MANUAL_PAID" -> "수동 상환"
    "WAIVED" -> "면제"
    "OVERDUE" -> "연체"
    else -> status
}

internal fun repaymentStatusColor(status: String): Color = when (status) {
    "PAID" -> RepaymentSuccess
    "OVERDUE" -> RepaymentDanger
    "SOMEPAID" -> RepaymentWarning
    "WAIVED" -> RepaymentInfo
    "NONPAID" -> RepaymentPrimary
    "MANUAL_PAID" -> RepaymentTextSecondary
    else -> RepaymentTextSecondary
}
