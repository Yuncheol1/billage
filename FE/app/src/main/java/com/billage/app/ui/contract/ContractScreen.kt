package com.billage.app.ui.contract

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.billage.app.ui.contract.steps.Step1FamilyScreen
import com.billage.app.ui.contract.steps.Step2AmountScreen
import com.billage.app.ui.contract.steps.Step3InterestScreen
import com.billage.app.ui.contract.steps.Step4DateScreen
import com.billage.app.ui.contract.steps.Step5RepaymentScreen
import com.billage.app.ui.contract.steps.Step6DebtorScreen
import com.billage.app.ui.theme.BillagePrimary
import com.billage.app.ui.theme.BillagePrimaryDisabled
import com.billage.app.ui.theme.BillagePrimaryLight
import com.billage.app.ui.theme.BillageTextPrimary

@Composable
fun ContractScreen(
    onNavigateBack: () -> Unit,
    onContractComplete: (contractSeq: Int, roomId: String, opponentName: String) -> Unit,
    role: ContractRole = ContractRole.CREDITOR,
    viewModel: ContractViewModel = hiltViewModel()
) {
    val currentStep by viewModel.currentStep.collectAsState()
    val formState by viewModel.formState.collectAsState()
    val submitState by viewModel.submitState.collectAsState()
    val isNextEnabled by remember(currentStep, formState) {
        derivedStateOf { viewModel.isNextEnabled() }
    }
    val searchState by viewModel.searchState.collectAsState()

    // role이 바뀌거나 화면이 처음 진입할 때 항상 폼을 초기화
    LaunchedEffect(role) { viewModel.resetForm(role) }

    // submitState 관찰 — 성공하면 contractSeq를 들고 다음 화면으로 이동
    LaunchedEffect(submitState) {
        if (submitState is ContractSubmitState.Success) {
            val success = submitState as ContractSubmitState.Success
            // 재진입 시 Success 상태로 LaunchedEffect가 재실행되는 것을 방지하기 위해
            // 화면 이동 전에 먼저 상태를 초기화
            viewModel.resetSubmitState()
            onContractComplete(success.contractSeq, success.roomId, success.opponentName)
        }
    }

    // 로딩 중일 때 뒤로가기 방지
    BackHandler(enabled = submitState is ContractSubmitState.Loading) { }
    BackHandler(enabled = submitState !is ContractSubmitState.Loading) {
        if (currentStep == 1) onNavigateBack()
        else viewModel.prevStep()
    }

    // 에러 다이얼로그
    if (submitState is ContractSubmitState.Error) {
        AlertDialog(
            onDismissRequest = { viewModel.resetSubmitState() },
            title = { Text("계약 생성 실패") },
            text = { Text((submitState as ContractSubmitState.Error).message) },
            confirmButton = {
                TextButton(onClick = { viewModel.resetSubmitState() }) {
                    Text("확인", color = BillagePrimary)
                }
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
        ) {
            ContractTopBar(
                currentStep = currentStep,
                onBackClick = {
                    if (currentStep == 1) onNavigateBack()
                    else viewModel.prevStep()
                }
            )

            AnimatedContent(
                targetState = currentStep,
                modifier = Modifier.weight(1f),
                transitionSpec = {
                    if (targetState > initialState) {
                        (slideInHorizontally { it } + fadeIn()) togetherWith
                                (slideOutHorizontally { -it } + fadeOut())
                    } else {
                        (slideInHorizontally { -it } + fadeIn()) togetherWith
                                (slideOutHorizontally { it } + fadeOut())
                    }
                },
                label = "StepTransition"
            ) { step ->
                when (step) {
                    1 -> Step6DebtorScreen(
                        realName = formState.debtorName,
                        onRealNameChange = { viewModel.updateDebtorName(it) },
                        phone = formState.debtorPhone,
                        onPhoneChange = { viewModel.updateDebtorPhone(it) },
                        searchState = searchState,
                        onSearch = { viewModel.searchUser() },
                        role = formState.role
                    )
                    2 -> Step1FamilyScreen(
                        isFamily = formState.isFamily,
                        onSelect = { viewModel.updateIsFamily(it) },
                        role = formState.role
                    )
                    3 -> Step2AmountScreen(
                        amount = formState.amount,
                        onAmountChange = { viewModel.updateAmount(it) }
                    )
                    4 -> Step3InterestScreen(
                        interestRate = formState.interestRate,
                        isFamily = formState.isFamily ?: false,
                        amount = formState.amount,
                        onRateChange = { viewModel.updateInterestRate(it) }
                    )
                    5 -> Step4DateScreen(
                        startDate = formState.startDate,
                        endDate = formState.endDate,
                        onStartDateSelect = { viewModel.updateStartDate(it) },
                        onEndDateSelect = { viewModel.updateEndDate(it) }
                    )
                    6 -> Step5RepaymentScreen(
                        startDate = formState.startDate,
                        endDate = formState.endDate,
                        selectedMethod = formState.repaymentMethod,
                        onMethodSelect = { viewModel.updateRepaymentMethod(it) }
                    )
                    else -> Unit
                }
            }

            ContractNextButton(
                text = if (currentStep == ContractViewModel.TOTAL_STEPS) "완료하기" else "다음",
                enabled = isNextEnabled && submitState !is ContractSubmitState.Loading,
                isLoading = submitState is ContractSubmitState.Loading,
                onClick = {
                    if (currentStep == ContractViewModel.TOTAL_STEPS) {
                        // TODO: 로그인한 사용자의 userSeq를 AuthSessionStorage에서 가져와야 해요
                        // 현재는 임시로 1 사용 — 로그인 연동 후 실제 값으로 교체
                        viewModel.submitContractDraft()
                    } else {
                        viewModel.nextStep()
                    }
                }
            )
        }

        // 로딩 오버레이 — API 호출 중 화면 전체를 덮어서 중복 클릭 방지
        if (submitState is ContractSubmitState.Loading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = BillagePrimary,
                    modifier = Modifier.size(48.dp)
                )
            }
        }
    }
}

@Composable
private fun ContractTopBar(
    currentStep: Int,
    onBackClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "뒤로가기",
                    tint = BillageTextPrimary
                )
            }
            Text(
                text = "$currentStep / ${ContractViewModel.TOTAL_STEPS}",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = BillageTextPrimary,
                modifier = Modifier.padding(end = 16.dp)
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        LinearProgressIndicator(
            progress = { currentStep.toFloat() / ContractViewModel.TOTAL_STEPS },
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp),
            color = BillagePrimary,
            trackColor = BillagePrimaryLight,
            strokeCap = StrokeCap.Round
        )
    }
}

@Composable
private fun ContractNextButton(
    text: String,
    enabled: Boolean,
    isLoading: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp)
            .height(56.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = BillagePrimary,
            disabledContainerColor = BillagePrimaryDisabled,
            contentColor = Color.White,
            disabledContentColor = Color.White.copy(alpha = 0.5f)
        )
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                color = Color.White,
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp
            )
        } else {
            Text(
                text = text,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}