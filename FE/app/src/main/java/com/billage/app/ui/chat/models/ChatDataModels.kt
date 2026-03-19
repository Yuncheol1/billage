package com.billage.app.ui.chat.models

import kotlinx.serialization.Serializable

@Serializable
data class ChatParticipant(
    val userSeq: Int,
    val name: String
)

@Serializable
data class ChatRoomRes(
    val id: String,
    val participants: List<ChatParticipant>,
    val lastMessage: String? = null,
    val lastModifiedAt: String? = null,
    val unreadCount: Long = 0
)

@Serializable
data class ChatMessageReq(
    val roomId: String,
    val senderSeq: Int,
    val content: String,
    val isContractMsg: Boolean = false
)

@Serializable
data class ChatMessageRes(
    val id: String,
    val roomId: String,
    val senderSeq: Int,
    val senderName: String,
    val content: String,
    val isContractMsg: Boolean,
    val createdAt: String
)
@Serializable
data class UserSearchResponse(
    val userSeq: Int,
    val email: String,
    val realName: String
)
