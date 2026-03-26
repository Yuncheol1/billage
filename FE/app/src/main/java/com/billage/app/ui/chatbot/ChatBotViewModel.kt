package com.billage.app.ui.chatbot

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.billage.app.data.remote.api.ChatBotApi
import com.billage.app.data.remote.api.ChatBotMessageDto
import com.billage.app.data.remote.api.ChatBotRequest
import com.billage.app.domain.model.ChatBotMessage
import com.billage.app.domain.model.ChatBotRole
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatBotViewModel @Inject constructor(
    private val chatBotApi: ChatBotApi
) : ViewModel() {

    private val _messages = MutableStateFlow<List<ChatBotMessage>>(
        listOf(
            ChatBotMessage(
                role = ChatBotRole.ASSISTANT,
                content = "안녕하세요! 빌리지 AI 상담사입니다. 😊\n세법, 차용증 작성, 증여세 등 궁금한 점이 있으시면 무엇이든 물어보세요."
            )
        )
    )
    val messages: StateFlow<List<ChatBotMessage>> = _messages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun sendMessage(content: String, contractSeq: Int? = null) {
        if (content.isBlank() || _isLoading.value) return

        val userMessage = ChatBotMessage(role = ChatBotRole.USER, content = content)
        _messages.value = _messages.value + userMessage

        viewModelScope.launch {
            _isLoading.value = true
            try {
                val history = _messages.value.dropLast(1).map {
                    ChatBotMessageDto(
                        role = if (it.role == ChatBotRole.USER) "user" else "assistant",
                        content = it.content
                    )
                }

                val response = chatBotApi.askChatBot(
                    ChatBotRequest(
                        message = content,
                        history = history,
                        contract_seq = contractSeq
                    )
                )

                val assistantMessage = ChatBotMessage(
                    role = ChatBotRole.ASSISTANT,
                    content = response.reply
                )
                _messages.value = _messages.value + assistantMessage
            } catch (e: Exception) {
                val errorMessage = ChatBotMessage(
                    role = ChatBotRole.ASSISTANT,
                    content = "죄송합니다. 서버와 통신 중 오류가 발생했습니다. 잠시 후 다시 시도해 주세요."
                )
                _messages.value = _messages.value + errorMessage
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearHistory() {
        _messages.value = listOf(
            ChatBotMessage(
                role = ChatBotRole.ASSISTANT,
                content = "안녕하세요! 빌리지 AI 상담사입니다. 😊\n세법, 차용증 작성, 증여세 등 궁금한 점이 있으시면 무엇이든 물어보세요."
            )
        )
    }
}
