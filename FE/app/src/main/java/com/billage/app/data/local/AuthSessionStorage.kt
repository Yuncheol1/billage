package com.billage.app.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class AuthSessionStorage(context: Context) {

    private val appContext = context.applicationContext

    private val legacyPreferences: SharedPreferences =
        appContext.getSharedPreferences(LEGACY_PREFS_NAME, Context.MODE_PRIVATE)

    private val preferences: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(appContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            appContext,
            ENCRYPTED_PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        ).also(::migrateLegacyPreferencesIfNeeded)
    }

    fun hasSession(): Boolean {
        return !preferences.getString(KEY_ACCESS_TOKEN, null).isNullOrBlank()
    }

    fun getAccessToken(): String? = preferences.getString(KEY_ACCESS_TOKEN, null)

    fun getRefreshToken(): String? = preferences.getString(KEY_REFRESH_TOKEN, null)

    fun getStoredFcmToken(): String? = preferences.getString(KEY_FCM_TOKEN, null)

    fun getUserState(): String? = preferences.getString(KEY_USER_STATE, null)

    fun getUserName(): String? = preferences.getString(KEY_USER_NAME, null)

    fun isBiometricLoginEnabled(): Boolean =
        preferences.getBoolean(KEY_BIOMETRIC_LOGIN_ENABLED, false)

    fun shouldUseBiometricLogin(): Boolean {
        return isBiometricLoginEnabled() && !getRefreshToken().isNullOrBlank()
    }

    fun save(authResult: AuthResult) {
        preferences.edit {
            putString(KEY_ACCESS_TOKEN, authResult.accessToken)
            putString(KEY_REFRESH_TOKEN, authResult.refreshToken)
            putLong(KEY_ACCESS_TOKEN_EXPIRES_IN, authResult.accessTokenExpiresIn)
            putLong(KEY_REFRESH_TOKEN_EXPIRES_IN, authResult.refreshTokenExpiresIn)
            putBoolean(KEY_IS_NEW_USER, authResult.isNewUser)
            putInt(KEY_USER_SEQ, authResult.userSeq ?: -1)
            putString(KEY_USER_STATE, authResult.userState)
        }
    }

    fun saveReissuedSession(
        accessToken: String,
        refreshToken: String,
        accessTokenExpiresIn: Long,
        refreshTokenExpiresIn: Long,
        userState: String
    ) {
        preferences.edit {
            putString(KEY_ACCESS_TOKEN, accessToken)
            putString(KEY_REFRESH_TOKEN, refreshToken)
            putLong(KEY_ACCESS_TOKEN_EXPIRES_IN, accessTokenExpiresIn)
            putLong(KEY_REFRESH_TOKEN_EXPIRES_IN, refreshTokenExpiresIn)
            putString(KEY_USER_STATE, userState)
            putBoolean(KEY_IS_NEW_USER, false)
        }
    }

    fun clear() {
        preferences.edit { clear() }
    }

    fun saveFcmToken(token: String) {
        preferences.edit {
            putString(KEY_FCM_TOKEN, token)
        }
    }

    fun setBiometricLoginEnabled(enabled: Boolean) {
        preferences.edit {
            putBoolean(KEY_BIOMETRIC_LOGIN_ENABLED, enabled)
        }
    }

    fun saveUserName(name: String) {
        preferences.edit {
            putString(KEY_USER_NAME, name)
        }
    }

    fun updateUserState(userState: String) {
        preferences.edit {
            putString(KEY_USER_STATE, userState)
            putBoolean(KEY_IS_NEW_USER, false)
        }
    }

    fun getUserSeq(): Int = preferences.getInt(KEY_USER_SEQ, -1)

    private fun migrateLegacyPreferencesIfNeeded(encryptedPreferences: SharedPreferences) {
        if (encryptedPreferences.getBoolean(KEY_MIGRATION_COMPLETED, false)) {
            return
        }

        if (!legacyPreferences.containsAnyTrackedKey()) {
            encryptedPreferences.edit { putBoolean(KEY_MIGRATION_COMPLETED, true) }
            return
        }

        encryptedPreferences.edit {
            legacyPreferences.getString(KEY_ACCESS_TOKEN, null)?.let {
                putString(KEY_ACCESS_TOKEN, it)
            }
            legacyPreferences.getString(KEY_REFRESH_TOKEN, null)?.let {
                putString(KEY_REFRESH_TOKEN, it)
            }
            if (legacyPreferences.contains(KEY_ACCESS_TOKEN_EXPIRES_IN)) {
                putLong(
                    KEY_ACCESS_TOKEN_EXPIRES_IN,
                    legacyPreferences.getLong(KEY_ACCESS_TOKEN_EXPIRES_IN, 0L)
                )
            }
            if (legacyPreferences.contains(KEY_REFRESH_TOKEN_EXPIRES_IN)) {
                putLong(
                    KEY_REFRESH_TOKEN_EXPIRES_IN,
                    legacyPreferences.getLong(KEY_REFRESH_TOKEN_EXPIRES_IN, 0L)
                )
            }
            if (legacyPreferences.contains(KEY_IS_NEW_USER)) {
                putBoolean(
                    KEY_IS_NEW_USER,
                    legacyPreferences.getBoolean(KEY_IS_NEW_USER, false)
                )
            }
            if (legacyPreferences.contains(KEY_USER_SEQ)) {
                putInt(KEY_USER_SEQ, legacyPreferences.getInt(KEY_USER_SEQ, -1))
            }
            legacyPreferences.getString(KEY_USER_STATE, null)?.let {
                putString(KEY_USER_STATE, it)
            }
            if (legacyPreferences.contains(KEY_BIOMETRIC_LOGIN_ENABLED)) {
                putBoolean(
                    KEY_BIOMETRIC_LOGIN_ENABLED,
                    legacyPreferences.getBoolean(KEY_BIOMETRIC_LOGIN_ENABLED, false)
                )
            }
            legacyPreferences.getString(KEY_FCM_TOKEN, null)?.let {
                putString(KEY_FCM_TOKEN, it)
            }
            legacyPreferences.getString(KEY_USER_NAME, null)?.let {
                putString(KEY_USER_NAME, it)
            }
            putBoolean(KEY_MIGRATION_COMPLETED, true)
        }

        legacyPreferences.edit { clear() }
    }

    private fun SharedPreferences.containsAnyTrackedKey(): Boolean {
        return TRACKED_KEYS.any(::contains)
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

    companion object {
        private const val LEGACY_PREFS_NAME = "auth_session"
        private const val ENCRYPTED_PREFS_NAME = "auth_session_encrypted"

        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_ACCESS_TOKEN_EXPIRES_IN = "access_token_expires_in"
        private const val KEY_REFRESH_TOKEN_EXPIRES_IN = "refresh_token_expires_in"
        private const val KEY_IS_NEW_USER = "is_new_user"
        private const val KEY_USER_SEQ = "user_seq"
        private const val KEY_USER_STATE = "user_state"
        private const val KEY_BIOMETRIC_LOGIN_ENABLED = "biometric_login_enabled"
        private const val KEY_FCM_TOKEN = "fcm_token"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_MIGRATION_COMPLETED = "migration_completed"

        private val TRACKED_KEYS = setOf(
            KEY_ACCESS_TOKEN,
            KEY_REFRESH_TOKEN,
            KEY_ACCESS_TOKEN_EXPIRES_IN,
            KEY_REFRESH_TOKEN_EXPIRES_IN,
            KEY_IS_NEW_USER,
            KEY_USER_SEQ,
            KEY_USER_STATE,
            KEY_BIOMETRIC_LOGIN_ENABLED,
            KEY_FCM_TOKEN,
            KEY_USER_NAME
        )
    }
}
