package com.billage.app.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.billage.app.data.remote.api.ChatApi
import com.billage.app.data.remote.api.ChatRoomCreateReq
import com.billage.app.ui.chat.models.ChatRoomRes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import android.util.Log

@HiltViewModel
class ChatListViewModel @Inject constructor(
    private val chatApi: ChatApi
) : ViewModel() {

    val myUserSeq: Int get() = com.billage.app.ui.chat.models.TestSessionManager.myUserSeq.value

    private val _rooms = MutableStateFlow<List<ChatRoomRes>>(emptyList())
    val rooms: StateFlow<List<ChatRoomRes>> = _rooms.asStateFlow()

    init {
        fetchRooms()
    }

    fun fetchRooms() {
        viewModelScope.launch {
            try {
                val roomList = chatApi.getMyChatRooms(myUserSeq)
                _rooms.value = roomList
            } catch (e: Exception) {
                Log.e("ChatListViewModel", "Failed to fetch rooms", e)
                _rooms.value = emptyList()
            }
        }
    }

    fun createRoomByEmail(targetEmail: String, onSuccess: (String, String) -> Unit, onError: () -> Unit) {
        viewModelScope.launch {
            try {
                val newRoom = chatApi.createRoomByEmail(myUserSeq, ChatRoomCreateReq(targetEmail))
                fetchRooms() // Refresh list
                val opponentName = newRoom.participants.firstOrNull { it.userSeq != myUserSeq }?.name ?: "알 수 없음"
                onSuccess(newRoom.id, opponentName)
            } catch (e: Exception) {
                Log.e("ChatListViewModel", "Failed to create room", e)
                onError()
            }
        }
    }

    fun leaveRoom(roomId: String) {
        viewModelScope.launch {
            try {
                chatApi.leaveRoom(myUserSeq, roomId)
                fetchRooms() // Refresh list
            } catch (e: Exception) {
                Log.e("ChatListViewModel", "Failed to leave room", e)
            }
        }
    }
}
