package com.billage.app.ui.settings

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.billage.app.data.auth.WithdrawalManager
import com.billage.app.data.local.AuthSessionStorage
import com.billage.app.ui.theme.BillageBackground
import com.billage.app.ui.theme.BillageDanger
import com.billage.app.ui.theme.BillageSurface
import com.billage.app.ui.theme.BillageTextPrimary
import com.billage.app.ui.theme.BillageTextSecondary
import kotlinx.coroutines.launch

private const val LOG_TAG = "WithdrawalSettings"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WithdrawalSettingsScreen(
    onBackClick: () -> Unit = {},
    onWithdrawSuccess: () -> Unit = {},
    isHomeScreenProtectionEnabled: Boolean = true,
    onHomeScreenProtectionChanged: (Boolean) -> Unit = {}
) {
    val applicationContext = LocalContext.current.applicationContext
    val coroutineScope = rememberCoroutineScope()
    val withdrawalManager = remember(applicationContext) {
        WithdrawalManager(
            authSessionStorage = AuthSessionStorage(applicationContext)
        )
    }

    var showWithdrawDialog by rememberSaveable { mutableStateOf(false) }
    var isWithdrawing by rememberSaveable { mutableStateOf(false) }
    var errorMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var successMessage by rememberSaveable { mutableStateOf<String?>(null) }

    fun submitWithdrawal() {
        if (isWithdrawing) {
            return
        }

        isWithdrawing = true
        errorMessage = null
        coroutineScope.launch {
            when (val result = withdrawalManager.withdraw()) {
                is WithdrawalManager.WithdrawalResult.Success -> {
                    showWithdrawDialog = false
                    successMessage = result.message
                }

                is WithdrawalManager.WithdrawalResult.Blocked -> {
                    showWithdrawDialog = false
                    errorMessage = result.message
                }

                is WithdrawalManager.WithdrawalResult.Failure -> {
                    showWithdrawDialog = false
                    errorMessage = result.message
                    if (result.error != null) {
                        Log.w(LOG_TAG, "Withdraw request failed", result.error)
                    }
                }
            }
            isWithdrawing = false
        }
    }

    Scaffold(
        containerColor = BillageBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "설정",
                        color = BillageTextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBackClick,
                        enabled = !isWithdrawing
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "뒤로가기",
                            tint = BillageTextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BillageSurface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            HomeScreenProtectionCard(
                isEnabled = isHomeScreenProtectionEnabled,
                onEnabledChange = onHomeScreenProtectionChanged,
                enabled = !isWithdrawing
            )

            Text(
                text = "계정 관리",
                color = BillageTextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = BillageSurface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = BillageDanger.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = "주의",
                            color = BillageDanger,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }

                    Text(
                        text = "회원 탈퇴",
                        color = BillageTextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "회원 탈퇴 후에는 계정과 연결된 서비스 이용이 중단됩니다. 탈퇴 전에 필요한 정보가 있다면 먼저 확인해주세요.",
                        color = BillageTextSecondary,
                        fontSize = 14.sp,
                        lineHeight = 21.sp
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = Color(0xFFFFF1F1),
                        shape = RoundedCornerShape(18.dp)
                    )
                    .padding(18.dp)
            ) {
                Text(
                    text = "진행 중인 계약이 하나라도 있으면 회원 탈퇴가 불가능합니다.\n계약을 모두 정리한 뒤 다시 시도해주세요.",
                    color = BillageDanger,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = 20.sp
                )
            }

            if (!errorMessage.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = Color(0xFFFFF7E8),
                            shape = RoundedCornerShape(18.dp)
                        )
                        .padding(18.dp)
                ) {
                    Text(
                        text = errorMessage.orEmpty(),
                        color = BillageTextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        lineHeight = 20.sp
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = { showWithdrawDialog = true },
                enabled = !isWithdrawing,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = BillageDanger,
                    contentColor = Color.White,
                    disabledContainerColor = BillageDanger.copy(alpha = 0.5f),
                    disabledContentColor = Color.White.copy(alpha = 0.85f)
                )
            ) {
                if (isWithdrawing) {
                    CircularProgressIndicator(
                        color = Color.White,
                        strokeWidth = 2.dp,
                        modifier = Modifier.height(20.dp)
                    )
                } else {
                    Text(
                        text = "회원 탈퇴",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

    if (showWithdrawDialog) {
        AlertDialog(
            onDismissRequest = {
                if (!isWithdrawing) {
                    showWithdrawDialog = false
                }
            },
            title = {
                Text(
                    text = "회원 탈퇴 진행",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "진행 중인 계약이 있으면 탈퇴할 수 없습니다.\n정말 회원 탈퇴를 진행하시겠습니까?"
                )
            },
            dismissButton = {
                TextButton(
                    onClick = { showWithdrawDialog = false },
                    enabled = !isWithdrawing
                ) {
                    Text("취소", color = BillageTextSecondary)
                }
            },
            confirmButton = {
                TextButton(
                    onClick = ::submitWithdrawal,
                    enabled = !isWithdrawing
                ) {
                    if (isWithdrawing) {
                        CircularProgressIndicator(
                            color = BillageDanger,
                            strokeWidth = 2.dp,
                            modifier = Modifier.height(18.dp)
                        )
                    } else {
                        Text(
                            text = "탈퇴 진행",
                            color = BillageDanger,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        )
    }

    if (!successMessage.isNullOrBlank()) {
        AlertDialog(
            onDismissRequest = {},
            title = {
                Text(
                    text = "회원 탈퇴 완료",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(successMessage.orEmpty())
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        successMessage = null
                        onWithdrawSuccess()
                    }
                ) {
                    Text(
                        text = "확인",
                        color = BillageDanger,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        )
    }
}

@Composable
private fun HomeScreenProtectionCard(
    isEnabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    enabled: Boolean
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = BillageSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "홈 화면 보호",
                color = BillageTextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "홈 화면에서 스크린샷, 화면 녹화, 일부 미러링을 차단합니다.",
                color = BillageTextSecondary,
                fontSize = 14.sp,
                lineHeight = 21.sp
            )
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = Color(0xFFF4F7FB)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = if (isEnabled) "보호 켜짐" else "보호 꺼짐",
                        color = BillageTextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = if (isEnabled) {
                            "홈 화면 캡처와 공유 화면 노출을 막고 있습니다."
                        } else {
                            "미러링과 화면공유 시 홈 화면이 정상 표시됩니다."
                        },
                        color = BillageTextSecondary,
                        fontSize = 13.sp,
                        lineHeight = 19.sp
                    )
                    Box(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Switch(
                            checked = isEnabled,
                            onCheckedChange = onEnabledChange,
                            enabled = enabled,
                            modifier = Modifier.align(androidx.compose.ui.Alignment.CenterEnd)
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun WithdrawalSettingsScreenPreview() {
    WithdrawalSettingsScreen()
}
