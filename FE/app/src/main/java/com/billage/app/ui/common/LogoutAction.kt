package com.billage.app.ui.common

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.billage.app.data.auth.LogoutManager
import com.billage.app.data.local.AuthSessionStorage
import kotlinx.coroutines.launch

private const val LOG_TAG = "LogoutAction"

data class LogoutUiState(
    val isLoggingOut: Boolean,
    val onLogoutClick: () -> Unit
)

@Composable
fun rememberLogoutUiState(
    onLoggedOut: () -> Unit
): LogoutUiState {
    val applicationContext = LocalContext.current.applicationContext
    val coroutineScope = rememberCoroutineScope()
    val logoutManager = remember(applicationContext) {
        LogoutManager(
            authSessionStorage = AuthSessionStorage(applicationContext)
        )
    }
    var isLoggingOut by rememberSaveable { mutableStateOf(false) }

    fun handleLogoutClick() {
        if (isLoggingOut) {
            return
        }

        isLoggingOut = true
        coroutineScope.launch {
            val result = logoutManager.logout()
            if (!result.remoteSucceeded && result.error != null) {
                Log.w(LOG_TAG, "Remote logout request failed", result.error)
            }
            isLoggingOut = false
            onLoggedOut()
        }
    }

    return LogoutUiState(
        isLoggingOut = isLoggingOut,
        onLogoutClick = ::handleLogoutClick
    )
}
