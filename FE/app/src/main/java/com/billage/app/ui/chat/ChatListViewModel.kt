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
import com.billage.app.data.remote.api.UserApi
import com.billage.app.data.local.AuthSessionStorage
import com.billage.app.ui.chat.models.UserSearchResponse
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged

@OptIn(FlowPreview::class)
@HiltViewModel
class ChatListViewModel @Inject constructor(
    private val chatApi: ChatApi,
    private val userApi: UserApi,
    private val authSessionStorage: AuthSessionStorage
) : ViewModel() {

    val myUserSeq: Int get() = authSessionStorage.getUserSeq()

    private val _rooms = MutableStateFlow<List<ChatRoomRes>>(emptyList())
    val rooms: StateFlow<List<ChatRoomRes>> = _rooms.asStateFlow()

    private val _userSearchResults = MutableStateFlow<List<UserSearchResponse>>(emptyList())
    val userSearchResults: StateFlow<List<UserSearchResponse>> = _userSearchResults.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    init {
        fetchRooms()
        viewModelScope.launch {
            searchQuery
                .debounce(300L)
                .distinctUntilChanged()
                .collect { query ->
                    if (query.length >= 2) {
                        try {
                            _userSearchResults.value = userApi.searchUsersByEmail(query)
                                .filter { it.userSeq != myUserSeq }
                        } catch (e: Exception) {
                            Log.e("ChatListViewModel", "Search failed", e)
                            _userSearchResults.value = emptyList()
                        }
                    } else {
                        _userSearchResults.value = emptyList()
                    }
                }
        }
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
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
