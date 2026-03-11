package com.billage.app.ui.contract

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.billage.app.ui.contract.steps.Step1FamilyScreen
import com.billage.app.ui.contract.steps.Step2AmountScreen
import com.billage.app.ui.contract.steps.Step3InterestScreen
import com.billage.app.ui.contract.steps.Step4StartDateScreen
import com.billage.app.ui.contract.steps.Step5EndDateScreen
import com.billage.app.ui.contract.steps.Step6RepaymentScreen
import com.billage.app.ui.contract.steps.Step7DebtorScreen
import com.billage.app.ui.contract.steps.Step8PreviewScreen
import com.billage.app.ui.theme.BillagePrimary
import com.billage.app.ui.theme.BillageTextPrimary
import com.billage.app.ui.theme.BillagePrimaryLight
import com.billage.app.ui.theme.BillagePrimaryDisabled

@Composable
fun ContractScreen(
    onNavigateBack: () -> Unit,          // 홈으로 나가기 (Step 1에서 뒤로가기)
    onContractComplete: () -> Unit,      // 전송 완료 후 대기 화면으로 이동
    viewModel: ContractViewModel = viewModel()
) {
    val currentStep by viewModel.currentStep.collectAsState()
    val formState by viewModel.formState.collectAsState()

    // ── 뒤로가기 처리 ─────────────────────
    // Step 1이면 홈으로, 그 외엔 이전 스텝으로
    BackHandler {
        if (currentStep == 1) {
            onNavigateBack()
        } else {
            viewModel.prevStep()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding() // 하단 네비게이션 바 높이만큼 패딩
    ) {
        // ── 상단 영역: 뒤로가기 + 스텝 표시 + 진행바 ──
        ContractTopBar(
            currentStep = currentStep,
            onBackClick = {
                if (currentStep == 1) onNavigateBack()
                else viewModel.prevStep()
            }
        )

        // ── 스텝별 화면 전환 ──────────────────
        // AnimatedContent: 스텝 이동 시 슬라이드 애니메이션
        AnimatedContent(
            targetState = currentStep,
            modifier = Modifier.weight(1f), // 남은 공간 전부 차지
            transitionSpec = {
                // 다음 스텝: 오른쪽에서 슬라이드인
                // 이전 스텝: 왼쪽에서 슬라이드인
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
                1 -> Step1FamilyScreen(
                    isFamily = formState.isFamily,
                    onSelect = { viewModel.updateIsFamily(it) }
                )
                2 -> Step2AmountScreen(
                    amount = formState.amount,
                    onAmountChange = { viewModel.updateAmount(it) }
                )
                3 -> Step3InterestScreen(
                    interestRate = formState.interestRate,
                    isFamily = formState.isFamily ?: false,
                    amount = formState.amount,
                    onRateChange = { viewModel.updateInterestRate(it) }
                )
                4 -> Step4StartDateScreen(
                    startDate = formState.startDate,
                    onDateSelect = { viewModel.updateStartDate(it) }
                )
                5 -> Step5EndDateScreen(
                    endDate = formState.endDate,
                    startDate = formState.startDate,
                    onDateSelect = { viewModel.updateEndDate(it) }
                )
                6 -> Step6RepaymentScreen(
                    startDate = formState.startDate,
                    endDate = formState.endDate,
                    selectedMethod = formState.repaymentMethod,
                    selectedDayType = formState.repaymentDayType,
                    customDay = formState.repaymentCustomDay,
                    onMethodSelect = { viewModel.updateRepaymentMethod(it) },
                    onDayTypeSelect = { viewModel.updateRepaymentDayType(it) },
                    onCustomDaySelect = { viewModel.updateRepaymentCustomDay(it) }
                )
                7 -> Step7DebtorScreen(
                    phone = formState.debtorPhone,
                    onPhoneChange = { viewModel.updateDebtorPhone(it) }
                )
                8 -> Step8PreviewScreen(
                    formState = formState,
                    onEditStep = { viewModel.goToStep(it) },
                    onSubmit = { onContractComplete() }
                )
                else -> Unit
            }
        }

        // ── 하단 다음 버튼 ────────────────────
        // Step 8(미리보기)에서는 버튼을 숨김
        // Step 8의 전송 버튼은 Step8PreviewScreen 내부에서 처리
        if (currentStep < ContractViewModel.TOTAL_STEPS) {
            ContractNextButton(
                enabled = viewModel.isNextEnabled(),
                onClick = { viewModel.nextStep() }
            )
        }
    }
}

// ── 상단 TopBar ────────────────────────────
// 뒤로가기 아이콘 + "현재스텝 / 전체스텝" + 진행바
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
            // 뒤로가기 버튼
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "뒤로가기",
                    tint = BillageTextPrimary
                )
            }

            // "4 / 8" 스텝 표시
            Text(
                text = "$currentStep / ${ContractViewModel.TOTAL_STEPS}",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = BillageTextPrimary,
                modifier = Modifier.padding(end = 16.dp)
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // 진행바
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

// ── 하단 다음 버튼 ──────────────────────────
@Composable
private fun ContractNextButton(
    enabled: Boolean,
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
            disabledContainerColor = BillagePrimaryDisabled, // 비활성 시 연한 민트
            contentColor = Color.White,           // 활성 시 텍스트
            disabledContentColor = Color.White.copy(alpha = 0.5f) // 비활성 시 텍스트 (반투명)
        )
    ) {
        Text(
            text = "다음",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}