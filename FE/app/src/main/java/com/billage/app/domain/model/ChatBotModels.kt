package com.billage.app.domain.model

import kotlinx.serialization.Serializable

enum class ChatBotRole {
    USER, ASSISTANT
}

@Serializable
data class ChatBotMessage(
    val role: ChatBotRole,
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
)
