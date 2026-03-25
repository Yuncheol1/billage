package com.billage.app.di

import com.billage.app.BuildConfig
import com.billage.app.data.local.AuthSessionStorage
import com.billage.app.data.remote.TokenReissueData
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.Response
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.locks.ReentrantLock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthInterceptor @Inject constructor(
    private val authSessionStorage: AuthSessionStorage
) : Interceptor {

    private val refreshLock = ReentrantLock()
    private val json = Json { ignoreUnknownKeys = true }

    override fun intercept(chain: Interceptor.Chain): Response {
        val token = authSessionStorage.getAccessToken()

        // header()로 설정해야 기존 헤더를 덮어씀 (addHeader는 중복 추가)
        val request = chain.request().newBuilder().apply {
            if (!token.isNullOrBlank()) {
                header("Authorization", "Bearer $token")
            }
        }.build()

        val response = chain.proceed(request)

        // 인증 오류 판별:
        // - 401: 표준 Unauthorized
        // - 500 + "userSeq" 포함: 서버가 401 대신 500을 내려주는 경우 (JWT 만료 시 필터가 통과시켜 컨트롤러에서 크래시)
        val shouldRefresh = when {
            response.code == 401 -> true
            response.code == 500 -> {
                val body = response.peekBody(2048).string()
                body.contains("userSeq") && body.contains("Missing request attribute")
            }
            else -> false
        }

        if (shouldRefresh) {
            response.close()
            val newToken = refreshTokenSync() ?: return chain.proceed(request)
            val retryRequest = request.newBuilder()
                .header("Authorization", "Bearer $newToken")
                .build()
            return chain.proceed(retryRequest)
        }

        return response
    }

    /**
     * refresh token으로 access token 재발급.
     * 여러 요청이 동시에 401을 받아도 refresh는 한 번만 수행.
     */
    private fun refreshTokenSync(): String? {
        refreshLock.lock()
        return try {
            // 다른 스레드가 이미 갱신했을 수 있으므로 현재 저장된 토큰 재확인
            val currentToken = authSessionStorage.getAccessToken()
            val refreshToken = authSessionStorage.getRefreshToken()

            if (refreshToken.isNullOrBlank()) {
                authSessionStorage.clear()
                return null
            }

            val baseUrl = BuildConfig.BACKEND_BASE_URL.trimEnd('/')
            val url = URL("$baseUrl/api/v1/auth/reissue")
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                setRequestProperty("Accept", "application/json")
                doOutput = true
                connectTimeout = 10_000
                readTimeout = 10_000
            }

            val body = """{"refreshToken":"$refreshToken"}"""
            connection.outputStream.bufferedWriter(Charsets.UTF_8).use { it.write(body) }

            val responseCode = connection.responseCode
            if (responseCode !in 200..299) {
                // refresh token도 만료 → 세션 초기화 (앱 재시작 후 재로그인 필요)
                authSessionStorage.clear()
                return null
            }

            val responseText = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            val data = json.decodeFromString<ReissueResponse>(responseText).data

            authSessionStorage.saveReissuedSession(
                accessToken = data.accessToken,
                refreshToken = data.refreshToken,
                accessTokenExpiresIn = data.accessTokenExpiresIn,
                refreshTokenExpiresIn = data.refreshTokenExpiresIn,
                userState = data.userState
            )

            data.accessToken
        } catch (e: Exception) {
            null
        } finally {
            refreshLock.unlock()
        }
    }

    @Serializable
    private data class ReissueResponse(
        val status: Int,
        val message: String,
        val data: TokenReissueData
    )
}
