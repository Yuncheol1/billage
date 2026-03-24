package com.billage.app.ui.contract.create.steps

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.billage.app.ui.theme.BillageBackground
import com.billage.app.ui.theme.BillageDanger
import com.billage.app.ui.theme.BillagePrimary
import com.billage.app.ui.theme.BillagePrimaryLight
import com.billage.app.ui.theme.BillageTextPrimary
import com.billage.app.ui.theme.BillageTextSecondary
import java.text.NumberFormat
import java.util.Locale

private const val MAX_DIGITS = 12 // 최대 12자리 (999,999,999,999원)

// 숫자 문자열을 "3억원", "500만원" 형태로 변환
private fun formatAmountLabel(raw: String): String {
    val number = raw.toLongOrNull() ?: return ""
    if (number == 0L) return ""

    val eok = number / 100_000_000L
    val man = (number % 100_000_000L) / 10_000L
    val rest = number % 10_000L

    return buildString {
        if (eok > 0) append("${eok}억원 ")
        if (man > 0) append("${man}만원 ")
        if (rest > 0) append("$rest")
    }.trim()
}

// 천 단위 쉼표 포맷 (예: 300000000 → 300,000,000)
private fun formatWithComma(raw: String): String {
    val number = raw.toLongOrNull() ?: return ""
    return NumberFormat.getNumberInstance(Locale.KOREA).format(number)
}

@Composable
fun Step3AmountScreen(
    amount: String,
    onAmountChange: (String) -> Unit
) {
    val amountLabel = formatAmountLabel(amount)
    // UI 전용 상태 — ViewModel까지 올릴 필요 없는 경고 표시 여부
    var showLimitWarning by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .padding(top = 32.dp)
    ) {
        Text(
            text = "계약 만들기",
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = BillagePrimary
        )

        Spacer(modifier = Modifier.height(8.dp))

        // 금액 입력 시 해당 부분만 민트색으로 하이라이트
        Text(
            text = buildAnnotatedString {
                if (amountLabel.isEmpty()) {
                    append("얼마를 빌려\n주시나요?")
                } else {
                    withStyle(SpanStyle(color = BillagePrimary)) {
                        append(amountLabel)
                    }
                    append("을\n빌려 주시나요?")
                }
            },
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = BillageTextPrimary,
            lineHeight = 36.sp
        )

        Spacer(modifier = Modifier.height(40.dp))

        // 금액 입력 필드
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = BillageBackground,
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(horizontal = 20.dp, vertical = 18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BasicTextField(
                    value = formatWithComma(amount),
                    onValueChange = { input ->
                        val digits = input.filter { it.isDigit() }.trimStart('0')
                        if (digits.length > MAX_DIGITS) {
                            showLimitWarning = true
                        } else {
                            showLimitWarning = false
                            onAmountChange(digits)
                        }
                    },
                    modifier = Modifier.weight(1f),
                    textStyle = TextStyle(
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = BillageTextPrimary
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    cursorBrush = SolidColor(BillagePrimary),
                    decorationBox = { innerTextField ->
                        if (amount.isEmpty()) {
                            Text(
                                text = "0",
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = BillageTextSecondary
                            )
                        }
                        innerTextField()
                    }
                )

                Text(
                    text = "원",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = BillageTextSecondary
                )
            }
        }

        // 경고 문구 + 초기화 버튼 한 행 — 항상 공간 유지로 레이아웃 밀림 방지
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 경고 문구 — 없을 때 투명 처리
            Text(
                text = "너무 큰 수는 입력할 수 없어요",
                fontSize = 12.sp,
                color = if (showLimitWarning) BillageDanger else Color.Transparent
            )

            // 초기화 버튼 — 없을 때 투명 처리
            Text(
                text = "초기화",
                fontSize = 11.sp,
                color = if (amount.isNotEmpty()) BillageTextSecondary else Color.Transparent,
                modifier = Modifier.clickable(enabled = amount.isNotEmpty()) {
                    onAmountChange("")
                    showLimitWarning = false
                }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 빠른 금액 추가 버튼
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(
                "+10만" to 100_000L,
                "+100만" to 1_000_000L,
                "+1천만" to 10_000_000L,
                "+1억" to 100_000_000L
            ).forEach { (label, value) ->
                QuickAmountButton(
                    modifier = Modifier.weight(1f),
                    label = label,
                    onClick = {
                        val current = amount.toLongOrNull() ?: 0L
                        val next = current + value
                        if (next.toString().length <= MAX_DIGITS) {
                            showLimitWarning = false
                            onAmountChange(next.toString())
                        } else {
                            showLimitWarning = true
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun QuickAmountButton(
    modifier: Modifier = Modifier,
    label: String,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .background(
                color = BillagePrimaryLight,
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
            color = BillagePrimary
        )
    }
}