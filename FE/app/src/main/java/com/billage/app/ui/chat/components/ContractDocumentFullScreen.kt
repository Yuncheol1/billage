package com.billage.app.ui.chat.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.billage.app.data.remote.api.ContractDetailResponse
import com.billage.app.data.remote.api.ContractUpdateRequest
import kotlinx.serialization.json.Json

@Composable
fun ContractDocumentFullScreen(
    contractTitle: String,
    contractDetail: ContractDetailResponse,
    onDismissRequest: () -> Unit,
    onAccept: () -> Unit,
    onEdit: () -> Unit = {},
    isMeAccepted: Boolean = false,
    isOpponentAccepted: Boolean = false
) {
    var showEditLockAlert by remember { mutableStateOf(false) }

    if (showEditLockAlert) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showEditLockAlert = false },
            title = { Text("수정 불가", fontWeight = FontWeight.Bold) },
            text = { Text("이미 양측이 모든 조건에 수락한 계약입니다.\n수정하시려면 상대방에게 수정 요청을 하시거나 채팅으로 대화해주세요.") },
            confirmButton = {
                TextButton(onClick = { showEditLockAlert = false }) {
                    Text("확인", color = BillagePrimary)
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
                var selectedTab by remember { mutableIntStateOf(0) }
                val tabs = listOf("반영된 원문 보기", "변경사항(Diff) 보기")

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
                        DocumentDiffView(contractTitle, contractDetail)
                    } else { // Original View
                        DocumentOriginalView(contractTitle, contractDetail)
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
                        AcceptanceStatusBadge(label = "나", isAccepted = isMeAccepted)
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
                            if (isMeAccepted && isOpponentAccepted) {
                                showEditLockAlert = true
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

// Diff View
@Composable
fun DocumentDiffView(title: String, detail: ContractDetailResponse?) {
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
    val originalPrincipal = detail?.principalAmount?.toLong() ?: 0L
    val originalInterest = detail?.interestRate ?: 0.0

    val propPrincipal = proposal?.principalAmount?.toLong() ?: originalPrincipal
    val propInterest = proposal?.interestRate ?: originalInterest

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        Text("금전소비대차계약서", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        if (title.isNotBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text("No. $title", fontSize = 11.sp, color = Color(0xFF999999), modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        }
        Spacer(modifier = Modifier.height(20.dp))

        Text("채권자와 채무자 '$counterparty'는 빌리지(Billage) 서비스를 통하여 다음과 같이 금전소비대차계약을 체결한다.", fontSize = 14.sp, lineHeight = 22.sp)
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text("제1조 (차용 원금)", fontWeight = FontWeight.Bold, fontSize = 15.sp)
        Text(buildAnnotatedString {
            append("원금은 일금 ")
            if (originalPrincipal != propPrincipal) {
                withStyle(delStyle) { append("%,d원".format(originalPrincipal)) }
                append(" ")
                withStyle(addStyle) { append("%,d원".format(propPrincipal)) }
            } else {
                append("%,d원".format(originalPrincipal))
            }
            append("으로 정한다.")
        }, fontSize = 14.sp, lineHeight = 24.sp, modifier = Modifier.padding(top = 4.dp))

        Spacer(modifier = Modifier.height(16.dp))

        Text("제2조 (기본 이자율)", fontWeight = FontWeight.Bold, fontSize = 15.sp)
        Text(buildAnnotatedString {
            append("본 계약의 기본 이자율은 ")
            if (originalInterest != propInterest) {
                withStyle(delStyle) { append("연 %.2f%%".format(originalInterest)) }
                append(" ")
                withStyle(addStyle) { append("연 %.2f%%".format(propInterest)) }
            } else {
                append("연 %.2f%%".format(originalInterest))
            }
            append("로 정한다.")
        }, fontSize = 14.sp, lineHeight = 24.sp, modifier = Modifier.padding(top = 4.dp))

        Spacer(modifier = Modifier.height(16.dp))

        proposal?.clauses?.forEachIndexed { index, propClause ->
            val origClause = detail?.clauses?.find { it.clauseSeq == propClause.clauseSeq }
            Text("제${index + 3}조 (특약)", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            if (origClause != null && origClause.content != propClause.content) {
                Text(buildAnnotatedString {
                    withStyle(delStyle) { append(origClause.content) }
                    append("\n")
                    withStyle(addStyle) { append(propClause.content) }
                }, fontSize = 14.sp, lineHeight = 24.sp, modifier = Modifier.padding(top = 4.dp))
            } else {
                Text(propClause.content, fontSize = 14.sp, lineHeight = 24.sp, modifier = Modifier.padding(top = 4.dp))
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

// Original Content View
@Composable
fun DocumentOriginalView(title: String, detail: ContractDetailResponse?) {
    val proposal = remember(detail) {
        detail?.aiDraftContent?.let {
            try {
                Json.decodeFromString<ContractUpdateRequest>(it)
            } catch (e: Exception) { null }
        }
    }

    val propPrincipal = proposal?.principalAmount?.toLong() ?: detail?.principalAmount?.toLong() ?: 0L
    val propInterest = proposal?.interestRate ?: detail?.interestRate ?: 0.0
    val counterparty = detail?.counterpartyName ?: "김철수"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        Text("금전소비대차계약서", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        if (title.isNotBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text("No. $title", fontSize = 11.sp, color = Color(0xFF999999), modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        }
        Spacer(modifier = Modifier.height(20.dp))
        Text("채권자와 채무자 '$counterparty'는 빌리지(Billage) 서비스를 통하여 다음과 같이 금전소비대차계약을 체결한다.", fontSize = 14.sp, lineHeight = 22.sp)
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text("제1조 (차용 원금)", fontWeight = FontWeight.Bold, fontSize = 15.sp)
        Text("원금은 일금 %,d원으로 정한다.".format(propPrincipal), fontSize = 14.sp, lineHeight = 24.sp, modifier = Modifier.padding(top = 4.dp))

        Spacer(modifier = Modifier.height(16.dp))

        Text("제2조 (기본 이자율)", fontWeight = FontWeight.Bold, fontSize = 15.sp)
        Text("본 계약의 기본 이자율은 연 %.2f%%로 정한다.".format(propInterest), fontSize = 14.sp, lineHeight = 24.sp, modifier = Modifier.padding(top = 4.dp))

        Spacer(modifier = Modifier.height(16.dp))
        
        proposal?.clauses?.forEachIndexed { index, propClause ->
            Text("제${index + 3}조 (특약)", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Text(propClause.content, fontSize = 14.sp, lineHeight = 24.sp, modifier = Modifier.padding(top = 4.dp))
            Spacer(modifier = Modifier.height(16.dp))
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}
