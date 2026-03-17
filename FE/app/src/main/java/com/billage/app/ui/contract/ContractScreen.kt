package com.billage.app.ui.contract

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.billage.app.ui.contract.steps.Step1FamilyScreen
import com.billage.app.ui.contract.steps.Step2AmountScreen
import com.billage.app.ui.contract.steps.Step3InterestScreen
import com.billage.app.ui.contract.steps.Step4DateScreen
import com.billage.app.ui.contract.steps.Step5RepaymentScreen
import com.billage.app.ui.contract.steps.Step6DebtorScreen
import com.billage.app.ui.contract.steps.Step7PreviewScreen
import com.billage.app.ui.theme.BillagePrimary
import com.billage.app.ui.theme.BillagePrimaryDisabled
import com.billage.app.ui.theme.BillagePrimaryLight
import com.billage.app.ui.theme.BillageTextPrimary

@Composable
fun ContractScreen(
    onNavigateBack: () -> Unit,
    onContractComplete: () -> Unit,
    viewModel: ContractViewModel = viewModel()
) {
    val currentStep by viewModel.currentStep.collectAsState()
    val formState by viewModel.formState.collectAsState()
    val isNextEnabled by remember(currentStep, formState) {
        derivedStateOf { viewModel.isNextEnabled() }
    }

    BackHandler {
        if (currentStep == 1) onNavigateBack()
        else viewModel.prevStep()
    }

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
                4 -> Step4DateScreen(
                    startDate = formState.startDate,
                    endDate = formState.endDate,
                    onStartDateSelect = { viewModel.updateStartDate(it) },
                    onEndDateSelect = { viewModel.updateEndDate(it) }
                )
                5 -> Step5RepaymentScreen(
                    startDate = formState.startDate,
                    endDate = formState.endDate,
                    selectedMethod = formState.repaymentMethod,
                    selectedDayType = formState.repaymentDayType,
                    customDay = formState.repaymentCustomDay,
                    onMethodSelect = { viewModel.updateRepaymentMethod(it) },
                    onDayTypeSelect = { viewModel.updateRepaymentDayType(it) },
                    onCustomDaySelect = { viewModel.updateRepaymentCustomDay(it) }
                )
                6 -> Step6DebtorScreen(
                    phone = formState.debtorPhone,
                    onPhoneChange = { viewModel.updateDebtorPhone(it) }
                )
                7 -> Step7PreviewScreen(
                    formState = formState,
                    onEditStep = { viewModel.goToStep(it) },
                    onSubmit = { onContractComplete() }
                )
                else -> Unit
            }
        }

        if (currentStep < ContractViewModel.TOTAL_STEPS) {
            ContractNextButton(
                enabled = isNextEnabled,
                onClick = { viewModel.nextStep() }
            )
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
            disabledContainerColor = BillagePrimaryDisabled,
            contentColor = Color.White,
            disabledContentColor = Color.White.copy(alpha = 0.5f)
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