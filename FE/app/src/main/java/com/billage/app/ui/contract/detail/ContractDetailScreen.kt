package com.billage.app.ui.contract.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.billage.app.ui.theme.BillageBackground
import com.billage.app.ui.theme.BillageDanger
import com.billage.app.ui.theme.BillageDivider
import com.billage.app.ui.theme.BillagePrimary
import com.billage.app.ui.theme.BillageSurface
import com.billage.app.ui.theme.BillageTextPrimary
import com.billage.app.ui.theme.BillageTextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContractDetailScreen(
    contractSeq: String,
    viewModel: ContractDetailViewModel,
    onBackClick: () -> Unit
) {
    val detail by viewModel.contractDetail.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val myName by viewModel.myName.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "계약 상세",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = BillageTextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "뒤로 가기",
                            tint = BillageTextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BillageSurface)
            )
        },
        bottomBar = {
            val d = detail
            if (d != null) {
                val btnText = when (d.status) {
                    "DRAFT", "NEGOTIATING" -> "채팅방에서 협의하기"
                    "ACTIVE"               -> "상환 내역 보기"
                    else                   -> null
                }
                if (btnText != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(BillageSurface)
                            .padding(horizontal = 20.dp, vertical = 16.dp)
                    ) {
                        Button(
                            onClick = { /* TODO */ },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = BillagePrimary)
                        ) {
                            Text(
                                text = btnText,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        },
        containerColor = BillageBackground
    ) { paddingValues ->
        when {
            isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = BillagePrimary)
                }
            }

            errorMessage != null -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(text = errorMessage!!, color = BillageDanger, fontSize = 14.sp)
                        Button(
                            onClick = {
                                contractSeq.toIntOrNull()?.let { viewModel.fetchContractDetail(it) }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = BillagePrimary)
                        ) {
                            Text("다시 시도")
                        }
                    }
                }
            }

            detail != null -> {
                val d = detail!!
                val iAmCreditor = d.myRole == "CREDITOR"
                val creditorName = if (iAmCreditor) myName else (d.counterpartyName ?: "상대방")
                val debtorName = if (iAmCreditor) (d.counterpartyName ?: "상대방") else myName
                val counterparty = d.counterpartyName ?: "상대방"
                val sortedClauses = d.clauses.sortedBy { it.orderNo }
                val dday = if (d.endDt != null && d.status !in listOf("COMPLETED", "CANCELLED")) {
                    calculateDday(d.endDt)
                } else null

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(vertical = 20.dp)
                ) {
                    item {
                        HeroSection(status = d.status, counterparty = counterparty, dday = dday)
                    }

                    item {
                        KeyMetricsSection(
                            principalAmount = d.principalAmount,
                            interestRate = d.interestRate
                        )
                    }

                    item {
                        ContractPartiesCard(
                            creditorName = creditorName,
                            debtorName = debtorName,
                            isFamily = d.family,
                            startDt = d.startDt,
                            endDt = d.endDt,
                            dday = dday,
                            repaymentMethod = d.repaymentMethod,
                            totalInstallments = d.totalInstallments
                        )
                    }

                    if (sortedClauses.isNotEmpty()) {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(BillageSurface, RoundedCornerShape(16.dp))
                            ) {
                                Text(
                                    text = "특약사항",
                                    modifier = Modifier.padding(
                                        horizontal = 20.dp,
                                        vertical = 16.dp
                                    ),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = BillageTextSecondary
                                )
                                sortedClauses.forEachIndexed { index, clause ->
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 20.dp)
                                            .padding(bottom = 16.dp)
                                    ) {
                                        Text(
                                            text = clause.title,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = BillageTextPrimary
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = clause.content,
                                            fontSize = 13.sp,
                                            color = BillageTextSecondary,
                                            lineHeight = 20.sp
                                        )
                                    }
                                    if (index < sortedClauses.lastIndex) {
                                        HorizontalDivider(
                                            modifier = Modifier.padding(horizontal = 20.dp),
                                            thickness = 1.dp,
                                            color = BillageDivider
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (d.signedDttm != null) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp, bottom = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "양측 서명 완료 · ${
                                        d.signedDttm.substringBefore("T").replace("-", ".")
                                    }",
                                    fontSize = 12.sp,
                                    color = BillageTextSecondary.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }

                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp, bottom = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "차용증 전문 보기",
                                fontSize = 13.sp,
                                color = BillagePrimary,
                                textDecoration = TextDecoration.Underline,
                                modifier = Modifier.clickable { /* TODO */ }
                            )
                        }
                    }
                }
            }
        }
    }
}
