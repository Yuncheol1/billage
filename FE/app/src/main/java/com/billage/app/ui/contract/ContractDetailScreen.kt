package com.billage.app.ui.contract

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.billage.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContractDetailScreen(
    contractSeq: String,
    viewModel: ContractDetailViewModel,
    onBackClick: () -> Unit
) {
    val detail by viewModel.contractDetail.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("차용증 원문 조회", fontSize = 18.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로 가기")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BillageSurface,
                    titleContentColor = BillageTextPrimary
                )
            )
        },
        containerColor = BillageBackground
    ) { paddingValues ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (detail != null) {
            val d = detail!!
            val creditor = if (d.myRole == "CREDITOR") d.counterpartyName else "상대방"
            val debtor = if (d.myRole == "DEBTOR") d.counterpartyName else "본인"
            
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Text(
                            text = "차 용 증",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.Black,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        Text(
                            text = "채권자 ${creditor}(이하 '갑')과 채무자 ${debtor}(이하 '을')는 빌리지(Billage) 서비스를 통하여 다음과 같이 금전소비대차계약을 체결한다.",
                            fontSize = 14.sp,
                            lineHeight = 22.sp,
                            color = Color.DarkGray
                        )
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        // 서버에서 내려주는 조항들을 순서대로 노출
                        d.clauses.sortedBy { it.orderNo }.forEach { clause ->
                            ClauseItem(
                                title = clause.title,
                                content = clause.content
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(32.dp))
                        
                        Text(
                            text = d.startDt ?: "작성일 미상",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center,
                            color = Color.Black
                        )
                    }
                }
            }
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = "계약 상세 정보를 불러올 수 없습니다.", color = BillageTextSecondary)
            }
        }
    }
}

@Composable
fun ClauseItem(title: String, content: String) {
    Column(modifier = Modifier.padding(bottom = 16.dp)) {
        Text(
            text = title,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Text(
            text = content,
            fontSize = 15.sp,
            color = Color.DarkGray,
            lineHeight = 22.sp
        )
    }
}
