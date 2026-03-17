package com.billage.app.ui.contract.steps

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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.billage.app.ui.theme.BillageBackground
import com.billage.app.ui.theme.BillagePrimary
import com.billage.app.ui.theme.BillagePrimaryDisabled
import com.billage.app.ui.theme.BillagePrimaryLight
import com.billage.app.ui.theme.BillageTextPrimary
import com.billage.app.ui.theme.BillageTextSecondary

private data class PresetTerm(val emoji: String, val label: String)

private val PRESET_TERMS = listOf(
    PresetTerm("⏩", "중도 상환 허용"),
    PresetTerm("⏰", "연체 이자 부과"),
    PresetTerm("💳", "계좌이체로만 상환"),
    PresetTerm("🔒", "채권 양도 금지"),
    PresetTerm("🤫", "계약 내용 비밀 유지"),
    PresetTerm("⚖️", "분쟁 시 조정 우선")
)

@Composable
fun Step7SpecialTermsScreen(
    specialTerms: List<String>,
    onAddTerm: (String) -> Unit,
    onRemoveTerm: (String) -> Unit
) {
    var customInput by remember { mutableStateOf("") }
    var aiSuggestedTerm by remember { mutableStateOf<String?>(null) }

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
            text = "특별히 더 적을 게\n있나요?",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = BillageTextPrimary,
            lineHeight = 36.sp
        )

        Spacer(modifier = Modifier.height(32.dp))

        // 추가된 특약 섹션 — 선택된 항목이 있을 때만 표시
        if (specialTerms.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "추가된 특약",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = BillageTextPrimary
                )
                Box(
                    modifier = Modifier
                        .background(
                            color = BillagePrimary,
                            shape = RoundedCornerShape(50)
                        )
                        .padding(horizontal = 10.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "${specialTerms.size}개",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = BillagePrimaryLight,
                        shape = RoundedCornerShape(16.dp)
                    )
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                specialTerms.forEach { term ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = term,
                            fontSize = 14.sp,
                            color = BillageTextPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "✕",
                            fontSize = 14.sp,
                            color = BillageTextSecondary,
                            modifier = Modifier
                                .clickable { onRemoveTerm(term) }
                                .padding(4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        // 추천 특약 섹션
        Text(
            text = "추천 특약",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = BillageTextSecondary
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 추천 특약 3열 그리드
        PRESET_TERMS.chunked(3).forEach { rowTerms ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowTerms.forEach { preset ->
                    val fullTerm = "${preset.emoji} ${preset.label}"
                    val isSelected = specialTerms.contains(fullTerm)
                    SpecialTermChip(
                        modifier = Modifier.weight(1f),
                        emoji = preset.emoji,
                        label = preset.label,
                        isSelected = isSelected,
                        onClick = {
                            if (isSelected) onRemoveTerm(fullTerm)
                            else onAddTerm(fullTerm)
                        }
                    )
                }
                // 3열 맞춤용 빈 공간
                repeat(3 - rowTerms.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 직접 입력 섹션
        Text(
            text = "AI로 특약 추가하기",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = BillageTextPrimary
        )

        Spacer(modifier = Modifier.height(10.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = BillageBackground,
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                BasicTextField(
                    value = customInput,
                    onValueChange = { customInput = it },
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp),
                    textStyle = TextStyle(
                        fontSize = 14.sp,
                        color = BillageTextPrimary,
                        lineHeight = 20.sp
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    cursorBrush = SolidColor(BillagePrimary),
                    maxLines = 4,
                    decorationBox = { innerTextField ->
                        if (customInput.isEmpty()) {
                            Text(
                                text = "편하게 적으면 AI가 다시 정리해드려요!",
                                fontSize = 14.sp,
                                color = BillageTextSecondary,
                                lineHeight = 20.sp
                            )
                        }
                        innerTextField()
                    }
                )

                // TODO: AI 변환 API 연동 후 변환된 텍스트로 추가
                // 현재는 입력값 그대로 추가
                Box(
                    modifier = Modifier
                        .background(
                            color = if (customInput.isNotBlank()) BillagePrimary else BillagePrimaryDisabled,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .clickable(enabled = customInput.isNotBlank()) {
                            // TODO: AI API 연동 후 실제 정제 결과로 교체
                            // 현재는 입력값 그대로 제안
                            aiSuggestedTerm = customInput.trim()
                        }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "AI 정리",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                // AI 정리 결과 확인 UI
                if (aiSuggestedTerm != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = BillagePrimaryLight,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "이렇게 넣을까요?",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = BillageTextPrimary
                        )
                        Text(
                            text = aiSuggestedTerm!!,
                            fontSize = 14.sp,
                            color = BillageTextPrimary,
                            lineHeight = 20.sp
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // 취소
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(
                                        color = BillageBackground,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable { aiSuggestedTerm = null }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "다시 입력할게요",
                                    fontSize = 13.sp,
                                    color = BillageTextSecondary
                                )
                            }
                            // 확인 후 추가
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(
                                        color = BillagePrimary,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable {
                                        onAddTerm("🤖 ${aiSuggestedTerm!!}")
                                        aiSuggestedTerm = null
                                        customInput = ""
                                    }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "추가할게요",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SpecialTermChip(
    modifier: Modifier = Modifier,
    emoji: String,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .background(
                color = if (isSelected) BillagePrimary else BillagePrimaryLight,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable { onClick() }
            .padding(vertical = 12.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(text = emoji, fontSize = 20.sp)
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = if (isSelected) Color.White else BillagePrimary,
            textAlign = TextAlign.Center,
            lineHeight = 15.sp
        )
    }
}