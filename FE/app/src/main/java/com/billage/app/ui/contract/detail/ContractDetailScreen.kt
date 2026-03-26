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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.billage.app.data.remote.api.ClauseDetailDto
import com.billage.app.ui.auth.BiometricLoginManager
import com.billage.app.ui.theme.BillageBackground
import com.billage.app.ui.theme.BillageDanger
import com.billage.app.ui.theme.BillageDivider
import com.billage.app.ui.theme.BillagePrimary
import com.billage.app.ui.theme.BillagePrimaryLight
import com.billage.app.ui.theme.BillageSurface
import com.billage.app.ui.theme.BillageTextPrimary
import com.billage.app.ui.theme.BillageTextSecondary

private enum class DetailTab(val label: String) {
    Info("계약 정보"),
    Timeline("상환 타임라인")
}

private enum class RepaymentActionKind(
    val title: String,
    val message: String,
    val confirmLabel: String
) {
    ManualPay(
        title = "수동 상환 처리",
        message = "이 회차를 상환 완료로 처리할까요?",
        confirmLabel = "처리"
    )
}

private data class RepaymentActionDialogState(
    val scheduleSeq: Int,
    val actionKind: RepaymentActionKind
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContractDetailScreen(
    contractSeq: String,
    viewModel: ContractDetailViewModel,
    onBackClick: () -> Unit,
    onViewDocumentClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    val detail by viewModel.contractDetail.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val myName by viewModel.myName.collectAsState()
    val myProfileImageUrl by viewModel.myProfileImageUrl.collectAsState()
    val repaymentSchedules by viewModel.repaymentSchedules.collectAsState()
    val isTimelineLoading by viewModel.isTimelineLoading.collectAsState()
    val timelineErrorMessage by viewModel.timelineErrorMessage.collectAsState()
    val activeActionScheduleSeq by viewModel.activeActionScheduleSeq.collectAsState()

    var selectedTab by rememberSaveable { mutableStateOf(DetailTab.Info) }
    var pendingAction by remember { mutableStateOf<RepaymentActionDialogState?>(null) }
    var biometricMessage by remember { mutableStateOf<String?>(null) }

    fun openManualRepaymentDialog(scheduleSeq: Int) {
        pendingAction = RepaymentActionDialogState(
            scheduleSeq = scheduleSeq,
            actionKind = RepaymentActionKind.ManualPay
        )
    }

    fun requestManualRepaymentAuth(scheduleSeq: Int, myRole: String?) {
        if (myRole != "CREDITOR") {
            openManualRepaymentDialog(scheduleSeq)
            return
        }

        if (activity == null) {
            biometricMessage = "생체 인증을 사용할 수 없는 화면입니다."
            return
        }

        BiometricLoginManager.prompt(
            activity = activity,
            onSuccess = { openManualRepaymentDialog(scheduleSeq) },
            onError = { message ->
                if (message.isNotBlank()) {
                    biometricMessage = message
                }
            },
            onEnrollmentRequired = {
                biometricMessage = "개별 상환처리를 하려면 기기에서 지문 인증을 먼저 설정해주세요."
            },
            title = "개별 상환처리",
            subtitle = "개별 상환처리를 하려면 지문 인증이 필요합니다."
        )
    }

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
            val currentDetail = detail
            if (currentDetail != null) {
                val buttonText = when (currentDetail.status) {
                    "DRAFT", "NEGOTIATING" -> "채팅방에서 수정하기"
                    "ACTIVE" -> "상환 내역 보기"
                    else -> null
                }
                if (buttonText != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(BillageSurface)
                            .padding(horizontal = 20.dp, vertical = 16.dp)
                    ) {
                        Button(
                            onClick = { },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = BillagePrimary)
                        ) {
                            Text(
                                text = buttonText,
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
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = BillagePrimary)
                }
            }

            errorMessage != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(text = errorMessage.orEmpty(), color = BillageDanger, fontSize = 14.sp)
                        Button(
                            onClick = {
                                contractSeq.toIntOrNull()?.let {
                                    viewModel.fetchContractDetail(it)
                                    viewModel.fetchRepaymentSchedules(it)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = BillagePrimary)
                        ) {
                            Text("다시 시도")
                        }
                    }
                }
            }

            detail != null -> {
                val currentDetail = detail!!
                val iAmCreditor = currentDetail.myRole == "CREDITOR"
                val creditorProfileImageUrl = if (iAmCreditor) myProfileImageUrl else currentDetail.counterpartyProfileImageUrl
                val debtorProfileImageUrl = if (iAmCreditor) currentDetail.counterpartyProfileImageUrl else myProfileImageUrl
                val creditorName = if (iAmCreditor) myName else (currentDetail.counterpartyName ?: "상대방")
                val debtorName = if (iAmCreditor) (currentDetail.counterpartyName ?: "상대방") else myName
                val counterparty = currentDetail.counterpartyName ?: "상대방"
                val sortedClauses = currentDetail.clauses.sortedBy { it.orderNo }
                val dday = if (currentDetail.endDt != null && currentDetail.status !in listOf("COMPLETED", "CANCELLED")) {
                    calculateDday(currentDetail.endDt)
                } else {
                    null
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(vertical = 20.dp)
                ) {
                    item {
                        HeroSection(
                            status = currentDetail.status,
                            counterparty = counterparty,
                            dday = dday
                        )
                    }

                    item {
                        DetailTabSelector(
                            selectedTab = selectedTab,
                            onTabSelected = { selectedTab = it }
                        )
                    }

                    when (selectedTab) {
                        DetailTab.Info -> {
                            item {
                                KeyMetricsSection(
                                    principalAmount = currentDetail.principalAmount,
                                    interestRate = currentDetail.interestRate
                                )
                            }

                            item {
                                ContractPartiesCard(
                                    creditorName = creditorName,
                                    debtorName = debtorName,
                                    creditorProfileImageUrl = creditorProfileImageUrl,
                                    debtorProfileImageUrl = debtorProfileImageUrl,
                                    isFamily = currentDetail.family,
                                    startDt = currentDetail.startDt,
                                    endDt = currentDetail.endDt,
                                    dday = dday,
                                    repaymentMethod = currentDetail.repaymentMethod,
                                    totalInstallments = currentDetail.totalInstallments
                                )
                            }

                            if (sortedClauses.isNotEmpty()) {
                                item {
                                    ClauseSection(sortedClauses)
                                }
                            }

                            if (currentDetail.signedDttm != null) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 8.dp, bottom = 4.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "전자서명 완료 ${formatDateTime(currentDetail.signedDttm)}",
                                            fontSize = 12.sp,
                                            color = BillageTextSecondary.copy(alpha = 0.7f)
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
                                        text = "차용증 원문 보기",
                                        fontSize = 13.sp,
                                        color = BillagePrimary,
                                        textDecoration = TextDecoration.Underline,
                                        modifier = Modifier.clickable { onViewDocumentClick() }
                                    )
                                }
                            }
                        }

                        DetailTab.Timeline -> {
                            item {
                                ContractTimelineSection(
                                    schedules = repaymentSchedules,
                                    isLoading = isTimelineLoading,
                                    errorMessage = timelineErrorMessage,
                                    myRole = currentDetail.myRole,
                                    activeActionScheduleSeq = activeActionScheduleSeq,
                                    onManualRepaymentClick = { scheduleSeq ->
                                        requestManualRepaymentAuth(scheduleSeq, currentDetail.myRole)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    pendingAction?.let { dialogState ->
        AlertDialog(
            onDismissRequest = { pendingAction = null },
            title = { Text(dialogState.actionKind.title) },
            text = { Text(dialogState.actionKind.message) },
            confirmButton = {
                TextButton(
                    onClick = {
                        contractSeq.toIntOrNull()?.let { contractSeqInt ->
                            viewModel.markManualRepayment(contractSeqInt, dialogState.scheduleSeq)
                        }
                        pendingAction = null
                    }
                ) {
                    Text(dialogState.actionKind.confirmLabel)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingAction = null }) {
                    Text("취소")
                }
            }
        )
    }

    biometricMessage?.let { message ->
        AlertDialog(
            onDismissRequest = { biometricMessage = null },
            title = { Text("지문 인증 안내") },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = { biometricMessage = null }) {
                    Text("확인")
                }
            }
        )
    }
}

@Composable
private fun DetailTabSelector(
    selectedTab: DetailTab,
    onTabSelected: (DetailTab) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(BillageSurface, RoundedCornerShape(16.dp))
            .padding(6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        DetailTab.entries.forEach { tab ->
            val selected = selectedTab == tab
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(
                        color = if (selected) BillagePrimaryLight else BillageSurface,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .clickable { onTabSelected(tab) }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = tab.label,
                    fontSize = 14.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    color = if (selected) BillagePrimary else BillageTextSecondary
                )
            }
        }
    }
}

@Composable
private fun ClauseSection(clauses: List<ClauseDetailDto>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(BillageSurface, RoundedCornerShape(16.dp))
    ) {
        Text(
            text = "특약사항",
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = BillageTextSecondary
        )

        clauses.forEachIndexed { index, clause ->
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

            if (index < clauses.lastIndex) {
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    thickness = 1.dp,
                    color = BillageDivider
                )
            }
        }
    }
}
