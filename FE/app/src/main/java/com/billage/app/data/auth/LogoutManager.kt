package com.billage.app.data.auth

import com.billage.app.data.local.AuthSessionStorage
import com.billage.app.data.remote.AuthApi

class LogoutManager(
    private val authSessionStorage: AuthSessionStorage,
    private val authApi: AuthApi = AuthApi()
) {

    suspend fun logout(): LogoutResult {
        val refreshToken = authSessionStorage.getRefreshToken()
        val apiResult = if (refreshToken.isNullOrBlank()) {
            Result.success(Unit)
        } else {
            authApi.logout(refreshToken = refreshToken)
        }

        authSessionStorage.clear()

        return LogoutResult(
            remoteSucceeded = apiResult.isSuccess,
            error = apiResult.exceptionOrNull()
        )
    }

    data class LogoutResult(
        val remoteSucceeded: Boolean,
        val error: Throwable? = null
    )
}
