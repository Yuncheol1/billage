package com.billage.app.ui.notification

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.billage.app.data.remote.api.NotificationItemResponse
import com.billage.app.data.repository.NotificationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NotificationUiState(
    val isLoading: Boolean = false,
    val notifications: List<NotificationItemResponse> = emptyList(),
    val unreadCount: Int = 0,
    val errorMessage: String? = null
)

@HiltViewModel
class NotificationViewModel @Inject constructor(
    private val notificationRepository: NotificationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotificationUiState())
    val uiState: StateFlow<NotificationUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            notificationRepository.unreadCount.collect { unreadCount ->
                _uiState.update { it.copy(unreadCount = unreadCount) }
            }
        }
        fetchNotifications()
    }

    fun fetchNotifications() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            runCatching { notificationRepository.fetchNotifications() }
                .onSuccess { notifications ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            notifications = notifications,
                            unreadCount = notifications.count { item -> !item.isRead }
                        )
                    }
                }
                .onFailure {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "알림을 불러오지 못했습니다."
                        )
                    }
                }
        }
    }

    fun markAsRead(notificationSeq: Long) {
        viewModelScope.launch {
            runCatching { notificationRepository.markAsRead(notificationSeq) }
                .onSuccess {
                    _uiState.update { state ->
                        state.copy(
                            notifications = state.notifications.map { notification ->
                                if (notification.notificationSeq == notificationSeq) {
                                    notification.copy(isRead = true)
                                } else {
                                    notification
                                }
                            }
                        )
                    }
                }
        }
    }

    fun markAllAsRead() {
        viewModelScope.launch {
            runCatching { notificationRepository.markAllAsRead() }
                .onSuccess {
                    _uiState.update { state ->
                        state.copy(
                            unreadCount = 0,
                            notifications = state.notifications.map { it.copy(isRead = true) }
                        )
                    }
                }
        }
    }
}
