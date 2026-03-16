package com.billage.app.ui.contract.steps

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.billage.app.ui.theme.BillagePrimary
import com.billage.app.ui.theme.BillagePrimaryLight
import com.billage.app.ui.theme.BillageTextPrimary
import com.billage.app.ui.theme.BillageTextSecondary

@Composable
fun Step1FamilyScreen(
    isFamily: Boolean?,
    onSelect: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .padding(top = 32.dp)
    ) {
        // ── 소제목 ──────────────────────────────
        Text(
            text = "계약 만들기",
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = BillagePrimary
        )

        Spacer(modifier = Modifier.height(8.dp))

        // ── 메인 질문 (한 줄) ────────────────────
        Text(
            text = "누구에게 빌려주시나요?",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = BillageTextPrimary,
            lineHeight = 36.sp
        )

        Spacer(modifier = Modifier.height(40.dp))

        // ── 선택 카드 2열 가로 배치 ─────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FamilyOptionCard(
                modifier = Modifier.weight(1f),
                emoji = "🏠",
                title = "가족",
                isSelected = isFamily == true,
                onClick = { onSelect(true) }
            )
            FamilyOptionCard(
                modifier = Modifier.weight(1f),
                emoji = "🤝",
                title = "가족 외",
                isSelected = isFamily == false,
                onClick = { onSelect(false) }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ── 가족 선택 시 안내 박스 ──────────────
        // ── 선택에 따라 안내 박스 교체 ──────────
// AnimatedContent — isFamily 값이 바뀔 때마다 위→아래 방향으로 통일
        AnimatedContent(
            targetState = isFamily,
            transitionSpec = {
                (fadeIn() + expandVertically()) togetherWith (fadeOut() + shrinkVertically())
            },
            label = "FamilyInfoBox"
        ) { state ->
            when (state) {
                true -> InfoBox(
                    title = "💡 이런 분들도 가족이에요",
                    rows = listOf(
                        "4촌 이내 혈족" to "부모님, 자녀, 형제자매, 조부모, 고모, 삼촌 등",
                        "3촌 이내 인척" to "시부모, 장인·장모, 형수, 매형 등"
                    )
                )
                false -> InfoBox(
                    title = "🤝 이런 분들과의 계약이에요",
                    rows = listOf(
                        "지인·친구" to "친구, 오랜 지인 등",
                        "직장 관계" to "동료, 상사, 부하직원 등",
                        "사업 관계" to "동업자, 거래처 등"
                    )
                )
                null -> Unit // 아무것도 선택 안 한 상태
            }
        }
    }
}

// ── 공통 안내 박스 ──────────────────────────────
// title과 rows(레이블 + 내용 쌍)를 받아서 렌더링한다
@Composable
private fun InfoBox(
    title: String,
    rows: List<Pair<String, String>>
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = BillagePrimaryLight,
                shape = RoundedCornerShape(16.dp)
            )
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Text(
            text = title,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = BillageTextPrimary
        )

        Spacer(modifier = Modifier.height(10.dp))

        rows.forEachIndexed { index, (label, content) ->
            if (index > 0) Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically // Top → CenterVertically로 높이 정렬
            ) {
                Text(
                    text = label,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = BillagePrimary,
                    modifier = Modifier.width(90.dp)
                )
                Text(
                    text = content,
                    fontSize = 12.sp,
                    color = BillageTextSecondary,
                    lineHeight = 18.sp,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

// ── 선택 카드 컴포넌트 ──────────────────────────
@Composable
private fun FamilyOptionCard(
    modifier: Modifier = Modifier,
    emoji: String,
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) BillagePrimary else Color(0xFFE0E8E6)
        ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) BillagePrimaryLight else Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 28.dp, horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .background(
                        color = Color(0xFFF2F4F3),
                        shape = RoundedCornerShape(14.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(text = emoji, fontSize = 26.sp)
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = BillageTextPrimary
            )
        }
    }
}