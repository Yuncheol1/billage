package com.billage.app.ui.chat.components

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.billage.app.ui.theme.*
import com.billage.app.ui.contract.detail.formatRepaymentMethodFriendly
import com.billage.app.data.remote.api.ContractDetailResponse
import com.billage.app.data.remote.api.ContractUpdateRequest
import kotlinx.serialization.json.Json

import com.billage.app.domain.model.AiAnalysisResult

@Composable
fun ContractDocumentFullScreen(
    contractTitle: String,
    contractDetail: ContractDetailResponse,
    onDismissRequest: () -> Unit,
    onAccept: () -> Unit,
    onEdit: () -> Unit = {},
    isMeAccepted: Boolean = false,
    isOpponentAccepted: Boolean = false,
    originalPrincipal: Double? = null,
    originalInterestRate: Double? = null,
    proposedPrincipal: Double? = null,
    proposedInterestRate: Double? = null,
    isInitial: Boolean = false,
    contractAiAnalysis: AiAnalysisResult? = null,
    rawAiAnalysisJson: String? = null,
    isAiAnalyzing: Boolean = false,
    proposedModifications: List<String>? = null
) {
    var showModificationWarning by remember { mutableStateOf(false) }

    if (showModificationWarning) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showModificationWarning = false },
            title = { Text("수정 시 수락 초기화 안내", fontWeight = FontWeight.Bold) },
            text = { Text("계약 조건을 수정하면 현재의 모든 수락 상태가 초기화됩니다. 계속하시겠습니까?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showModificationWarning = false
                        onEdit()
                    }
                ) {
                    Text("네, 수정할게요", color = BillagePrimary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showModificationWarning = false }) {
                    Text("취소", color = BillageTextSecondary)
                }
            }
        )
    }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = BillageBackground
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .systemBarsPadding()
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismissRequest) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Close")
                    }
                    Text(
                        text = "차용증 전문 보기",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = BillageTextPrimary
                    )
                }

                HorizontalDivider(color = BillageDivider)

                // Tabs
                var selectedTab by remember { 
                    mutableIntStateOf(if (!isInitial && (originalPrincipal != null || originalInterestRate != null)) 1 else 0) 
                }
                
                // 제안서(AI 수정안 등)가 있는 경우 무조건 Diff 탭 표시를 허용 (사용자 요구)
                val tabs = if (isInitial && originalPrincipal == null && originalInterestRate == null) {
                    listOf("반영된 원문 보기")
                } else {
                    listOf("반영된 원문 보기", "변경사항 보기")
                }

                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = BillageBackground,
                    contentColor = BillagePrimary,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = BillagePrimary
                        )
                    }
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = {
                                Text(
                                    text = title,
                                    fontSize = 14.sp,
                                    fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selectedTab == index) BillagePrimary else BillageTextSecondary
                                )
                            }
                        )
                    }
                }

                HorizontalDivider(color = BillageDivider)

                // Content
                Box(modifier = Modifier.weight(1f).background(Color.White)) {
                    if (selectedTab == 1) { // Diff View
                        DocumentDiffView(
                            title = contractTitle, 
                            detail = contractDetail, 
                            providedOriginalPrincipal = originalPrincipal, 
                            providedOriginalInterest = originalInterestRate,
                            providedProposedPrincipal = proposedPrincipal,
                            providedProposedInterest = proposedInterestRate,
                            aiAnalysis = contractAiAnalysis, 
                            rawAiAnalysisJson = rawAiAnalysisJson, 
                            isAiAnalyzing = isAiAnalyzing,
                            proposedModifications = proposedModifications
                        )
                    } else { // Original View
                        DocumentOriginalView(contractTitle, contractDetail, contractAiAnalysis, rawAiAnalysisJson, isAiAnalyzing)
                    }
                }

                // Footer
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(BillageSurface)
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 수락 상태 표시 (NEW)
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        AcceptanceStatusBadge(label = contractDetail.myRealName ?: "나", isAccepted = isMeAccepted)
                        AcceptanceStatusBadge(label = "상대방", isAccepted = isOpponentAccepted)
                    }

                    // CTA 1: 수락하기 (Primary)
                    Button(
                        onClick = onAccept,
                        enabled = !isMeAccepted, // 이미 수락했으면 비활성화
                        colors = ButtonDefaults.buttonColors(containerColor = BillagePrimary, contentColor = Color.White),
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(if (isMeAccepted) "✅ 수락 완료" else "✅ 수락하기", fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // CTA 2: 조건 수정하기 (보조 액션)
                    OutlinedButton(
                        onClick = {
                            if (isMeAccepted || isOpponentAccepted) {
                                showModificationWarning = true
                            } else {
                                onEdit()
                            }
                        },
                        border = BorderStroke(1.dp, BillagePrimary),
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("✏️ 계약 조건 수정하기", color = BillagePrimary, fontWeight = FontWeight.Bold)
                    }

                    // CTA 3: 닫기 (이전 "나중에 결정하기")
                    TextButton(
                        onClick = onDismissRequest,
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                    ) {
                        Text("나중에 결정하기", color = BillageTextSecondary, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun AcceptanceStatusBadge(label: String, isAccepted: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("$label: ", fontSize = 13.sp, color = BillageTextSecondary)
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(if (isAccepted) BillagePrimary.copy(alpha = 0.1f) else Color.LightGray.copy(alpha = 0.2f))
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Text(
                text = if (isAccepted) "수락 완료" else "대기 중",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (isAccepted) BillagePrimary else BillageTextSecondary
            )
        }
    }
}

@Composable
fun TabButton(text: String, isSelected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clickable(onClick = onClick)
            .background(if (isSelected) Color.White else BillageSurface)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = text,
                color = if (isSelected) BillagePrimary else BillageTextSecondary,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                fontSize = 14.sp
            )
            if (isSelected) {
                Spacer(modifier = Modifier.height(4.dp))
                Box(modifier = Modifier.height(3.dp).width(40.dp).background(BillagePrimary))
            }
        }
    }
}

// Summary Header Component
@Composable
fun ContractSummaryHeader(detail: ContractDetailResponse) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 24.dp),
        colors = CardDefaults.cardColors(containerColor = BillageBackground),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, BillageDivider)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("계약 요약 정보", fontWeight = FontWeight.Bold, color = BillagePrimary, fontSize = 13.sp)
            Spacer(modifier = Modifier.height(12.dp))
            
            SummaryRow("채권자", detail.myRealName ?: "나")
            SummaryRow("채무자", detail.counterpartyName ?: "-")
            SummaryRow("차용 원금", "%,d원".format(detail.principalAmount?.toLong() ?: 0L))
            SummaryRow("이자율", "연 %.2f%%".format((detail.interestRate ?: 0.0) * 100))
            SummaryRow("차용 기간", "${detail.startDt ?: "-"} ~ ${detail.endDt ?: "-"}")
            SummaryRow("상환 방식", formatRepaymentMethodFriendly(detail.repaymentMethod, detail.totalInstallments))
        }
    }
}

// Unified Diff Summary Header (Maintains Original Structure)
@Composable
fun DiffContractSummaryHeader(
    detail: ContractDetailResponse,
    originalPrincipal: Long,
    proposedPrincipal: Long,
    originalInterest: Double,
    proposedInterest: Double
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 24.dp),
        colors = CardDefaults.cardColors(containerColor = BillageBackground),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, BillageDivider)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("계약 요약 정보 (변경사항)", fontWeight = FontWeight.Bold, color = BillagePrimary, fontSize = 13.sp)
            Spacer(modifier = Modifier.height(12.dp))
            
            SummaryRow("채권자", detail.myRealName ?: "나")
            SummaryRow("채무자", detail.counterpartyName ?: "-")

            // Principal Diff
            val isPrincipalChanged = originalPrincipal != proposedPrincipal
            if (isPrincipalChanged) {
                DiffSummaryRow(
                    label = "차용 원금",
                    original = "%,d원".format(originalPrincipal),
                    proposed = "%,d원".format(proposedPrincipal),
                    isChanged = true
                )
            } else {
                SummaryRow("차용 원금", "%,d원".format(proposedPrincipal))
            }

            // Interest Diff
            val isInterestChanged = Math.abs(originalInterest - proposedInterest) > 0.0001
            if (isInterestChanged) {
                DiffSummaryRow(
                    label = "이자율",
                    original = "연 %.2f%%".format(originalInterest * 100),
                    proposed = "연 %.2f%%".format(proposedInterest * 100),
                    isChanged = true
                )
            } else {
                SummaryRow("이자율", "연 %.2f%%".format(proposedInterest * 100))
            }

            SummaryRow("차용 기간", "${detail.startDt ?: "-"} ~ ${detail.endDt ?: "-"}")
            SummaryRow("상환 방식", formatRepaymentMethodFriendly(detail.repaymentMethod, detail.totalInstallments))
        }
    }
}

@Composable
fun DiffSummaryRow(label: String, original: String, proposed: String, isChanged: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = BillageTextSecondary, fontSize = 14.sp)
        if (isChanged) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(original, color = BillageTextSecondary, textDecoration = TextDecoration.LineThrough, fontSize = 13.sp)
                Icon(imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = BillagePrimary, modifier = Modifier.size(16.dp))
                Text(proposed, color = BillagePrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        } else {
            Text(proposed, color = BillageTextPrimary, fontWeight = FontWeight.Medium, fontSize = 14.sp)
        }
    }
}

@Composable
fun SummaryRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = BillageTextSecondary, fontSize = 14.sp)
        Text(value, color = BillageTextPrimary, fontWeight = FontWeight.Medium, fontSize = 14.sp)
    }
}

// AI Analysis Section Component
@Composable
fun AiAnalysisSection(
    analysisJson: AiAnalysisResult?,
    rawJson: String? = null,
    isLoading: Boolean = false
) {
    if (!isLoading && analysisJson == null && rawJson == null) return

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp, bottom = 32.dp),
        colors = CardDefaults.cardColors(containerColor = BillagePrimary.copy(alpha = 0.05f)),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, BillagePrimary.copy(alpha = 0.2f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("✨ AI 차용증 안심 검토", fontWeight = FontWeight.ExtraBold, color = BillagePrimary, fontSize = 16.sp)
            }
            Spacer(modifier = Modifier.height(16.dp))
            
            if (isLoading) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(color = BillagePrimary, modifier = Modifier.size(32.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("AI가 차용증을 꼼꼼하게 분석하고 있어요...", fontSize = 13.sp, color = BillageTextSecondary)
                }
            } else if (analysisJson != null) {
                // 안전 상태 표시
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (analysisJson.is_safe) BillagePrimary.copy(alpha = 0.1f) else BillageDanger.copy(alpha = 0.1f))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (analysisJson.is_safe) "✅ 이 계약은 안전해 보여요" else "⚠️ 주의가 필요한 항목이 있어요",
                        fontWeight = FontWeight.Bold,
                        color = if (analysisJson.is_safe) BillagePrimary else BillageDanger,
                        fontSize = 14.sp
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 요약 텍스트
                Text(
                    text = analysisJson.summary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = BillageTextPrimary,
                    lineHeight = 22.sp
                )

                if (analysisJson.warnings.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(20.dp))
                    Text("검토 의견", fontWeight = FontWeight.Bold, color = BillageTextPrimary, fontSize = 15.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    analysisJson.warnings.forEach { warning ->
                        AiWarningItem(warning)
                    }
                }

                if (analysisJson.tax_summary != null) {
                    Spacer(modifier = Modifier.height(20.dp))
                    Text("세무 리스크 및 가이드", fontWeight = FontWeight.Bold, color = BillageTextPrimary, fontSize = 15.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    AiTaxSummaryCard(analysisJson.tax_summary)
                }
            } else if (rawJson != null) {
                // 파싱 실패 시 원문이라도 표시 (기존 유저 요구사항 대응)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(BillageDanger.copy(alpha = 0.05f))
                        .border(1.dp, BillageDanger.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    Text("⚠️ 정밀 분석 리포트 생성 중 오류", color = BillageDanger, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(rawJson, fontSize = 12.sp, color = BillageTextPrimary, lineHeight = 18.sp)
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "* AI 검토 결과는 법적 효력을 보장하지 않으며 참고용으로만 활용해 주세요.",
                color = BillageTextSecondary,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
fun AiWarningItem(warning: com.billage.app.domain.model.AiWarning) {
    val riskColor = when(warning.risk_level) {
        "HIGH" -> BillageDanger
        "MEDIUM" -> Color(0xFFF2994A)
        else -> BillagePrimary
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .border(1.dp, BillageDivider, RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(riskColor.copy(alpha = 0.1f))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(warning.risk_level, color = riskColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(warning.category, fontWeight = FontWeight.Bold, color = BillageTextPrimary, fontSize = 14.sp)
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(warning.reason, fontSize = 13.sp, color = BillageTextPrimary, lineHeight = 20.sp)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "💡 제안: ${warning.suggestion}",
            fontSize = 13.sp,
            color = BillagePrimary,
            fontWeight = FontWeight.Medium,
            lineHeight = 20.sp
        )
    }
}

@Composable
fun AiTaxSummaryCard(tax: com.billage.app.domain.model.AiTaxSummary) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .border(1.dp, BillageDivider, RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("증여세 리스크", color = BillageTextSecondary, fontSize = 13.sp)
            Text(
                text = if (tax.gift_tax_risk) "⚠️ 발생 가능성 있음" else "✅ 낮음",
                color = if (tax.gift_tax_risk) BillageDanger else BillagePrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("원천징수 세율", color = BillageTextSecondary, fontSize = 13.sp)
            Text(tax.withholding_tax_rate ?: "-", color = BillageTextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
    }
}

// Diff View
@Composable
fun DocumentDiffView(
    title: String, 
    detail: ContractDetailResponse?,
    providedOriginalPrincipal: Double? = null,
    providedOriginalInterest: Double? = null,
    providedProposedPrincipal: Double? = null,
    providedProposedInterest: Double? = null,
    aiAnalysis: com.billage.app.domain.model.AiAnalysisResult? = null,
    rawAiAnalysisJson: String? = null,
    isAiAnalyzing: Boolean = false,
    proposedModifications: List<String>? = null
) {
    val delStyle = SpanStyle(
        color = Color(0xFFCB2431),
        background = Color(0xFFFFEEF0),
        textDecoration = TextDecoration.LineThrough,
        fontWeight = FontWeight.Bold
    )
    val addStyle = SpanStyle(
        color = Color(0xFF22863A),
        background = Color(0xFFE6FFEC),
        fontWeight = FontWeight.Bold
    )

    val proposal = remember(detail) {
        detail?.aiDraftContent?.let {
            try {
                Json.decodeFromString<ContractUpdateRequest>(it)
            } catch (e: Exception) { null }
        }
    }

    val counterparty = detail?.counterpartyName ?: "김철수"
    
    // 1. Original (Baseline): payload에서 명시적으로 넘어온 값을 우선하고, 없으면 detail(현재 상태)을 사용
    val originalPrincipal = providedOriginalPrincipal?.toLong() ?: detail?.principalAmount?.toLong() ?: 0L
    val originalInterest = providedOriginalInterest ?: detail?.interestRate ?: 0.0
    
    // 2. Proposed (Target): payload에서 명시적으로 넘어온 "제안값"을 최우선함. (이게 있어야 DB가 바뀌어도 Diff가 유지됨)
    // payload에도 없으면 detail.aiDraftContent(수정 제안본)을 파싱해서 사용
    val propPrincipal = providedProposedPrincipal?.toLong() 
        ?: proposal?.principalAmount?.toLong() 
        ?: detail?.principalAmount?.toLong() 
        ?: originalPrincipal
        
    val propInterest = providedProposedInterest 
        ?: proposal?.interestRate 
        ?: detail?.interestRate 
        ?: originalInterest

    // 강제 확인: 만약 providedOriginalInterest가 있고(예: 4.6), propInterest가 다르면(예: 4.5), 
    // detail.interestRate이 무엇이든 간에 originalInterest != propInterest는 참이어야 함.

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        Text("금전소비대차계약서 (변경사항)", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center, color = BillageTextPrimary)
        if (title.isNotBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text("No. $title", fontSize = 11.sp, color = BillageTextSecondary, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        }
        Spacer(modifier = Modifier.height(24.dp))

        // 1. 변경사항 요약 헤더 (원문 요약과 동일한 스타일 유지)
        if (detail != null) {
            DiffContractSummaryHeader(
                detail = detail,
                originalPrincipal = originalPrincipal,
                proposedPrincipal = propPrincipal,
                originalInterest = originalInterest,
                proposedInterest = propInterest
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))

        // ADD: proposedModifications 기반의 추가 렌더링 로직
        if (!proposedModifications.isNullOrEmpty()) {
            Text(
                text = "제안된 조항 변경사항", 
                fontWeight = FontWeight.ExtraBold, 
                fontSize = 18.sp, 
                color = BillagePrimary,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            proposedModifications.forEach { mod ->
                val isAdd = mod.startsWith("ADD:")
                val isDel = mod.startsWith("DEL:")
                val displayMod = mod.removePrefix("ADD:").removePrefix("DEL:").trim()
                
                if (isAdd || isDel) {
                    val contentColor = if (isAdd) BillagePrimary else BillageDanger
                    
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(contentColor.copy(alpha = 0.05f))
                            .border(1.dp, contentColor.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isAdd) Icons.Default.Add else Icons.Default.Remove,
                                contentDescription = null,
                                tint = contentColor,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isAdd) "추가/수정됨" else "삭제/수정전",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = contentColor
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = displayMod,
                            fontSize = 14.sp,
                            lineHeight = 22.sp,
                            color = BillageTextPrimary,
                            textDecoration = if (isDel) TextDecoration.LineThrough else null
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
            HorizontalDivider(color = BillageDivider, modifier = Modifier.padding(vertical = 16.dp))
        }

        val displayClauses = if (!proposal?.clauses.isNullOrEmpty()) {
            proposal?.clauses ?: emptyList()
        } else {
            // proposal.clauses가 없거나 비어있으면 원본 특약을 보여주어 '변경 없음' 상태임을 알림
            detail?.clauses ?: emptyList()
        }
        
        if (displayClauses.isEmpty()) {
            Text("설정된 특약사항이 없습니다.", fontSize = 14.sp, color = BillageTextSecondary, modifier = Modifier.padding(vertical = 16.dp))
        } else {
            Text("본 계약의 각 조항은 다음과 같이 구성되며, 채권자와 채무자 '$counterparty'는 상기 요약 정보와 아래 조항에 동의한다.", fontSize = 14.sp, color = BillageTextPrimary, lineHeight = 22.sp)
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // 특약사항들 (번호는 1조부터 다시 매김)
        displayClauses.forEachIndexed { index, propClauseOrDetail ->
            // propClauseOrDetail은 ClauseUpdateDto이거나 ClauseDetailDto일 수 있음 (둘 다 content 가짐)
            val clauseContent = when (propClauseOrDetail) {
                is com.billage.app.data.remote.api.ClauseUpdateDto -> propClauseOrDetail.content
                is com.billage.app.data.remote.api.ClauseDetailDto -> propClauseOrDetail.content
                else -> ""
            }
            val clauseSeq = when (propClauseOrDetail) {
                is com.billage.app.data.remote.api.ClauseUpdateDto -> propClauseOrDetail.clauseSeq
                is com.billage.app.data.remote.api.ClauseDetailDto -> propClauseOrDetail.clauseSeq
                else -> -1
            }

            val origClause = detail?.clauses?.find { it.clauseSeq == clauseSeq }
            val isModified = origClause != null && origClause.content.trim() != clauseContent.trim()
            val isNew = origClause == null && proposal?.clauses?.any { (it as? com.billage.app.data.remote.api.ClauseUpdateDto)?.clauseSeq == clauseSeq } == true

            Text(
                text = "제${index + 1}조 (특약)${if (isNew) " [신규]" else if (isModified) " [수정됨]" else ""}", 
                fontWeight = FontWeight.Bold, 
                fontSize = 16.sp, 
                color = if (isNew) BillagePrimary else if (isModified) Color(0xFFCB2431) else BillageTextPrimary
            )
            
            if (isModified) {
                Text(buildAnnotatedString {
                    withStyle(delStyle) { append(origClause!!.content) }
                    append("\n")
                    withStyle(addStyle) { append(clauseContent) }
                }, fontSize = 14.sp, lineHeight = 24.sp, modifier = Modifier.padding(top = 8.dp))
            } else if (isNew) {
                Text(buildAnnotatedString {
                    withStyle(addStyle) { append(clauseContent) }
                }, fontSize = 14.sp, lineHeight = 24.sp, modifier = Modifier.padding(top = 8.dp))
            } else {
                Text(clauseContent, fontSize = 14.sp, lineHeight = 24.sp, modifier = Modifier.padding(top = 8.dp), color = BillageTextPrimary)
            }
            Spacer(modifier = Modifier.height(20.dp))
        }

        // 2. AI 분석 리포트 제거 (사용자 요청)
        // AiAnalysisSection(aiAnalysis, rawAiAnalysisJson, isAiAnalyzing)

        Spacer(modifier = Modifier.height(32.dp))
    }
}

// Original Content View
@Composable
fun DocumentOriginalView(
    title: String, 
    detail: ContractDetailResponse?, 
    aiAnalysis: com.billage.app.domain.model.AiAnalysisResult? = null, 
    rawAiAnalysisJson: String? = null,
    isAiAnalyzing: Boolean = false
) {
    val proposal = remember(detail) {
        detail?.aiDraftContent?.let {
            try {
                Json.decodeFromString<ContractUpdateRequest>(it)
            } catch (e: Exception) { null }
        }
    }

    val counterparty = detail?.counterpartyName ?: "김철수"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        Text("금전소비대차계약서", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center, color = BillageTextPrimary)
        if (title.isNotBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text("No. $title", fontSize = 11.sp, color = BillageTextSecondary, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        }
        Spacer(modifier = Modifier.height(24.dp))

        // 1. 요약 정보 카드
        detail?.let { ContractSummaryHeader(it) }

        Spacer(modifier = Modifier.height(24.dp))
        Text("본 계약의 각 조항은 다음과 같이 구성되며, 채권자와 채무자 '$counterparty'는 상기 요약 정보와 아래 조항에 동의한다.", fontSize = 14.sp, color = BillageTextPrimary, lineHeight = 22.sp)
        
        Spacer(modifier = Modifier.height(32.dp))
        
        val displayClauses = proposal?.clauses?.map { it.content } 
            ?: detail?.clauses?.map { it.content } 
            ?: emptyList()
        
        if (displayClauses.isEmpty()) {
            Text("설정된 특약사항이 없습니다.", fontSize = 14.sp, color = BillageTextSecondary, modifier = Modifier.padding(vertical = 16.dp))
        } else {
            displayClauses.forEachIndexed { index, content ->
                Text("제${index + 1}조 (특약)", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = BillageTextPrimary)
                Text(content, fontSize = 14.sp, lineHeight = 24.sp, modifier = Modifier.padding(top = 8.dp), color = BillageTextPrimary)
                Spacer(modifier = Modifier.height(20.dp))
            }
        }

        // 2. AI 분석 리포트
        AiAnalysisSection(aiAnalysis, rawAiAnalysisJson, isAiAnalyzing)

        Spacer(modifier = Modifier.height(32.dp))
    }
}
