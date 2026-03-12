package com.billage.app.ui.chat.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import com.billage.app.ui.theme.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
fun ContractDocumentFullScreen(
    title: String,
    details: String,
    onDismissRequest: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            usePlatformDefaultWidth = false, // 화면 전체를 덮도록 설정
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
                    .systemBarsPadding() // 상태바, 네비게이션바 영역 확보
            ) {
                // 상단 헤더 영역 (뒤로가기 + 타이틀)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismissRequest) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack, 
                            contentDescription = "닫기",
                            tint = BillageTextPrimary
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "차용증 전문 보기",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = BillageTextPrimary
                    )
                }
                
                HorizontalDivider(color = BillageDivider.copy(alpha=0.5f))

                // 스크롤 가능한 본문 영역
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(24.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = BillagePrimary
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = details,
                        style = MaterialTheme.typography.bodyLarge,
                        color = BillageTextPrimary,
                        lineHeight = MaterialTheme.typography.bodyLarge.lineHeight * 1.6f // 가독성을 위한 줄간격 조정
                    )
                    
                    Spacer(modifier = Modifier.height(48.dp)) // 스크롤 하단 여유 공간
                }
            }
        }
    }
}

// ==========================================
// 프리뷰 영역
// ==========================================
@androidx.compose.ui.tooling.preview.Preview(showSystemUi = true, name = "차용증 단독 보기 (스크롤 테스트용)")
@Composable
fun PreviewContractDocumentFullScreen() {
    com.billage.app.ui.theme.BillageTheme(dynamicColor = false) {
        ContractDocumentFullScreen(
            title = "100만원 소액 대출 건",
            details = com.billage.app.ui.chat.preview.ChatPreviewData.dummyContractDetails,
            onDismissRequest = {}
        )
    }
}
