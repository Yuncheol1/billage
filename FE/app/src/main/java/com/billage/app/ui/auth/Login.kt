package com.billage.app.ui.auth

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.billage.app.BuildConfig
import com.billage.app.R
import com.billage.app.data.local.AuthSessionStorage
import com.billage.app.data.remote.AuthApi
import com.billage.app.ui.theme.BillagePrimary
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    onBiometricLoginSuccess: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val applicationContext = context.applicationContext
    val activity = context as? FragmentActivity
    val authSessionStorage = remember(applicationContext) { AuthSessionStorage(applicationContext) }
    val authApi = remember { AuthApi() }
    val coroutineScope = rememberCoroutineScope()
    val biometricLoginEnabled = remember(applicationContext) {
        authSessionStorage.shouldUseBiometricLogin() &&
            BiometricLoginManager.isAuthenticationAvailable(applicationContext)
    }

    var isBiometricLoading by rememberSaveable { mutableStateOf(false) }
    var biometricMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var autoPromptRequested by rememberSaveable { mutableStateOf(false) }
    var showEnrollmentDialog by rememberSaveable { mutableStateOf(false) }

    fun completeBiometricLogin() {
        val refreshToken = authSessionStorage.getRefreshToken()
        if (refreshToken.isNullOrBlank()) {
            biometricMessage = "Saved login session is unavailable."
            authSessionStorage.setBiometricLoginEnabled(false)
            isBiometricLoading = false
            return
        }

        coroutineScope.launch {
            authApi.reissue(refreshToken)
                .onSuccess { response ->
                    authSessionStorage.saveReissuedSession(
                        accessToken = response.accessToken,
                        refreshToken = response.refreshToken,
                        accessTokenExpiresIn = response.accessTokenExpiresIn,
                        refreshTokenExpiresIn = response.refreshTokenExpiresIn,
                        userState = response.userState
                    )
                    isBiometricLoading = false
                    biometricMessage = null
                    onBiometricLoginSuccess(response.userState)
                }
                .onFailure { throwable ->
                    isBiometricLoading = false
                    biometricMessage = throwable.message ?: "Biometric sign in failed."
                }
        }
    }

    fun launchBiometricLogin() {
        if (!biometricLoginEnabled || isBiometricLoading) {
            return
        }

        if (activity == null) {
            biometricMessage = "Biometric sign in is not available in this screen."
            return
        }

        biometricMessage = null
        isBiometricLoading = true
        BiometricLoginManager.prompt(
            activity = activity,
            onSuccess = ::completeBiometricLogin,
            onError = { message ->
                isBiometricLoading = false
                biometricMessage = message
            },
            onEnrollmentRequired = {
                isBiometricLoading = false
                showEnrollmentDialog = true
            }
        )
    }

    LaunchedEffect(biometricLoginEnabled) {
        if (biometricLoginEnabled && !autoPromptRequested) {
            autoPromptRequested = true
            launchBiometricLogin()
        }
    }

    if (showEnrollmentDialog) {
        AlertDialog(
            onDismissRequest = { showEnrollmentDialog = false },
            title = { Text("Biometric setup required", fontWeight = FontWeight.Bold) },
            text = {
                Text("Register a screen lock and fingerprint on this device, then try biometric sign in again.")
            },
            confirmButton = {
                TextButton(onClick = { showEnrollmentDialog = false }) {
                    Text("OK", color = BillagePrimary, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = Color(0xFF60CBC3)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(BillagePrimary)
                .padding(horizontal = 28.dp, vertical = 32.dp)
        ) {
            Spacer(modifier = Modifier.height(176.dp))

            Text(
                text = "Billage",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 64.sp,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Text(
                text = "쉽게 빌려주고 빌려요",
                color = Color.White.copy(alpha = 0.92f),
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                modifier = Modifier
                    .padding(top = 8.dp)
                    .align(Alignment.CenterHorizontally)
            )

            Box(
                modifier = Modifier
                    .padding(top = 20.dp)
                    .align(Alignment.CenterHorizontally)
                    .border(
                        width = 2.dp,
                        color = Color.White.copy(alpha = 0.55f),
                        shape = RoundedCornerShape(30.dp)
                    )
                    .padding(horizontal = 28.dp, vertical = 10.dp)
            ) {
                Text(
                    text = "상세 보기",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            if (biometricLoginEnabled) {
                OutlinedButton(
                    onClick = ::launchBiometricLogin,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    enabled = !isBiometricLoading,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.65f)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color.White
                    )
                ) {
                    if (isBiometricLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = Color.White
                        )
                    } else {
                        Text(
                            text = "지문으로 로그인",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }

                if (!biometricMessage.isNullOrBlank()) {
                    Text(
                        text = biometricMessage.orEmpty(),
                        color = Color.White,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(top = 10.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
            }

            Image(
                painter = painterResource(id = R.drawable.kakao_login_medium_wide),
                contentDescription = "Kakao login",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clickable {
                        startKakaoLogin(context)
                    }
            )
            Spacer(modifier = Modifier.height(12.dp))

            Spacer(modifier = Modifier.height(48.dp))
            Spacer(modifier = Modifier.height(38.dp))
        }
    }
}

private fun startKakaoLogin(context: Context) {
    val authorizeUri = Uri.parse("${BuildConfig.BACKEND_BASE_URL}/api/v1/auth/kakao/authorize")
    val intent = Intent(Intent.ACTION_VIEW, authorizeUri)

    try {
        context.startActivity(intent)
    } catch (error: ActivityNotFoundException) {
        Log.e("KakaoLogin", "Browser launch failed", error)
        Toast.makeText(context, "Cannot open browser.", Toast.LENGTH_SHORT).show()
    }
}
