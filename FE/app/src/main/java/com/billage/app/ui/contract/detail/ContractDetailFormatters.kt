package com.billage.app.ui.contract.detail

import java.time.LocalDate
import java.time.temporal.ChronoUnit

internal fun formatAmount(amount: Double): String =
    "%,.0f".format(amount)

internal fun formatRepaymentMethodFriendly(method: String?, totalInstallments: Int?): String {
    val n = totalInstallments?.let { " · 총 ${it}회" } ?: ""
    return when (method) {
        "LUMP_SUM"    -> "만기일에 한 번에"
        "MONTHLY"     -> "매달$n"
        "QUARTERLY"   -> "3개월마다$n"
        "SEMI_ANNUAL" -> "6개월마다$n"
        "ANNUAL"      -> "1년마다$n"
        else          -> "-"
    }
}

internal fun calculateDday(endDt: String): String {
    return try {
        val days = ChronoUnit.DAYS.between(LocalDate.now(), LocalDate.parse(endDt))
        when {
            days > 0   -> "D-$days"
            days == 0L -> "오늘 만기"
            else       -> "만기 초과"
        }
    } catch (e: Exception) {
        "-"
    }
}
