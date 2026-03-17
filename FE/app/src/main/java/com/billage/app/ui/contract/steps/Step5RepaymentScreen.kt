package com.billage.app.ui.contract.steps

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.billage.app.ui.contract.RepaymentDayType
import com.billage.app.ui.contract.RepaymentMethod

@Composable
fun Step5RepaymentScreen(
    startDate: String,
    endDate: String,
    selectedMethod: RepaymentMethod?,
    selectedDayType: RepaymentDayType?,
    customDay: Int?,
    onMethodSelect: (RepaymentMethod) -> Unit,
    onDayTypeSelect: (RepaymentDayType) -> Unit,
    onCustomDaySelect: (Int) -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text("Step 6 - 상환 방식")
    }
}