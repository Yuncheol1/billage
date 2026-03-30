package com.billage.app.ui.home

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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.billage.app.ui.theme.BillageBackground
import com.billage.app.ui.theme.BillageDanger
import com.billage.app.ui.theme.BillagePrimary
import com.billage.app.ui.theme.BillageSurface
import com.billage.app.ui.theme.BillageSuccess
import com.billage.app.ui.theme.BillageTextPrimary
import com.billage.app.ui.theme.BillageTextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountScreen(
    onBackClick: () -> Unit,
    onTransactionClick: (Long) -> Unit = {},
    viewModel: AccountViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        containerColor = BillageBackground,
        topBar = {
            TopAppBar(
                title = { Text("내 계좌", fontWeight = FontWeight.Bold) },
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
        when (val s = state) {
            is AccountScreenState.Loading -> {
                Box(
                    modifier = Modifier.padding(paddingValues).fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = BillagePrimary)
                }
            }

            is AccountScreenState.HasAccount -> {
                AccountContent(
                    account = s.account,
                    transactions = s.transactions,
                    onTransactionClick = onTransactionClick,
                    modifier = Modifier.padding(paddingValues)
                )
            }

            is AccountScreenState.NoAccount -> {
                NoAccountContent(
                    onCreateClick = { viewModel.createAccount() },
                    modifier = Modifier.padding(paddingValues)
                )
            }

            is AccountScreenState.Error -> {
                ErrorContent(
                    message = s.message,
                    onRetryClick = { viewModel.checkAccount() },
                    modifier = Modifier.padding(paddingValues)
                )
            }
        }
    }
}

@Composable
private fun AccountContent(
    account: BankAccountDto,
    transactions: List<TransactionDto>,
    onTransactionClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            BalanceCard(account = account)
        }
        item {
            Text(
                text = "입출금 내역",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = BillageTextPrimary,
                modifier = Modifier.padding(top = 8.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = BillageSurface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column {
                    transactions.forEachIndexed { index, transaction ->
                        TransactionItem(
                            transaction = transaction,
                            onClick = { onTransactionClick(transaction.transactionUniqueNo) }
                        )
                        if (index < transactions.lastIndex) {
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                color = BillageBackground
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BalanceCard(account: BankAccountDto) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = BillagePrimary),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            // 은행명
            Text(
                text = account.bankName.ifEmpty { account.bankCode },
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
            // 계좌번호
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = account.accountNo.ifEmpty { "계좌번호 없음" },
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 13.sp
            )
            // 구분선
            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.2f))
            Spacer(modifier = Modifier.height(14.dp))
            // 잔액 레이블
            Text(
                text = "잔액",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 12.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            // 잔액
            Text(
                text = "${String.format("%,d", account.accountBalance)}원",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun TransactionItem(transaction: TransactionDto, onClick: () -> Unit) {
    val isDeposit = transaction.transactionTypeName.contains("입금")
    val amountColor = if (isDeposit) BillageSuccess else BillageDanger
    val amountPrefix = if (isDeposit) "+" else "-"
    val displayCounterpart = transaction.counterpartName.ifBlank {
        transaction.transactionSummary.ifBlank { "-" }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = displayCounterpart,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = BillageTextPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${transaction.transactionDate} ${transaction.transactionTime}",
                fontSize = 12.sp,
                color = BillageTextSecondary
            )
        }
        Text(
            text = "$amountPrefix${String.format("%,d", transaction.transactionBalance)}원",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = amountColor
        )
    }
}

@Composable
private fun NoAccountContent(
    onCreateClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "아직 연결된 계좌가 없어요",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = BillageTextPrimary
            )
            Text(
                text = "빌리지 계좌를 만들어 거래를 관리해보세요",
                fontSize = 14.sp,
                color = BillageTextSecondary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Button(
                onClick = onCreateClick,
                colors = ButtonDefaults.buttonColors(containerColor = BillagePrimary),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(text = "계좌 만들기", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ErrorContent(
    message: String,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = message,
                fontSize = 14.sp,
                color = BillageDanger
            )
            Button(
                onClick = onRetryClick,
                colors = ButtonDefaults.buttonColors(containerColor = BillagePrimary),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(text = "다시 시도", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}
