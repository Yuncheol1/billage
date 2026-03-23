package com.billage.app.ui.chat.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
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
    contractDetail: ContractDetailResponse?,
    onDismissRequest: () -> Unit,
    onAccept: () -> Unit
) {
    var selectedTab by remember { mutableStateOf("DIFF") }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
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
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismissRequest) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "닫기", tint = BillageTextPrimary)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "차용증 전문 보기",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = BillageTextPrimary
                    )
                }

                HorizontalDivider(color = BillageDivider)

                // Tabs
                Row(modifier = Modifier.fillMaxWidth()) {
                    TabButton(
                        text = "반영된 원문 보기",
                        isSelected = selectedTab == "ORIGINAL",
                        onClick = { selectedTab = "ORIGINAL" },
                        modifier = Modifier.weight(1f)
                    )
                    TabButton(
                        text = "변경사항(Diff) 보기",
                        isSelected = selectedTab == "DIFF",
                        onClick = { selectedTab = "DIFF" },
                        modifier = Modifier.weight(1f)
                    )
                }
                
                HorizontalDivider(color = BillageDivider)

                // Content
                Box(modifier = Modifier.weight(1f).background(Color(0xFFFFFDF8))) {
                    if (selectedTab == "DIFF") {
                        DocumentDiffView(contractTitle, contractDetail)
                    } else {
                        DocumentOriginalView(contractTitle, contractDetail)
                    }
                }

                // Footer
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White)
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onDismissRequest,
                        colors = ButtonDefaults.buttonColors(containerColor = BillageSurface, contentColor = BillageTextSecondary),
                        modifier = Modifier.weight(1f).height(50.dp)
                    ) {
                        Text("나중에 결정")
                    }
                    Button(
                        onClick = onAccept,
                        colors = ButtonDefaults.buttonColors(containerColor = BillagePrimary, contentColor = Color.White),
                        modifier = Modifier.weight(1f).height(50.dp)
                    ) {
                        Text("✅ 수락/전자서명", fontWeight = FontWeight.Bold)
                    }
                }
            }
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
        Text(title, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(24.dp))
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
        Text(title, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(24.dp))
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
