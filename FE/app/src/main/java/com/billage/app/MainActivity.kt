package com.billage.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.billage.app.data.local.AuthSessionStorage
import com.billage.app.ui.navigation.AppDestination
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import com.billage.app.ui.navigation.AppNavHost
import com.billage.app.ui.theme.BillageTheme

@AndroidEntryPoint
class MainActivity : FragmentActivity() {
private lateinit var authSessionStorage: AuthSessionStorage
    private var startDestination by mutableStateOf(AppDestination.Login.route)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        authSessionStorage = AuthSessionStorage(applicationContext)
        startDestination = if (authSessionStorage.hasSession()) {
            if (authSessionStorage.getUserState().equals("PENDING", ignoreCase = true)) {
                AppDestination.ConditionsOfUse.route
            } else {
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
            AppDestination.Home.route
        }
    }
}

private fun Uri.isAuthCallbackUri(): Boolean =
    scheme == "billage" && host == "auth" && path == "/callback"
