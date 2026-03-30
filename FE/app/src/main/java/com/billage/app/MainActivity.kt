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
import com.billage.app.data.repository.NotificationRepository
import com.billage.app.ui.auth.BiometricLoginManager
import com.billage.app.data.remote.api.FcmTokenRequest
import com.billage.app.data.remote.api.NotificationApi
import com.billage.app.ui.navigation.AppDestination
import com.billage.app.ui.navigation.AppNavHost
import com.billage.app.ui.navigation.NotificationNavigationTarget
import com.billage.app.ui.theme.BillageTheme
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    @Inject
    lateinit var notificationApi: NotificationApi

    @Inject
    lateinit var notificationRepository: NotificationRepository

    private lateinit var authSessionStorage: AuthSessionStorage
    private var startDestination by mutableStateOf(AppDestination.Login.route)
    private var pendingNotificationTarget by mutableStateOf<NotificationNavigationTarget?>(null)
    private var isHomeScreenProtectionEnabled by mutableStateOf(true)

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
        isHomeScreenProtectionEnabled = authSessionStorage.isHomeScreenProtectionEnabled()
        startDestination = resolveStartDestination()
        handleAuthCallback(intent)
        handleNotificationNavigation(intent)

        enableEdgeToEdge()
        setContent {
            BillageTheme {
                AppNavHost(
                    startDestination = startDestination,
                    isHomeScreenProtectionEnabled = isHomeScreenProtectionEnabled,
                    onHomeScreenProtectionChanged = { enabled ->
                        authSessionStorage.setHomeScreenProtectionEnabled(enabled)
                        isHomeScreenProtectionEnabled = enabled
                    },
                    pendingNotificationTarget = pendingNotificationTarget,
                    onNotificationTargetConsumed = { pendingNotificationTarget = null }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleAuthCallback(intent)
        handleNotificationNavigation(intent)
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
        authSessionStorage.setBiometricLoginEnabled(
            BiometricLoginManager.isAuthenticationAvailable(applicationContext)
        )
        startDestination = resolveAuthenticatedDestination(
            isNewUser = authResult.isNewUser,
            userState = authResult.userState
        )
    }

    private fun resolveStartDestination(): String {
        if (authSessionStorage.shouldUseBiometricLogin()) {
            return AppDestination.Login.route
        }

        if (!authSessionStorage.hasSession()) {
            return AppDestination.Login.route
        }

        return resolveAuthenticatedDestination(
            isNewUser = false,
            userState = authSessionStorage.getUserState().orEmpty()
        )
    }

    private fun resolveAuthenticatedDestination(
        isNewUser: Boolean,
        userState: String
    ): String {
        return if (isNewUser || userState.equals("PENDING", ignoreCase = true)) {
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
            authSessionStorage.saveFcmToken(token)

            // 토큰을 백엔드로 비동기 전송
            lifecycleScope.launch {
                try {
                    notificationApi.registerToken(FcmTokenRequest(token))
                    Log.d("FCM", "Token successfully registered to backend")
                    notificationRepository.refreshUnreadCount()
                } catch (e: Exception) {
                    Log.e("FCM", "Failed to register backend token", e)
                }
            }
        }
    }

    private fun handleNotificationNavigation(intent: Intent?) {
        if (!authSessionStorage.hasSession()) {
            return
        }

        val contractSeq = intent
            ?.takeIf { it.hasExtra(EXTRA_CONTRACT_SEQ) }
            ?.getIntExtra(EXTRA_CONTRACT_SEQ, -1)
            ?.takeIf { it > 0 }
        val notificationSeq = intent
            ?.takeIf { it.hasExtra(EXTRA_NOTIFICATION_SEQ) }
            ?.getLongExtra(EXTRA_NOTIFICATION_SEQ, -1L)
            ?.takeIf { it > 0L }

        if (notificationSeq != null) {
            lifecycleScope.launch {
                runCatching { notificationRepository.markAsRead(notificationSeq) }
            }
        }

        if (contractSeq != null) {
            pendingNotificationTarget = NotificationNavigationTarget(contractSeq = contractSeq)
        } else if (intent?.getBooleanExtra(EXTRA_OPEN_NOTIFICATIONS, false) == true) {
            pendingNotificationTarget = NotificationNavigationTarget(openNotifications = true)
        }
    }

    companion object {
        const val EXTRA_CONTRACT_SEQ = "extra_contract_seq"
        const val EXTRA_NOTIFICATION_SEQ = "extra_notification_seq"
        const val EXTRA_OPEN_NOTIFICATIONS = "extra_open_notifications"
    }
}

private fun Uri.isAuthCallbackUri(): Boolean =
    scheme == "billage" && host == "auth" && path == "/callback"
