package com.billage.app.ui.contract.create

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
fun ContractCreateCompleteScreen(
    formState: ContractFormState,
    onHomeClick: () -> Unit,
    onChatClick: () -> Unit,
    role: ContractRole = ContractRole.CREDITOR
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
            text = "계약 초안이 만들어졌어요!",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = BillageTextPrimary
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "${formState.debtorName}님과의 채팅방이 열렸어요\n초안을 확인하고 계약을 완성해보세요",
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
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 채팅방으로 이동 버튼
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = BillagePrimary,
                    shape = RoundedCornerShape(16.dp)
                )
                .clickable {
                    // TODO: 생성된 계약 채팅방으로 이동
                    onChatClick()
                }
                .padding(vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(text = "💬", fontSize = 16.sp)
                Text(
                    text = "채팅방으로 이동",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = androidx.compose.ui.graphics.Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 이제 이런 순서로 진행돼요
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "이제 이런 순서로 진행돼요",
                fontSize = 13.sp,
                color = BillageTextSecondary
            )

            listOf(
                Triple("💬", "채팅방에서 초안을 확인해요", "상대방과 계약 내용을 자유롭게 조율할 수 있어요"),
                Triple("✏️", "특약을 추가할 수 있어요", "필요한 조건이 있으면 특약으로 추가해요"),
                Triple("✍️", "전자서명으로 계약을 확정해요", "양쪽 모두 서명하면 법적 효력이 생겨요"),
                Triple("🔄", "상환 관리는 자동으로 해드려요", "정해진 날짜에 자동이체와 알림이 작동해요")
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