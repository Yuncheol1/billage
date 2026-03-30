package com.billage.app.data.auth

import com.billage.app.data.local.AuthSessionStorage
import com.billage.app.data.remote.AccountApi

class WithdrawalManager(
    private val authSessionStorage: AuthSessionStorage,
    private val accountApi: AccountApi = AccountApi()
) {

    suspend fun withdraw(): WithdrawalResult {
        val accessToken = authSessionStorage.getAccessToken()
        if (accessToken.isNullOrBlank()) {
            return WithdrawalResult.Failure("로그인 정보가 없습니다. 다시 로그인해주세요.")
        }

        return accountApi.withdraw(accessToken).fold(
            onSuccess = { response ->
                authSessionStorage.clear()
                WithdrawalResult.Success(response.message)
            },
            onFailure = { error ->
                when (error) {
                    is AccountApi.ApiException -> {
                        if (error.statusCode == 409) {
                            WithdrawalResult.Blocked(error.message)
                        } else {
                            WithdrawalResult.Failure(error.message, error)
                        }
                    }

                    else -> WithdrawalResult.Failure(
                        message = error.message ?: "회원 탈퇴에 실패했습니다.",
                        error = error
                    )
                }
            }
        )
    }

    sealed interface WithdrawalResult {
        data class Success(val message: String) : WithdrawalResult
        data class Blocked(val message: String) : WithdrawalResult
        data class Failure(
            val message: String,
            val error: Throwable? = null
        ) : WithdrawalResult
    }
}
