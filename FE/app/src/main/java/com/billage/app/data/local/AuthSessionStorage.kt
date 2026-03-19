package com.billage.app.data.local

import android.content.Context
import androidx.core.content.edit

class AuthSessionStorage(context: Context) {

    private val preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun hasSession(): Boolean {
        return !preferences.getString(KEY_ACCESS_TOKEN, null).isNullOrBlank()
    }

    fun getAccessToken(): String? = preferences.getString(KEY_ACCESS_TOKEN, null)

    fun getRefreshToken(): String? = preferences.getString(KEY_REFRESH_TOKEN, null)

    fun getUserState(): String? = preferences.getString(KEY_USER_STATE, null)

    fun save(authResult: AuthResult) {
        preferences.edit {
            putString(KEY_ACCESS_TOKEN, authResult.accessToken)
                .putString(KEY_REFRESH_TOKEN, authResult.refreshToken)
                .putLong(KEY_ACCESS_TOKEN_EXPIRES_IN, authResult.accessTokenExpiresIn)
                .putLong(KEY_REFRESH_TOKEN_EXPIRES_IN, authResult.refreshTokenExpiresIn)
                .putBoolean(KEY_IS_NEW_USER, authResult.isNewUser)
                .putInt(KEY_USER_SEQ, authResult.userSeq ?: -1)
                .putString(KEY_USER_STATE, authResult.userState)
        }
    }

    fun clear() {
        preferences.edit { clear() }
    }

    fun updateUserState(userState: String) {
        preferences.edit {
            putString(KEY_USER_STATE, userState)
                .putBoolean(KEY_IS_NEW_USER, false)
        }
    }

    data class AuthResult(
        val accessToken: String,
        val refreshToken: String,
        val accessTokenExpiresIn: Long,
        val refreshTokenExpiresIn: Long,
        val isNewUser: Boolean,
        val userSeq: Int?,
        val userState: String
    )

    fun getUserSeq(): Int = preferences.getInt(KEY_USER_SEQ, -1)

    companion object {
        private const val PREFS_NAME = "auth_session"
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_ACCESS_TOKEN_EXPIRES_IN = "access_token_expires_in"
        private const val KEY_REFRESH_TOKEN_EXPIRES_IN = "refresh_token_expires_in"
        private const val KEY_IS_NEW_USER = "is_new_user"
        private const val KEY_USER_SEQ = "user_seq"
        private const val KEY_USER_STATE = "user_state"
    }
}
