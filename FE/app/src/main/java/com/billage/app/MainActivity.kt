package com.billage.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.billage.app.data.local.AuthSessionStorage
import com.billage.app.data.remote.api.FcmTokenRequest
import com.billage.app.data.remote.api.NotificationApi
import com.billage.app.ui.navigation.AppDestination
import com.billage.app.ui.navigation.AppNavHost
import com.billage.app.ui.theme.BillageTheme
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    @Inject
    lateinit var notificationApi: NotificationApi

    private lateinit var authSessionStorage: AuthSessionStorage
    private var startDestination by mutableStateOf(AppDestination.Login.route)

    // 알림 권한 런처
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            syncFcmToken()
        } else {
            Toast.makeText(this, "알림을 허용하지 않으면 중요한 일정을 놓칠 수 있습니다.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        authSessionStorage = AuthSessionStorage(applicationContext)
        startDestination = if (authSessionStorage.hasSession()) {
            if (authSessionStorage.getUserState().equals("PENDING", ignoreCase = true)) {
                AppDestination.ConditionsOfUse.route
            } else {
                askNotificationPermission()
                AppDestination.Home.route
            }
        } else {
            AppDestination.Login.route
        }
        handleAuthCallback(intent)

        enableEdgeToEdge()
        setContent {
            BillageTheme {
                AppNavHost(startDestination = startDestination)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleAuthCallback(intent)
    }

    private fun handleAuthCallback(intent: Intent?) {
        val data = intent?.data ?: return
        if (!data.isAuthCallbackUri()) {
            return
        }

        val error = data.getQueryParameter("error")
        if (!error.isNullOrBlank()) {
            val description = data.getQueryParameter("error_description") ?: "Kakao login failed."
            Toast.makeText(this, description, Toast.LENGTH_SHORT).show()
            startDestination = AppDestination.Login.route
            return
        }

        val accessToken = data.getQueryParameter("accessToken")
        val refreshToken = data.getQueryParameter("refreshToken")
        if (accessToken.isNullOrBlank() || refreshToken.isNullOrBlank()) {
            Toast.makeText(this, "Invalid login response.", Toast.LENGTH_SHORT).show()
            startDestination = AppDestination.Login.route
            return
        }

        val authResult = AuthSessionStorage.AuthResult(
            accessToken = accessToken,
            refreshToken = refreshToken,
            accessTokenExpiresIn = data.getQueryParameter("accessTokenExpiresIn")?.toLongOrNull() ?: 0L,
            refreshTokenExpiresIn = data.getQueryParameter("refreshTokenExpiresIn")?.toLongOrNull() ?: 0L,
            isNewUser = data.getQueryParameter("isNewUser").toBoolean(),
            userSeq = data.getQueryParameter("userSeq")?.toIntOrNull(),
            userState = data.getQueryParameter("userState").orEmpty()
        )

        authSessionStorage.save(authResult)
        startDestination = if (authResult.isNewUser || authResult.userState.equals("PENDING", ignoreCase = true)) {
            AppDestination.ConditionsOfUse.route
        } else {
            askNotificationPermission()
            AppDestination.Home.route
        }
    }

    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
            ) {
                syncFcmToken()
            } else {
                // 권한 요청
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            // 안드로이드 12 이하: 자동 부여됨
            syncFcmToken()
        }
    }

    private fun syncFcmToken() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w("FCM", "Fetching FCM registration token failed", task.exception)
                return@addOnCompleteListener
            }

            val token = task.result
            Log.d("FCM", "FCM Token: $token")
            
            // 토큰을 백엔드로 비동기 전송
            lifecycleScope.launch {
                try {
                    notificationApi.registerToken(FcmTokenRequest(token))
                    Log.d("FCM", "Token successfully registered to backend")
                } catch (e: Exception) {
                    Log.e("FCM", "Failed to register backend token", e)
                }
            }
        }
    }
}

private fun Uri.isAuthCallbackUri(): Boolean =
    scheme == "billage" && host == "auth" && path == "/callback"
