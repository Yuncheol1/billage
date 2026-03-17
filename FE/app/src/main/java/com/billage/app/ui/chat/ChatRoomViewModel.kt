package com.billage.app.ui.chat

import android.os.Build
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.billage.app.data.remote.api.ChatApi
import com.billage.app.ui.chat.models.ChatMessageReq
import com.billage.app.ui.chat.models.ChatMessageRes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import org.hildan.krossbow.stomp.StompClient
import org.hildan.krossbow.stomp.StompSession
import org.hildan.krossbow.stomp.headers.StompSubscribeHeaders
import org.hildan.krossbow.stomp.sendText
import org.hildan.krossbow.websocket.okhttp.OkHttpWebSocketClient
import javax.inject.Inject

@HiltViewModel
class ChatRoomViewModel @Inject constructor(
    private val chatApi: ChatApi
) : ViewModel() {

    companion object {
        private const val TAG = "ChatRoomViewModel"
    }

    val myUserSeq: Int get() = com.billage.app.ui.chat.models.TestSessionManager.myUserSeq.value

    private val _messages = MutableStateFlow<List<ChatMessageRes>>(emptyList())
    val messages: StateFlow<List<ChatMessageRes>> = _messages.asStateFlow()

    private val _stompSession = MutableStateFlow<StompSession?>(null)
    private val stompSession: StateFlow<StompSession?> = _stompSession.asStateFlow()
    private var currentRoomId: String = ""

    init {
        connectStomp()
    }

    private fun connectStomp() {
        viewModelScope.launch {
            try {
                // 실기기(adb reverse)는 localhost, 에뮬레이터는 기본 10.0.2.2
                val isEmulator = Build.FINGERPRINT.contains("generic", ignoreCase = true) || Build.MODEL.contains("sdk", ignoreCase = true)
                val host = if (isEmulator) "10.0.2.2:8080" else "localhost:8080"
                val url = "ws://$host/ws-chat"

                Log.d(TAG, "Connecting to STOMP at $url")
                val okHttpClient = OkHttpClient.Builder().build()
                val stompClient = StompClient(OkHttpWebSocketClient(okHttpClient))

                val session = stompClient.connect(url)
                _stompSession.value = session
                Log.d(TAG, "STOMP Connected successfully")

            } catch (e: Exception) {
                Log.e(TAG, "STOMP Connection failed", e)
            }
        }
    }

    fun joinRoom(roomId: String) {
        currentRoomId = roomId
        Log.d(TAG, "Joining room: $roomId")

        viewModelScope.launch {
            // 1. 과거 메시지 내역 로드 (REST API)
            try {
                val history = chatApi.getChatHistory(roomId)
                _messages.value = history
                Log.d(TAG, "History loaded: ${history.size} messages")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load history", e)
            }

            // 2. STOMP 연결 대기 및 구독
            stompSession.collect { session ->
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
                Log.d(TAG, "Subscribed successfully")

                subscription.collect { frame ->
                    val payload = frame.bodyAsText
                    Log.d(TAG, "Received frame: $payload")
                    if (payload != null) {
                        try {
                            val json = Json { ignoreUnknownKeys = true }
                            val receivedMsg = json.decodeFromString<ChatMessageRes>(payload)
                            
                            // 중복 체크 (이미 기기에 있는 메시지면 무시 - History 로드 시점과 차이가 있을 수 있음)
                            _messages.update { currentList ->
                                if (currentList.any { it.id == receivedMsg.id }) currentList
                                else currentList + receivedMsg
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Decode error", e)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Subscription error", e)
            }
        }
    }

    fun sendMessage(content: String) {
        val session = _stompSession.value
        if (currentRoomId.isEmpty() || content.isBlank() || session == null) {
            Log.w(TAG, "Cannot send: room=$currentRoomId, session=$session")
            return
        }

        viewModelScope.launch {
            try {
                val req = ChatMessageReq(
                    roomId = currentRoomId,
                    senderSeq = myUserSeq,
                    content = content,
                    isContractMsg = false
                )
                val jsonPayload = Json.encodeToString(req)
                Log.d(TAG, "Sending message to /pub/chat/message: $jsonPayload")

                session.sendText("/pub/chat/message", jsonPayload)
            } catch (e: Exception) {
                Log.e(TAG, "Send failed", e)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch {
            _stompSession.value?.disconnect()
        }
    }
}
