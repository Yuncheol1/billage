package com.billage.app.ui.contract

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.billage.app.ui.theme.*

@Composable
fun ContractSpecialTermsScreen(
    onSkip: () -> Unit,      // 건너뛰기 → ContractCompleteScreen
    onComplete: () -> Unit,  // 완료하기 → ContractCompleteScreen (이미 저장 완료된 상태)
    viewModel: ContractSpecialTermsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        containerColor = BillageBackground
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            // 상단 헤더 + 건너뛰기
            SpecialTermsHeader(onSkip = onSkip)

            // 추가된 특약 목록 (있을 때만 표시)
            AnimatedVisibility(
                visible = uiState.addedTerms.isNotEmpty(),
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(8.dp))
                    AddedTermsSection(
                        terms = uiState.addedTerms,
                        onRemove = { viewModel.removeTerm(it) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 추천 특약 버튼 그리드
            RecommendedTermsSection(
                recommendedTerms = viewModel.recommendedTerms,
                addedTermTitles = uiState.addedTerms
                    .filter { it.isRecommended }
                    .map { it.title },
                isLoading = uiState.isLoading,
                onToggle = { viewModel.addRecommendedTerm(it) }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 직접 입력 섹션
            DirectInputSection(
                inputText = uiState.directInputText,
                onInputChange = { viewModel.updateDirectInputText(it) },
                isLoading = uiState.isLoading,
                onAddClick = { viewModel.addDirectInputTerm() }
            )

            // 에러 메시지
            if (uiState.errorMessage != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = uiState.errorMessage!!,
                    color = BillageDanger,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // 완료하기 버튼 — 이미 특약이 저장된 상태이므로 API 호출 없이 이동
            Button(
                onClick = onComplete,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .padding(horizontal = 24.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BillagePrimary)
            ) {
                Text(
                    text = "완료하기",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SpecialTermsHeader(onSkip: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                text = "계약 만들기",
                color = BillagePrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "특별히 더 적을 게\n있나요?",
                color = BillageTextPrimary,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 30.sp
            )
        }
        TextButton(onClick = onSkip) {
            Text(
                text = "건너뛰기",
                color = BillageTextSecondary,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
private fun AddedTermsSection(
    terms: List<SpecialTerm>,
    onRemove: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = BillagePrimaryLight),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "추가된 특약",
                    color = BillageTextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(BillagePrimary)
                        .padding(horizontal = 10.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "${terms.size}개",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            terms.forEach { term ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = term.title,
                        color = BillageTextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = { onRemove(term.id) },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "삭제",
                            tint = BillageTextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RecommendedTermsSection(
    recommendedTerms: List<RecommendedTermItem>,
    addedTermTitles: List<String>,
    isLoading: Boolean,
    onToggle: (RecommendedTermItem) -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 24.dp)) {
        Text(
            text = "추천 특별약관",
            color = BillageTextSecondary,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(12.dp))

        // FlowRow 대신 — 3개씩 Row로 직접 배치
        val rows = recommendedTerms.chunked(3) // 3개씩 묶기
        rows.forEach { rowItems ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                rowItems.forEach { item ->
                    val isAdded = addedTermTitles.contains(item.label)
                    RecommendedTermChip(
                        item = item,
                        isAdded = isAdded,
                        onClick = { if (!isLoading && !isAdded) onToggle(item) },
                        modifier = Modifier.weight(1f)
                    )
                }
                // 마지막 행 아이템이 3개 미만이면 빈 공간으로 채우기
                repeat(3 - rowItems.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

@Composable
private fun RecommendedTermChip(
    item: RecommendedTermItem,
    isAdded: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isAdded) BillagePrimaryLight else BillageSurface
        ),
        border = BorderStroke(
            width = if (isAdded) 1.5.dp else 1.dp,
            color = if (isAdded) BillagePrimary else BillageCardBorder
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = item.emoji, fontSize = 22.sp)
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = item.label,
                color = if (isAdded) BillagePrimary else BillageTextPrimary,
                fontSize = 12.sp,
                fontWeight = if (isAdded) FontWeight.Bold else FontWeight.Medium,
                textAlign = TextAlign.Center,
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
private fun DirectInputSection(
    inputText: String,
    onInputChange: (String) -> Unit,
    isLoading: Boolean,
    onAddClick: () -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 24.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "직접 입력",
                color = BillageTextSecondary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.width(10.dp))
            // AI 변환 배지 — 탭하면 입력 내용을 BE로 전송해 AI가 처리
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(BillagePrimary)
                    .clickable(
                        enabled = inputText.isNotBlank() && !isLoading,
                        onClick = onAddClick
                    )
                    .padding(horizontal = 12.dp, vertical = 5.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(12.dp),
                            color = Color.White,
                            strokeWidth = 1.5.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isLoading) "저장 중..." else "AI 변환",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = inputText,
            onValueChange = onInputChange,
            modifier = Modifier
                .fillMaxWidth()
                .height(110.dp),
            placeholder = {
                Text(
                    text = "사연어로 편하게 적어요\n예) 늦으면 이자 더 낼게",
                    color = BillageTextSecondary,
                    fontSize = 14.sp
                )
            },
            enabled = !isLoading,
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = BillagePrimary,
                unfocusedBorderColor = BillageCardBorder,
                focusedContainerColor = BillageSurface,
                unfocusedContainerColor = BillageSurface
            )
        )
    }
}