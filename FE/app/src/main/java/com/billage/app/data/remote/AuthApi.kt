package com.billage.app.data.remote

import com.billage.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

class AuthApi(
    private val baseUrl: String = BuildConfig.BACKEND_BASE_URL,
    private val json: Json = Json { ignoreUnknownKeys = true }
) {

    suspend fun getConsents(accessToken: String): Result<ConsentListResponseData> = withContext(Dispatchers.IO) {
        runCatching {
            val connection = createConnection(
                path = "/api/v1/auth/consents",
                method = "GET",
                accessToken = accessToken
            )

            try {
                val responseCode = connection.responseCode
                val responseBody = readResponseBody(connection, responseCode)
                if (responseCode !in 200..299) {
                    throw IOException("Get consents failed with HTTP $responseCode: $responseBody")
                }

                json.decodeFromString<GetConsentsResponse>(responseBody).data
            } finally {
                connection.disconnect()
            }
        }
    }

    suspend fun completeConsents(
        accessToken: String,
        consents: List<ConsentSubmissionItem>
    ): Result<ConsentCompletionData> = withContext(Dispatchers.IO) {
        runCatching {
            val connection = createConnection(
                path = "/api/v1/auth/consents",
                method = "POST",
                accessToken = accessToken,
                doOutput = true
            )

            try {
                val requestBody = json.encodeToString(CompleteConsentsRequest(consents = consents))
                connection.outputStream.bufferedWriter(Charsets.UTF_8).use { writer ->
                    writer.write(requestBody)
                }

                val responseCode = connection.responseCode
                val responseBody = readResponseBody(connection, responseCode)
                if (responseCode !in 200..299) {
                    throw IOException("Complete consents failed with HTTP $responseCode: $responseBody")
                }

                json.decodeFromString<CompleteConsentsResponse>(responseBody).data
            } finally {
                connection.disconnect()
            }
        }
    }

    suspend fun logout(
        refreshToken: String,
        fcmToken: String? = null
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val connection = createConnection(
                path = "/api/v1/auth/logout",
                method = "POST",
                doOutput = true
            )
            try {
                val requestBody = json.encodeToString(
                    LogoutRequest(
                        refreshToken = refreshToken,
                        fcmToken = fcmToken
                    )
                )

                connection.outputStream.bufferedWriter(Charsets.UTF_8).use { writer ->
                    writer.write(requestBody)
                }

                val responseCode = connection.responseCode
                val responseBody = readResponseBody(connection, responseCode)
                if (responseCode !in 200..299) {
                    throw IOException("Logout failed with HTTP $responseCode: $responseBody")
                }
            } finally {
                connection.disconnect()
            }
        }
    }

    private fun createConnection(
        path: String,
        method: String,
        accessToken: String? = null,
        doOutput: Boolean = false
    ): HttpURLConnection {
        val requestUrl = URL(baseUrl.trimEnd('/') + path)
        return (requestUrl.openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 10_000
            readTimeout = 10_000
            this.doOutput = doOutput
            setRequestProperty("Accept", "application/json")
            if (doOutput) {
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            }
            if (!accessToken.isNullOrBlank()) {
                setRequestProperty("Authorization", "Bearer $accessToken")
            }
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

    @Serializable
    private data class LogoutRequest(
        val refreshToken: String,
        val fcmToken: String? = null
    )

    @Serializable
    private data class GetConsentsResponse(
        val status: Int,
        val message: String,
        val data: ConsentListResponseData
    )

    @Serializable
    private data class CompleteConsentsRequest(
        val consents: List<ConsentSubmissionItem>
    )

    @Serializable
    private data class CompleteConsentsResponse(
        val status: Int,
        val message: String,
        val data: ConsentCompletionData
    )
}

@Serializable
data class ConsentListResponseData(
    val userSeq: Int,
    val userState: String,
    val consents: List<ConsentListItem>
)

@Serializable
data class ConsentListItem(
    val termsSeq: Int,
    val termsKey: String,
    val termsType: String,
    val termsVersion: String,
    val termsContent: String,
    val agreed: Boolean
)

@Serializable
data class ConsentSubmissionItem(
    val termsSeq: Int,
    val agreed: Boolean
)

@Serializable
data class ConsentCompletionData(
    val userSeq: Int,
    val userState: String
)
