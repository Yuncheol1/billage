package com.billage.app.ui.auth

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.billage.app.data.local.AuthSessionStorage
import com.billage.app.data.remote.AuthApi
import com.billage.app.data.remote.ConsentSubmissionItem
import com.billage.app.ui.theme.BillagePrimary
import kotlinx.coroutines.launch

@Composable
fun ConditionsOfUseScreen(
    onBackClick: () -> Unit,
    onAgreeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val applicationContext = LocalContext.current.applicationContext
    val authSessionStorage = remember(applicationContext) { AuthSessionStorage(applicationContext) }
    val authApi = remember { AuthApi() }
    val coroutineScope = rememberCoroutineScope()

    var consentItems by remember { mutableStateOf<List<ConsentUiItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isSubmitting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        val accessToken = authSessionStorage.getAccessToken()
        if (accessToken.isNullOrBlank()) {
            errorMessage = "Login required."
            isLoading = false
            return@LaunchedEffect
        }

        authApi.getConsents(accessToken)
            .onSuccess { response ->
                authSessionStorage.updateUserState(response.userState)
                if (response.userState.equals("ACTIVE", ignoreCase = true)) {
                    onAgreeClick()
                    return@onSuccess
                }

                consentItems = response.consents.map { consent ->
                    ConsentUiItem(
                        termsSeq = consent.termsSeq,
                        title = consent.termsKey.toDisplayTitle(),
                        required = consent.termsType.equals("mandatory", ignoreCase = true),
                        checked = consent.agreed
                    )
                }
            }
            .onFailure { throwable ->
                errorMessage = throwable.message ?: "Failed to load terms."
            }

        isLoading = false
    }

    val requiredItems = consentItems.filter { it.required }
    val allRequiredChecked = requiredItems.isNotEmpty() && requiredItems.all { it.checked }
    val allChecked = consentItems.isNotEmpty() && consentItems.all { it.checked }

    fun toggleAll() {
        val next = !allChecked
        consentItems = consentItems.map { it.copy(checked = next) }
    }

    fun toggleConsent(termsSeq: Int) {
        consentItems = consentItems.map { item ->
            if (item.termsSeq == termsSeq) item.copy(checked = !item.checked) else item
        }
    }

    fun submitConsents() {
        val accessToken = authSessionStorage.getAccessToken()
        if (accessToken.isNullOrBlank()) {
            errorMessage = "Login required."
            return
        }

        isSubmitting = true
        errorMessage = null
        coroutineScope.launch {
            authApi.completeConsents(
                accessToken = accessToken,
                consents = consentItems.map { ConsentSubmissionItem(it.termsSeq, it.checked) }
            ).onSuccess { response ->
                authSessionStorage.updateUserState(response.userState)
                onAgreeClick()
            }.onFailure { throwable ->
                errorMessage = throwable.message ?: "Failed to save terms."
            }
            isSubmitting = false
        }
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = BillagePrimary
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(BillagePrimary)
                .padding(horizontal = 24.dp, vertical = 96.dp)
        ) {
            Text(
                text = "Billage",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 38.sp,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Text(
                text = "Consent is required to continue.",
                color = Color.White.copy(alpha = 0.9f),
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                modifier = Modifier
                    .padding(top = 8.dp)
                    .align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(32.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White, RoundedCornerShape(16.dp))
                    .padding(18.dp)
            ) {
                Column {
                    TermsCheckRow(
                        title = "전체 동의",
                        required = false,
                        checked = allChecked,
                        onClick = ::toggleAll,
                        titleFontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    DividerLine()
                    Spacer(modifier = Modifier.height(12.dp))

                    when {
                        isLoading -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = BillagePrimary)
                            }
                        }

                        consentItems.isEmpty() -> {
                            Text(
                                text = errorMessage ?: "No terms available.",
                                color = Color(0xFF1F2A28),
                                fontSize = 14.sp
                            )
                        }

                        else -> {
                            consentItems.forEachIndexed { index, item ->
                                TermsCheckRow(
                                    title = item.title,
                                    required = item.required,
                                    checked = item.checked,
                                    onClick = { toggleConsent(item.termsSeq) }
                                )
                                if (index != consentItems.lastIndex) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (!errorMessage.isNullOrBlank() && consentItems.isNotEmpty()) {
                Text(
                    text = errorMessage.orEmpty(),
                    color = Color.White,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            Spacer(modifier = Modifier.weight(1f))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .background(
                        color = if (allRequiredChecked && !isSubmitting && !isLoading) {
                            Color(0xFFF3D54E)
                        } else {
                            Color(0xFFB6DAD6)
                        },
                        shape = RoundedCornerShape(8.dp)
                    )
                    .clickable(
                        enabled = allRequiredChecked && !isSubmitting && !isLoading,
                        onClick = ::submitConsents
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isSubmitting) "저장 중..." else "동의하고 계속하기",
                    color = Color(0xFF25312F),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }
    }
}

private data class ConsentUiItem(
    val termsSeq: Int,
    val title: String,
    val required: Boolean,
    val checked: Boolean
)

private fun String.toDisplayTitle(): String = when (this) {
    "service_terms" -> "서비스 이용약관"
    "privacy_policy" -> "개인정보 수집 및 이용 동의"
    "marketing_notifications" -> "마케팅 정보 수신 동의"
    "personalized_recommendations" -> "맞춤형 추천 제공 동의"
    else -> replace("_", " ")
}

@Composable
private fun TermsCheckRow(
    title: String,
    required: Boolean,
    checked: Boolean,
    onClick: () -> Unit,
    titleFontWeight: FontWeight = FontWeight.Medium
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .background(
                        color = if (checked) BillagePrimary else Color(0xFFE8EEED),
                        shape = RoundedCornerShape(6.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "V",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }

            Text(
                text = title,
                color = Color(0xFF1F2A28),
                fontWeight = titleFontWeight,
                fontSize = 15.sp,
                modifier = Modifier.padding(start = 10.dp)
            )
        }

        if (required) {
            Text(
                text = "(필수)",
                color = Color(0xFF3D8C84),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun DividerLine() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(Color(0xFFE6EEEE))
    )
}
