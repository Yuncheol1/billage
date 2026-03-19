package com.billage.app.data.repository

import android.util.Log
import com.billage.app.BuildConfig
import com.billage.app.data.local.AuthSessionStorage
import com.billage.app.ui.chat.models.ChatMessageReq
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import org.hildan.krossbow.stomp.StompClient
import org.hildan.krossbow.stomp.StompSession
import org.hildan.krossbow.stomp.sendText
import org.hildan.krossbow.websocket.okhttp.OkHttpWebSocketClient
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepository @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val authSessionStorage: AuthSessionStorage
) {
    private val TAG = "ChatRepository"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _session = MutableStateFlow<StompSession?>(null)
    val session: StateFlow<StompSession?> = _session.asStateFlow()

    init {
        connect()
    }

    fun connect() {
        if (_session.value != null) return

        scope.launch {
            try {
                // Use BACKEND_BASE_URL from BuildConfig and convert to ws/wss
                val baseUrl = BuildConfig.BACKEND_BASE_URL.removeSuffix("/")
                val wsBase = if (baseUrl.startsWith("https")) {
                    baseUrl.replace("https", "wss")
                } else {
                    baseUrl.replace("http", "ws")
                }
                val url = "$wsBase/ws-chat"

                Log.d(TAG, "Connecting to STOMP at $url")

                val stompClient = StompClient(OkHttpWebSocketClient(okHttpClient))

                // Add headers for the STOMP CONNECT frame
                val token = authSessionStorage.getAccessToken()
                val stompHeaders = mutableMapOf<String, String>()
                stompHeaders["userSeq"] = authSessionStorage.getUserSeq().toString()
                if (!token.isNullOrBlank()) {
                    stompHeaders["Authorization"] = "Bearer $token"
                }

                // Connect with customStompConnectHeaders
                val stompSession = stompClient.connect(
                    url = url,
                    customStompConnectHeaders = stompHeaders
                )
                _session.value = stompSession
                Log.d(TAG, "STOMP Connected")
            } catch (e: Exception) {
                Log.e(TAG, "STOMP Connection failed", e)
            }
        }
    }

    fun sendMessage(roomId: String, senderSeq: Int, content: String) {
        val currentSession = _session.value
        if (currentSession == null) {
            Log.e(TAG, "Cannot send message: session is null. Attempting to reconnect...")
            connect()
            return
        }

        scope.launch {
            try {
                val req = ChatMessageReq(
                    roomId = roomId,
                    senderSeq = senderSeq,
                    content = content,
                    isContractMsg = false
                )
                val jsonPayload = Json.encodeToString(req)
                currentSession.sendText("/pub/chat/message", jsonPayload)
                Log.d(TAG, "Message sent to $roomId: $content")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send message", e)
            }
        }
    }
}
