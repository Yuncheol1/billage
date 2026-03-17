package com.billage.app.ui.contract

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.billage.app.ui.theme.BillageBackground
import com.billage.app.ui.theme.BillageCardBorder
import com.billage.app.ui.theme.BillageDivider
import com.billage.app.ui.theme.BillagePrimary
import com.billage.app.ui.theme.BillagePrimaryLight
import com.billage.app.ui.theme.BillageSurface
import com.billage.app.ui.theme.BillageTextPrimary
import com.billage.app.ui.theme.BillageTextSecondary
import com.billage.app.ui.theme.KakaoTextBrown
import com.billage.app.ui.theme.KakaoYellow
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd")
private val DISPLAY_FORMATTER = DateTimeFormatter.ofPattern("yyyy. M. d.")

private fun formatAmount(amount: String): String {
    val number = amount.toLongOrNull() ?: return "-"
    val eok = number / 100_000_000L
    val man = (number % 100_000_000L) / 10_000L
    val rest = number % 10_000L
    return buildString {
        if (eok > 0) append("${eok}억 ")
        if (man > 0) append("${man}만 ")
        if (rest > 0) append(rest)
    }.trim().ifEmpty { "-" } + "원"
}

private fun formatDate(dateStr: String): String {
    return runCatching {
        LocalDate.parse(dateStr, DATE_FORMATTER).format(DISPLAY_FORMATTER)
    }.getOrDefault("-")
}

private fun formatRepaymentMethod(method: RepaymentMethod?): String = when (method) {
    RepaymentMethod.LUMP_SUM -> "만기 일시 상환"
    RepaymentMethod.MONTHLY -> "월별 분할 상환"
    RepaymentMethod.QUARTERLY -> "분기별 분할 상환"
    RepaymentMethod.SEMI_ANNUAL -> "반기별 분할 상환"
    RepaymentMethod.ANNUAL -> "연별 분할 상환"
    null -> "-"
}

@Composable
fun ContractCompleteScreen(
    formState: ContractFormState,
    onHomeClick: () -> Unit,
    onKakaoClick: () -> Unit,
    onSmsClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(color = BillageBackground)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
            .padding(top = 48.dp, bottom = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 완료 아이콘
        Box(
            modifier = Modifier
                .size(72.dp)
                .background(
                    color = BillagePrimaryLight,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "✓", fontSize = 32.sp, color = BillagePrimary)
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "계약 내용이 준비됐어요!",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = BillageTextPrimary
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "${formState.debtorName}님에게 알림을 보내\n수락을 요청해보세요",
            fontSize = 14.sp,
            color = BillageTextSecondary,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )

        Spacer(modifier = Modifier.height(28.dp))

        // 계약 요약 카드
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = BillageSurface,
                    shape = RoundedCornerShape(16.dp)
                )
                .border(
                    width = 1.dp,
                    color = BillageCardBorder,
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "계약 요약",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = BillageTextSecondary
            )

            HorizontalDivider(color = BillageDivider)

            SummaryRow(label = "금액", value = formatAmount(formState.amount))
            SummaryRow(label = "상대방", value = formState.debtorName.ifBlank { "-" })
            SummaryRow(label = "만기일", value = formatDate(formState.endDate))
            SummaryRow(
                label = "이자",
                value = if (formState.interestRate == "0") "무이자"
                else "연 ${formState.interestRate}%"
            )
            SummaryRow(
                label = "상환",
                value = formatRepaymentMethod(formState.repaymentMethod)
            )
            SummaryRow(
                label = "특약",
                value = if (formState.specialTerms.isEmpty()) "없음"
                else "${formState.specialTerms.size}개"
            )

            // 특약 목록 (있을 때만 표시)
            if (formState.specialTerms.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = BillagePrimaryLight,
                            shape = RoundedCornerShape(10.dp)
                        )
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    formState.specialTerms.forEach { term ->
                        Text(
                            text = "• $term",
                            fontSize = 13.sp,
                            color = BillageTextPrimary,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 카카오톡 / 문자 버튼
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 카카오톡 버튼
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(
                        color = KakaoYellow,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .clickable {
                        // TODO: 카카오톡 공유 SDK 연동
                        onKakaoClick()
                    }
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(text = "💬", fontSize = 16.sp)
                    Text(
                        text = "카카오톡",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = KakaoTextBrown
                    )
                }
            }

            // 문자 버튼
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(
                        color = BillageSurface,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .border(
                        width = 1.dp,
                        color = BillageCardBorder,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .clickable {
                        // TODO: 문자 공유 연동
                        onSmsClick()
                    }
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(text = "📱", fontSize = 16.sp)
                    Text(
                        text = "문자",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = BillageTextPrimary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 보내면 이런 순서로 진행돼요
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "보내면 이런 순서로 진행돼요",
                fontSize = 13.sp,
                color = BillageTextSecondary
            )

            listOf(
                Triple("📱", "초대를 수락해요", "링크를 누르면 빌리지에 가입할 수 있어요"),
                Triple("💬", "채팅방에서 조건을 확인해요", "계약 내용에 대해 자유롭게 대화할 수 있어요"),
                Triple("✍️", "전자서명으로 계약을 완성해요", "양쪽 모두 서명하면 법적 효력이 생겨요"),
                Triple("🔄", "자동이체로 상환이 시작돼요", "정해진 날짜에 맞춰 자동으로 이체돼요")
            ).forEach { (emoji, title, description) ->
                Row(
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(text = emoji, fontSize = 18.sp)
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = title,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = BillageTextPrimary
                        )
                        Text(
                            text = description,
                            fontSize = 12.sp,
                            color = BillageTextSecondary,
                            lineHeight = 17.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // 홈으로 가기 버튼
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = BillageSurface,
                    shape = RoundedCornerShape(16.dp)
                )
                .border(
                    width = 1.dp,
                    color = BillageCardBorder,
                    shape = RoundedCornerShape(16.dp)
                )
                .clickable { onHomeClick() }
                .padding(vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "홈으로 가기",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = BillageTextPrimary
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 차용증 전문 보기
        Text(
            text = "차용증 전문 보기",
            fontSize = 13.sp,
            color = BillageTextSecondary,
            modifier = Modifier
                .clickable {
                    // TODO: 차용증 전문 보기 연동
                }
                .padding(vertical = 8.dp)
        )
    }
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = BillageTextSecondary
        )
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = BillageTextPrimary
        )
    }
}