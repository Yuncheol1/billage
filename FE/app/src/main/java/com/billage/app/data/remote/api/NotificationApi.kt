package com.billage.app.data.remote.api

import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.POST

@Serializable
data class FcmTokenRequest(
    val token: String
)

interface NotificationApi {
    @POST("/api/v1/fcm-tokens")
    suspend fun registerToken(@Body request: FcmTokenRequest)
}
