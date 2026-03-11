package com.billage.app.ui.contract.steps

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.billage.app.ui.contract.ContractFormState

@Composable
fun Step8PreviewScreen(
    formState: ContractFormState,
    onEditStep: (Int) -> Unit,
    onSubmit: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text("Step 8 - 미리보기")
    }
}