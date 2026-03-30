package com.billage.app.ui.contract.detail

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.billage.app.data.remote.api.ClauseDetailDto
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.BorderStroke
import com.billage.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContractEditScreen(
    contractSeq: String,
    viewModel: ContractDetailViewModel,
    roomId: String? = null,
    onBackClick: () -> Unit,
    onSaveSuccess: () -> Unit
) {
    val detail by viewModel.contractDetail.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    // 폼 상태 관리용
    var principalAmount by remember { mutableStateOf("") }
    var interestRate by remember { mutableStateOf("") }
    var clauseList by remember { mutableStateOf<List<ClauseDetailDto>>(emptyList()) }

    LaunchedEffect(detail) {
        detail?.let {
            if (principalAmount.isBlank()) {
                principalAmount = it.principalAmount?.toInt()?.toString() ?: ""
            }
            if (interestRate.isBlank()) {
                interestRate = ((it.interestRate ?: 0.0) * 100.0).toString()
            }
            if (clauseList.isEmpty()) {
                clauseList = it.clauses.sortedBy { clause -> clause.orderNo }
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.updateSuccess.collect { success ->
            if (success) onSaveSuccess()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("차용증 직접 수정하기", fontSize = 18.sp, fontWeight = FontWeight.Bold) },
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
        bottomBar = {
            Button(
                onClick = { 
                    viewModel.updateContract(
                        contractSeq = contractSeq.toInt(),
                        principalAmount = principalAmount.toDoubleOrNull(),
                        interestRate = (interestRate.toDoubleOrNull() ?: 0.0) / 100.0,
                        clauses = clauseList.map { com.billage.app.data.remote.api.ClauseUpdateDto(it.clauseSeq, it.content) },
                        roomId = roomId
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding() // 시스템 네비게이션 바 대응
                    .padding(16.dp)
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BillagePrimary)
            ) {
                Text("수정 내용 저장", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        },
        containerColor = BillageBackground
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .imePadding() // 키보드만큼 공간 확보
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Text(
                text = "기본 정보 (수정 불가)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = BillageTextPrimary,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            // Read-only 뼈대 정보 카드
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(androidx.compose.ui.Alignment.CenterHorizontally))
            } else if (detail != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = BillageSurface),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        ReadOnlyField(label = "상환 방법", value = formatRepaymentMethodFriendly(detail!!.repaymentMethod, detail!!.totalInstallments))
                        Spacer(modifier = Modifier.height(8.dp))
                        ReadOnlyField(label = "상환일", value = detail!!.endDt ?: "알 수 없음")
                    }
                }
            } else {
                Text("계약 정보를 불러올 수 없습니다.", color = BillageTextSecondary, modifier = Modifier.padding(vertical = 16.dp))
            }
            
            HorizontalDivider(color = BillageDivider)
            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "계약 세부 조항 수정",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = BillageTextPrimary,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                text = "이율 및 특약 등 세부 조건만 변경할 수 있습니다.",
                style = MaterialTheme.typography.bodySmall,
                color = BillageTextSecondary,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // 원금 수정 창
            OutlinedTextField(
                value = principalAmount,
                onValueChange = { principalAmount = it },
                label = { Text("차용 원금 (원)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BillagePrimary,
                    focusedLabelColor = BillagePrimary
                )
            )
            
            Spacer(modifier = Modifier.height(16.dp))

            // 금리 수정 창
            OutlinedTextField(
                value = interestRate,
                onValueChange = { interestRate = it },
                label = { Text("기본 이자율 (연 단위 %)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BillagePrimary,
                    focusedLabelColor = BillagePrimary
                )
            )
            
            Spacer(modifier = Modifier.height(16.dp))

            // 개별 조항 수정 창
            clauseList.forEachIndexed { index, clause ->
                OutlinedTextField(
                    value = clause.content,
                    onValueChange = { newContent ->
                        val updatedList = clauseList.toMutableList()
                        updatedList[index] = clause.copy(content = newContent)
                        clauseList = updatedList
                    },
                    label = { Text(if (clause.title.isNotBlank()) clause.title else "특약 (제${index + 1}조)") },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    maxLines = 5,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BillagePrimary,
                        focusedLabelColor = BillagePrimary
                    ),
                    trailingIcon = {
                        IconButton(onClick = {
                            val updatedList = clauseList.toMutableList()
                            updatedList.removeAt(index)
                            clauseList = updatedList
                        }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "특약 삭제",
                                tint = BillageTextSecondary
                            )
                        }
                    }
                )
            }
            
            // 특약 추가 버튼
            OutlinedButton(
                onClick = {
                    val newClause = ClauseDetailDto(
                        clauseSeq = 0,
                        title = "추가 특약",
                        content = "",
                        orderNo = clauseList.size + 1
                    )
                    clauseList = clauseList + newClause
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .padding(bottom = 16.dp),
                border = BorderStroke(1.dp, BillageDivider),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = BillageTextPrimary)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "특약 추가",
                    tint = BillageTextSecondary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("특약 추가하기", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            }
            
            // 키보드에 가려지지 않도록 추가 여백
            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}

@Composable
fun ReadOnlyField(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = BillageTextSecondary, fontSize = 14.sp)
        Text(text = value, color = BillageTextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }
}
