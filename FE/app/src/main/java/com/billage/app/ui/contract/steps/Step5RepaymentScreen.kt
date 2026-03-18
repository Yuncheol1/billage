package com.billage.app.ui.contract.steps

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.billage.app.ui.contract.RepaymentMethod
import com.billage.app.ui.theme.BillageCardBorder
import com.billage.app.ui.theme.BillageDanger
import com.billage.app.ui.theme.BillagePrimary
import com.billage.app.ui.theme.BillagePrimaryLight
import com.billage.app.ui.theme.BillageTextPrimary
import com.billage.app.ui.theme.BillageTextSecondary
import com.billage.app.ui.theme.BillageWarningBackground
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

private val DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd")

private fun calculateMonthsBetween(startDate: String, endDate: String): Long {
    return runCatching {
        val start = LocalDate.parse(startDate, DATE_FORMATTER)
        val end = LocalDate.parse(endDate, DATE_FORMATTER)
        ChronoUnit.MONTHS.between(start, end)
    }.getOrDefault(0L)
}

private fun getAvailableMethods(months: Long): List<Pair<RepaymentMethod, String>> {
    return buildList {
        if (months >= 1) add(RepaymentMethod.MONTHLY to "월마다")
        if (months >= 3) add(RepaymentMethod.QUARTERLY to "분기마다")
        if (months >= 6) add(RepaymentMethod.SEMI_ANNUAL to "반기마다")
        if (months >= 24) add(RepaymentMethod.ANNUAL to "연마다")
    }
}

// 잔여 개월 수 계산 — 상환 주기로 나누어 떨어지지 않는 나머지
private fun calculateRemainderMonths(months: Long, method: RepaymentMethod): Int {
    return when (method) {
        RepaymentMethod.MONTHLY -> 0
        RepaymentMethod.QUARTERLY -> (months % 3).toInt()
        RepaymentMethod.SEMI_ANNUAL -> (months % 6).toInt()
        RepaymentMethod.ANNUAL -> (months % 12).toInt()
        else -> 0
    }
}

@Composable
fun Step5RepaymentScreen(
    startDate: String,
    endDate: String,
    selectedMethod: RepaymentMethod?,
    onMethodSelect: (RepaymentMethod) -> Unit
) {
    val isInstallment = selectedMethod != null && selectedMethod != RepaymentMethod.LUMP_SUM
    val months = remember(startDate, endDate) { calculateMonthsBetween(startDate, endDate) }
    val availableMethods = remember(months) { getAvailableMethods(months) }

    val isMethodInvalid = selectedMethod != null
            && selectedMethod != RepaymentMethod.LUMP_SUM
            && availableMethods.none { it.first == selectedMethod }

    val remainder = if (selectedMethod != null && selectedMethod != RepaymentMethod.LUMP_SUM) {
        calculateRemainderMonths(months, selectedMethod)
    } else 0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
            .padding(top = 32.dp, bottom = 24.dp)
    ) {
        Text(
            text = "계약 만들기",
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = BillagePrimary
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "원금을 어떻게\n갚을까요?",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = BillageTextPrimary,
            lineHeight = 36.sp
        )

        Spacer(modifier = Modifier.height(40.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            RepaymentMethodCard(
                modifier = Modifier.weight(1f),
                emoji = "💰",
                title = "한 번에",
                description = "만기일에 원금 전액을\n한꺼번에 갚아요",
                isSelected = selectedMethod == RepaymentMethod.LUMP_SUM,
                onClick = { onMethodSelect(RepaymentMethod.LUMP_SUM) }
            )
            RepaymentMethodCard(
                modifier = Modifier.weight(1f),
                emoji = "📅",
                title = "나눠서",
                description = "일정 기간마다\n원금 일부를 갚아요",
                isSelected = isInstallment,
                onClick = {
                    val firstAvailable = availableMethods.firstOrNull()?.first
                    if (firstAvailable != null) onMethodSelect(firstAvailable)
                }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        AnimatedVisibility(
            visible = selectedMethod == RepaymentMethod.LUMP_SUM,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            InfoBox(text = "원금은 만기에 한 번에 갚고, 이자는 매달 납부하는 방식이에요")
        }

        AnimatedVisibility(
            visible = isInstallment,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Column {
                Text(
                    text = "얼마나 자주 갚을까요?",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = BillageTextPrimary
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    availableMethods.forEach { (method, label) ->
                        PeriodChip(
                            modifier = Modifier.weight(1f),
                            label = label,
                            isSelected = selectedMethod == method,
                            onClick = { onMethodSelect(method) }
                        )
                    }
                }

                // 계약 기간보다 긴 주기 선택 시 경고
                if (isMethodInvalid) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "⚠️", fontSize = 12.sp)
                        Text(
                            text = "계약 기간보다 긴 주기예요. 다시 선택해주세요",
                            fontSize = 12.sp,
                            color = BillageDanger
                        )
                    }
                }

                // 잔여 개월 안내 — 상환 주기로 나누어 떨어지지 않을 때
                if (remainder > 0) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(text = "💡", fontSize = 12.sp)
                        Text(
                            text = "계약 기간이 상환 주기로 딱 나누어 떨어지지 않아요. " +
                                    "마지막 ${remainder}개월은 상환 일정에서 제외되고 만기일에 함께 정산돼요",
                            fontSize = 12.sp,
                            color = BillageTextSecondary,
                            lineHeight = 18.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                InfoBox(text = "원금을 갚을수록 남은 원금이 줄어들어 내는 이자도 조금씩 줄어들어요")
            }
        }
    }
}

@Composable
private fun RepaymentMethodCard(
    modifier: Modifier = Modifier,
    emoji: String,
    title: String,
    description: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) BillagePrimary else BillageCardBorder
        ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) BillagePrimaryLight else Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 20.dp, horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = emoji, fontSize = 28.sp)

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = BillageTextPrimary
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = description,
                fontSize = 11.sp,
                color = BillageTextSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
private fun PeriodChip(
    modifier: Modifier = Modifier,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .background(
                color = if (isSelected) BillagePrimary else BillagePrimaryLight,
                shape = RoundedCornerShape(10.dp)
            )
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = if (isSelected) Color.White else BillagePrimary
        )
    }
}

@Composable
private fun InfoBox(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = BillageWarningBackground,
                shape = RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(text = "💡", fontSize = 14.sp)
        Text(
            text = text,
            fontSize = 13.sp,
            color = BillageTextSecondary,
            lineHeight = 19.sp
        )
    }
}