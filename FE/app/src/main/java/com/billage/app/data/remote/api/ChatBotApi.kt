package com.billage.app.data.remote.api

import retrofit2.http.Body
import retrofit2.http.POST
import kotlinx.serialization.Serializable

@Serializable
data class ChatBotRequest(
    val message: String,
    val history: List<ChatBotMessageDto>,
    val contract_seq: Int? = null
)

@Serializable
data class ChatBotMessageDto(
    val role: String,
    val content: String
)

@Serializable
data class ChatBotResponse(
    val reply: String
)

interface ChatBotApi {
    @POST("api/v1/chatbot")
    suspend fun askChatBot(
        @Body request: ChatBotRequest
    ): ChatBotResponse
}
