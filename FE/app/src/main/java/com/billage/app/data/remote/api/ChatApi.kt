package com.billage.app.data.remote.api

import com.billage.app.ui.chat.models.ChatMessageRes
import com.billage.app.ui.chat.models.ChatRoomRes
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Body
import kotlinx.serialization.Serializable
import retrofit2.http.PATCH

@Serializable
data class ChatRoomCreateReq(val targetEmail: String)

interface ChatApi {
    @GET("/api/v1/chats/{roomId}/messages")
    suspend fun getChatHistory(
        @Path("roomId") roomId: String
    ): List<ChatMessageRes>

    @GET("/api/v1/chats/rooms")
    suspend fun getMyChatRooms(
        @retrofit2.http.Header("userSeq") userSeq: Int
    ): List<ChatRoomRes>

    @POST("/api/v1/chats/rooms")
    suspend fun createRoomByEmail(
        @retrofit2.http.Header("userSeq") userSeq: Int,
        @Body request: ChatRoomCreateReq
    ): ChatRoomRes

    @POST("/api/v1/chats/rooms/user/{targetUserSeq}")
    suspend fun createRoomByUserSeq(
        @retrofit2.http.Header("userSeq") userSeq: Int,
        @Path("targetUserSeq") targetUserSeq: Int
    ): ChatRoomRes

    @POST("/api/v1/chats/{roomId}/leave")
    suspend fun leaveRoom(
        @retrofit2.http.Header("userSeq") userSeq: Int,
        @Path("roomId") roomId: String
    )

    @PATCH("/api/v1/chats/{roomId}/read")
    suspend fun markAsRead(
        @retrofit2.http.Header("userSeq") userSeq: Int,
        @Path("roomId") roomId: String
    )
}
