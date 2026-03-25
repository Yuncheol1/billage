package com.billage.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.billage.app.ui.theme.BillageBackground
import com.billage.app.ui.theme.BillageDanger
import com.billage.app.ui.theme.BillageSurface
import com.billage.app.ui.theme.BillageSuccess
import com.billage.app.ui.theme.BillageTextPrimary
import com.billage.app.ui.theme.BillageTextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountDetailScreen(
    transactionSeq: Int,
    onBackClick: () -> Unit
) {
    val transaction = mockTransactions.find { it.transactionSeq == transactionSeq }

    Scaffold(
        containerColor = BillageBackground,
        topBar = {
            TopAppBar(
                title = { Text("거래 상세", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "뒤로가기"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BillageSurface)
            )
        }
    ) { paddingValues ->
        if (transaction == null) {
            Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
                Text(
                    text = "거래 내역을 찾을 수 없습니다.",
                    modifier = Modifier.padding(16.dp)
                )
            }
        } else {
            val isDeposit = transaction.type == TransactionType.DEPOSIT
            val amountColor = if (isDeposit) BillageSuccess else BillageDanger
            val amountPrefix = if (isDeposit) "+" else "-"

            Column(
                modifier = Modifier
                    .padding(paddingValues)
                    .padding(16.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // ── 거래 정보 카드 ──────────────────────────
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = BillageSurface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        DetailRow(label = "거래처", value = transaction.description)
                        DetailRow(
                            label = "날짜",
                            value = "${transaction.date} ${transaction.time}"
                        )
                        DetailRow(
                            label = "금액",
                            value = "$amountPrefix${String.format("%,d", transaction.amount)}원",
                            valueColor = amountColor
                        )
                        TypeBadgeRow(isDeposit = isDeposit)
                        DetailRow(
                            label = "메모",
                            value = transaction.memo?.takeIf { it.isNotBlank() } ?: "메모 없음",
                            valueColor = if (transaction.memo.isNullOrBlank()) BillageTextSecondary
                                         else BillageTextPrimary
                        )
                    }
                }

                // ── 계좌 정보 카드 ──────────────────────────
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = BillageSurface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        DetailRow(
                            label = "계좌번호",
                            value = transaction.accountNumber.ifEmpty { "-" }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: String,
    valueColor: Color = BillageTextPrimary
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 14.sp, color = BillageTextSecondary)
        Text(text = value, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = valueColor)
    }
}

@Composable
private fun TypeBadgeRow(isDeposit: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = "구분", fontSize = 14.sp, color = BillageTextSecondary)
        Box(
            modifier = Modifier
                .background(
                    color = if (isDeposit) BillageSuccess else BillageDanger,
                    shape = RoundedCornerShape(6.dp)
                )
                .padding(horizontal = 12.dp, vertical = 4.dp)
        ) {
            Text(
                text = if (isDeposit) "입금" else "출금",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
        }
    }
}
