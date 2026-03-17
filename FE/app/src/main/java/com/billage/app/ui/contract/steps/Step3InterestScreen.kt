package com.billage.app.ui.contract.steps

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.billage.app.ui.theme.BillageDanger
import com.billage.app.ui.theme.BillagePrimary
import com.billage.app.ui.theme.BillagePrimaryLight
import com.billage.app.ui.theme.BillageTextPrimary
import com.billage.app.ui.theme.BillageTextSecondary
import com.billage.app.ui.theme.BillageWarningBackground
import com.billage.app.ui.theme.BillageCardBorder
import com.billage.app.ui.theme.BillageDialogDivider
import kotlin.math.roundToInt

private const val DEFAULT_RATE = "4.6"
private const val MAX_LEGAL_RATE = 20.0
private const val LEGAL_RATE = 0.046
private const val GIFT_TAX_MIN = 10_000_000.0

// 무이자 시 연간 이자이익이 1천만원 이상인지 (증여세 과세 대상 여부)
private fun isNoInterestGiftTaxRisk(amountStr: String): Boolean {
    val amount = amountStr.toLongOrNull() ?: return false
    return amount * LEGAL_RATE >= GIFT_TAX_MIN
}

// 저리대여 시 이자 차액이 1천만원 이상인지 (증여세 과세 대상 여부)
private fun isLowInterestGiftTaxRisk(amountStr: String, rateStr: String): Boolean {
    val amount = amountStr.toLongOrNull() ?: return false
    val rate = rateStr.toDoubleOrNull() ?: return false
    if (rate >= 4.6) return false
    val giftTaxableAmount = amount * LEGAL_RATE - amount * (rate / 100.0)
    return giftTaxableAmount >= GIFT_TAX_MIN
}

// 이자율 조정 (소수점 오차 방지를 위해 roundToInt 사용)
private fun adjustRate(current: String, delta: Double): String {
    val currentRate = current.toDoubleOrNull() ?: 0.0
    val adjusted = ((currentRate + delta) * 10).roundToInt() / 10.0
    return when {
        adjusted <= 0.0 -> "0.0"
        adjusted > MAX_LEGAL_RATE -> MAX_LEGAL_RATE.toString()
        else -> adjusted.toString()
    }
}

@Composable
fun Step3InterestScreen(
    interestRate: String,
    isFamily: Boolean,
    amount: String,
    onRateChange: (String) -> Unit
) {
    var wantsInterest by remember { mutableStateOf<Boolean?>(null) }
    var showNoInterestWarning by remember { mutableStateOf(false) }

    // 이전 스텝에서 돌아온 경우 선택 상태 복원
    LaunchedEffect(Unit) {
        when {
            interestRate == "0" -> wantsInterest = false
            interestRate.isNotBlank() -> wantsInterest = true
        }
    }

    // 20% 초과 입력 시 자동으로 20%로 고정
    LaunchedEffect(interestRate) {
        val rate = interestRate.toDoubleOrNull() ?: return@LaunchedEffect
        if (rate > MAX_LEGAL_RATE) onRateChange(MAX_LEGAL_RATE.toString())
    }

    val showLowInterestWarning = wantsInterest == true
            && isFamily
            && isLowInterestGiftTaxRisk(amount, interestRate)

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

        Text(
            text = "이자를 받으시나요?",
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
            InterestOptionCard(
                modifier = Modifier.weight(1f),
                emoji = "✅",
                title = "받을게요",
                description = "이자율을 직접 설정해요",
                isSelected = wantsInterest == true,
                onClick = {
                    wantsInterest = true
                    if (interestRate == "0" || interestRate.isBlank()) {
                        onRateChange(DEFAULT_RATE)
                    }
                }
            )
            InterestOptionCard(
                modifier = Modifier.weight(1f),
                emoji = "🙅",
                title = "안받을게요",
                description = "무이자 계약으로 진행해요",
                isSelected = wantsInterest == false,
                onClick = {
                    if (isFamily && isNoInterestGiftTaxRisk(amount)) {
                        showNoInterestWarning = true
                    } else {
                        wantsInterest = false
                        onRateChange("0")
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (wantsInterest == true) {
            // 이자율 입력 필드
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = BillagePrimaryLight,
                        shape = RoundedCornerShape(16.dp)
                    )
                    .padding(horizontal = 20.dp, vertical = 18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BasicTextField(
                        value = if (interestRate == "0") "" else interestRate,
                        onValueChange = { input ->
                            val filtered = input.filter { it.isDigit() || it == '.' }
                            val dotIndex = filtered.indexOf('.')
                            val valid = if (dotIndex != -1) {
                                filtered.substring(0, minOf(dotIndex + 3, filtered.length))
                            } else filtered
                            if (valid.toDoubleOrNull() != null || valid.isEmpty()) {
                                onRateChange(valid)
                            }
                        },
                        modifier = Modifier.weight(1f),
                        textStyle = TextStyle(
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = BillageTextPrimary
                        ),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        cursorBrush = SolidColor(BillagePrimary),
                        decorationBox = { innerTextField ->
                            if (interestRate.isBlank()) {
                                Text(
                                    text = "0.0",
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BillageTextSecondary
                                )
                            }
                            innerTextField()
                        }
                    )

                    Text(
                        text = "%",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Medium,
                        color = BillageTextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 이자율 조정 칩 버튼 (Step2 빠른 금액 버튼과 동일한 스타일)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    "-0.5%" to -0.5,
                    "-0.1%" to -0.1,
                    "+0.1%" to 0.1,
                    "+0.5%" to 0.5
                ).forEach { (label, delta) ->
                    RateAdjustButton(
                        modifier = Modifier.weight(1f),
                        label = label,
                        onClick = { onRateChange(adjustRate(interestRate, delta)) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 안내 문구 — 상태에 따라 분기
            when {
                interestRate.toDoubleOrNull() == MAX_LEGAL_RATE -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(text = "⚠️", fontSize = 12.sp)
                        Text(
                            text = "최고 이자율은 연 20%예요",
                            fontSize = 12.sp,
                            color = BillageDanger
                        )
                    }
                }
                showLowInterestWarning -> {
                    Row(
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(text = "⚠️", fontSize = 12.sp)
                        Text(
                            text = "설정한 이자율이 낮아 이자 차액이 연 1천만 원 이상이에요. 그 차액만큼 증여로 볼 수 있어요",
                            fontSize = 12.sp,
                            color = BillageDanger,
                            lineHeight = 18.sp
                        )
                    }
                }
                interestRate == DEFAULT_RATE -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(text = "✓", fontSize = 12.sp, color = BillagePrimary)
                        Text(
                            text = "세법상 적정 이자율이에요! (연 4.6%)",
                            fontSize = 12.sp,
                            color = BillageTextSecondary
                        )
                    }
                }
            }
        }
    }

    if (showNoInterestWarning) {
        GiftTaxWarningDialog(
            onConfirm = {
                showNoInterestWarning = false
                wantsInterest = false
                onRateChange("0")
            },
            onDismiss = {
                showNoInterestWarning = false
            }
        )
    }
}

@Composable
private fun InterestOptionCard(
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

// 이자율 조정 칩 버튼
@Composable
private fun RateAdjustButton(
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

@Composable
private fun GiftTaxWarningDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = Color.White,
                    shape = RoundedCornerShape(20.dp)
                )
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        color = BillageWarningBackground,
                        shape = RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "⚠️", fontSize = 24.sp)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "증여세 과세 주의",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = BillageTextPrimary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "2억 원을 초과하는 무이자 대여는 세법상 이자 상당액을 증여로 볼 수 있어요. 무이자로 진행하시겠어요?",
                fontSize = 14.sp,
                color = BillageTextSecondary,
                lineHeight = 20.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            HorizontalDivider(color = BillageDialogDivider)

            Row(modifier = Modifier.fillMaxWidth()) {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "다시 선택할게요",
                        color = BillageTextSecondary,
                        fontWeight = FontWeight.Medium
                    )
                }

                Box(
                    modifier = Modifier
                        .size(1.dp, 48.dp)
                        .background(BillageDialogDivider)
                )

                TextButton(
                    onClick = onConfirm,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "확인했어요",
                        color = BillagePrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}