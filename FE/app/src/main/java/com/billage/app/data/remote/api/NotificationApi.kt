package com.billage.app.data.remote.api

import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.Path
import retrofit2.http.POST

@Serializable
data class FcmTokenRequest(
    val token: String
)

@Serializable
data class NotificationListApiResponse(
    val status: Int,
    val message: String,
    val data: List<NotificationItemResponse>
)

@Serializable
data class NotificationItemResponse(
    val notificationSeq: Long,
    val eventKey: String,
    val title: String,
    val message: String,
    val isRead: Boolean,
    val sentDttm: String? = null,
    val contractSeq: Int? = null,
    val scheduleSeq: Int? = null
)

@Serializable
data class UnreadCountApiResponse(
    val status: Int,
    val message: String,
    val data: UnreadCountResponse
)

@Serializable
data class UnreadCountResponse(
    val unreadCount: Long
)

@Serializable
data class SimpleApiResponse(
    val status: Int,
    val message: String
)

interface NotificationApi {
    @POST("/api/v1/fcm-tokens")
    suspend fun registerToken(@Body request: FcmTokenRequest)

    @GET("/api/v1/notifications")
    suspend fun getNotifications(): NotificationListApiResponse

    @GET("/api/v1/notifications/unread-count")
    suspend fun getUnreadCount(): UnreadCountApiResponse

    @PATCH("/api/v1/notifications/{notificationSeq}/read")
    suspend fun markAsRead(
        @Path("notificationSeq") notificationSeq: Long
    ): SimpleApiResponse

    @PATCH("/api/v1/notifications/read-all")
    suspend fun markAllAsRead(): SimpleApiResponse
}
