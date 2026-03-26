package com.billage.app.ui.home

import android.widget.Toast
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.billage.app.ui.theme.BillageBackground
import com.billage.app.ui.theme.BillageDanger
import com.billage.app.ui.theme.BillageDivider
import com.billage.app.ui.theme.BillagePrimary
import com.billage.app.ui.theme.BillagePrimaryLight
import com.billage.app.ui.theme.BillageSurface
import com.billage.app.ui.theme.BillageSuccess
import com.billage.app.ui.theme.BillageTextPrimary
import com.billage.app.ui.theme.BillageTextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountDetailScreen(
    transactionUniqueNo: Long,
    onBackClick: () -> Unit,
    viewModel: AccountViewModel = hiltViewModel()
) {
    val transaction by viewModel.transactionDetail.collectAsState()
    val isLoading by viewModel.transactionDetailLoading.collectAsState()

    LaunchedEffect(transactionUniqueNo) {
        viewModel.loadTransactionDetail(transactionUniqueNo)
    }

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
        when {
            isLoading -> {
                Box(
                    modifier = Modifier.padding(paddingValues).fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = BillagePrimary)
                }
            }

            transaction == null -> {
                Box(
                    modifier = Modifier.padding(paddingValues).fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "거래 내역을 찾을 수 없습니다.",
                        fontSize = 14.sp,
                        color = BillageTextSecondary
                    )
                }
            }

            else -> {
                TransactionDetailContent(
                    transaction = transaction!!,
                    modifier = Modifier.padding(paddingValues)
                )
            }
        }
    }
}

@Composable
private fun TransactionDetailContent(
    transaction: TransactionDto,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val isDeposit = transaction.transactionTypeName.contains("입금")
    val amountColor = if (isDeposit) BillageSuccess else BillageDanger
    val amountPrefix = if (isDeposit) "+" else "-"
    val displayCounterpart = transaction.counterpartName.ifBlank {
        transaction.transactionSummary.ifBlank { "-" }
    }
    val initial = displayCounterpart.firstOrNull()?.toString() ?: "?"
    val formattedDateTime = formatTransactionDateTime(transaction.transactionDate, transaction.transactionTime)

    LazyColumn(
        modifier = modifier.fillMaxSize()
    ) {
        // ── 상단 헤더 ──────────────────────────────────
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(BillageSurface)
                    .padding(horizontal = 24.dp, vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 원형 아이콘 (이니셜)
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(BillagePrimaryLight),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = initial,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = BillagePrimary
                    )
                }

                // 적요 (거래처명)
                Text(
                    text = displayCounterpart,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = BillageTextPrimary
                )

                // 금액 + 복사 버튼
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "$amountPrefix${String.format("%,d", transaction.transactionBalance)}원",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = amountColor
                    )
                    Text(
                        text = "복사",
                        fontSize = 13.sp,
                        color = BillageTextSecondary,
                        modifier = Modifier
                            .clickable {
                                clipboardManager.setText(
                                    AnnotatedString("${transaction.transactionBalance}")
                                )
                            }
                            .background(
                                color = BillageBackground,
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(6.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            HorizontalDivider(color = BillageDivider)
        }

        // ── 본문 상세 행 ───────────────────────────────
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(BillageSurface)
            ) {
                DetailRow(label = "적요", value = displayCounterpart)
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = BillageDivider)

                DetailRow(label = "거래 유형", value = transaction.transactionTypeName.ifBlank { "-" })
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = BillageDivider)

                DetailRow(label = "계좌번호", value = transaction.transactionAccountNo.ifBlank { "-" })
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = BillageDivider)

                // 메모: 값 있으면 표시, 없으면 BillagePrimary "메모 남기기 >"
                val hasMemo = transaction.transactionMemo.isNotBlank()
                DetailRow(
                    label = "메모",
                    value = if (hasMemo) transaction.transactionMemo else "메모 남기기 >",
                    valueColor = if (hasMemo) BillageTextPrimary else BillagePrimary
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = BillageDivider)

                DetailRow(label = "일시", value = formattedDateTime)
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = BillageDivider)

                DetailRow(
                    label = "거래 후 잔액",
                    value = "${String.format("%,d", transaction.transactionAfterBalance)}원"
                )
            }

            HorizontalDivider(color = BillageDivider)
        }

        // ── 하단 버튼 ──────────────────────────────────
        item {
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(BillageSurface)
                    .clickable {
                        Toast
                            .makeText(context, "준비 중입니다", Toast.LENGTH_SHORT)
                            .show()
                    }
                    .padding(horizontal = 24.dp, vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "증명서 발급하기 >",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = BillageTextPrimary
                )
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
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 14.sp, color = BillageTextSecondary)
        Text(text = value, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = valueColor)
    }
}

private fun formatTransactionDateTime(date: String, time: String): String {
    if (date.length < 8) return "$date $time"
    val year = date.substring(0, 4)
    val month = date.substring(4, 6).trimStart('0').ifEmpty { "0" }
    val day = date.substring(6, 8).trimStart('0').ifEmpty { "0" }
    val hour = if (time.length >= 4) time.substring(0, 2) else "00"
    val minute = if (time.length >= 4) time.substring(2, 4) else "00"
    return "${year}년 ${month}월 ${day}일 ${hour}:${minute}"
}
