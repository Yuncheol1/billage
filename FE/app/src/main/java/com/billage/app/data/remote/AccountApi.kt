package com.billage.app.data.remote

import com.billage.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

class AccountApi(
    private val baseUrl: String = BuildConfig.BACKEND_BASE_URL,
    private val json: Json = Json { ignoreUnknownKeys = true }
) {

    suspend fun withdraw(accessToken: String): Result<WithdrawResponse> = withContext(Dispatchers.IO) {
        runCatching {
            val connection = createConnection(
                path = "/api/v1/users/me",
                method = "DELETE",
                accessToken = accessToken
            )

            try {
                val responseCode = connection.responseCode
                val responseBody = readResponseBody(connection, responseCode)
                val message = extractMessage(
                    responseBody = responseBody,
                    fallback = if (responseCode in 200..299) {
                        "회원 탈퇴가 완료되었습니다."
                    } else {
                        "회원 탈퇴에 실패했습니다."
                    }
                )

                if (responseCode !in 200..299) {
                    throw ApiException(
                        statusCode = responseCode,
                        message = message
                    )
                }

                WithdrawResponse(message = message)
            } finally {
                connection.disconnect()
            }
        }
    }

    private fun createConnection(
        path: String,
        method: String,
        accessToken: String
    ): HttpURLConnection {
        val requestUrl = URL(baseUrl.trimEnd('/') + path)
        return (requestUrl.openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 10_000
            readTimeout = 10_000
            setRequestProperty("Accept", "application/json")
            setRequestProperty("Authorization", "Bearer $accessToken")
        }
    }

    private fun readResponseBody(connection: HttpURLConnection, responseCode: Int): String {
        val stream = if (responseCode in 200..299) {
            connection.inputStream
        } else {
            connection.errorStream
        }

        return stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
    }

    private fun extractMessage(responseBody: String, fallback: String): String {
        return runCatching {
            json.parseToJsonElement(responseBody)
                .jsonObject["message"]
                ?.jsonPrimitive
                ?.contentOrNull
                ?.takeIf { it.isNotBlank() }
                ?: fallback
        }.getOrElse { fallback }
    }

    data class WithdrawResponse(
        val message: String
    )

    class ApiException(
        val statusCode: Int,
        override val message: String
    ) : IOException(message)
}
