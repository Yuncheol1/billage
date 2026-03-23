package com.billage.app.ui.chat

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.billage.app.data.remote.api.ChatApi
import com.billage.app.data.remote.api.ContractApi
import com.billage.app.data.remote.api.ContractListResponse
import com.billage.app.data.repository.ChatRepository
import com.billage.app.ui.chat.models.ChatMessageRes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.hildan.krossbow.stomp.StompSession
import org.hildan.krossbow.stomp.headers.StompSubscribeHeaders
import com.billage.app.data.local.AuthSessionStorage
import javax.inject.Inject

@HiltViewModel
class ChatRoomViewModel @Inject constructor(
    private val chatApi: ChatApi,
    private val contractApi: ContractApi,
    private val chatRepository: ChatRepository,
    private val authSessionStorage: AuthSessionStorage
) : ViewModel() {

    companion object {
        private const val TAG = "ChatRoomViewModel"
    }

    val myUserSeq: Int get() = authSessionStorage.getUserSeq()

    private val _messages = MutableStateFlow<List<ChatMessageRes>>(emptyList())
    val messages: StateFlow<List<ChatMessageRes>> = _messages.asStateFlow()

    private val _contracts = MutableStateFlow<List<ContractListResponse>>(emptyList())
    val contracts: StateFlow<List<ContractListResponse>> = _contracts.asStateFlow()

    private var currentRoomId: String = ""

    fun joinRoom(roomId: String) {
        if (currentRoomId == roomId && _messages.value.isNotEmpty()) return 
        currentRoomId = roomId
        Log.d(TAG, "Joining room: $roomId")

        viewModelScope.launch {
            // 0. 읽음 처리
            try {
                chatApi.markAsRead(myUserSeq, roomId)
                Log.d(TAG, "Marked as read: $roomId")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to mark as read", e)
            }

            // 1. 과거 메시지 내역 로드 (REST API)
            try {
                val history = chatApi.getChatHistory(roomId)
                _messages.value = history
                Log.d(TAG, "History loaded: ${history.size} messages")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load history", e)
            }

            // 2. STOMP 세션 관찰 및 구독
            chatRepository.session.collect { session ->
                session?.let {
                    Log.d(TAG, "Session ready, subscribing to /sub/chat/room/$roomId")
                    subscribeToRoom(it, roomId)
                }
            }
        }
    }

    private fun subscribeToRoom(session: StompSession, roomId: String) {
        viewModelScope.launch {
            try {
                val subscription = session.subscribe(StompSubscribeHeaders(destination = "/sub/chat/room/$roomId"))
                Log.d(TAG, "Subscribed successfully to $roomId")

                subscription.collect { frame ->
                    val payload = frame.bodyAsText
                    Log.d(TAG, "Received frame: $payload")
                    try {
                        val json = Json { ignoreUnknownKeys = true }
                        val receivedMsg = json.decodeFromString<ChatMessageRes>(payload)

                        _messages.update { currentList ->
                            if (currentList.any { it.id == receivedMsg.id }) currentList
                            else currentList + receivedMsg
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Decode error", e)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Subscription error", e)
                chatRepository.clearSession()
                chatRepository.connect()
            }
        }
    }

    fun sendMessage(content: String) {
        if (currentRoomId.isEmpty() || content.isBlank()) return
        chatRepository.sendMessage(currentRoomId, myUserSeq, content)
    }

    fun fetchContracts() {
        viewModelScope.launch {
            try {
                val res = contractApi.getContracts()
                if (res.status == 200) {
                    _contracts.value = res.data
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load contracts", e)
            }
        }
    }
}
