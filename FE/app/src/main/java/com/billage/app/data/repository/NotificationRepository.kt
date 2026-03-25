package com.billage.app.data.repository

import com.billage.app.data.local.AuthSessionStorage
import com.billage.app.data.remote.api.NotificationApi
import com.billage.app.data.remote.api.NotificationItemResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationRepository @Inject constructor(
    private val notificationApi: NotificationApi,
    private val authSessionStorage: AuthSessionStorage
) {
    private val _unreadCount = MutableStateFlow(0)
    val unreadCount: StateFlow<Int> = _unreadCount.asStateFlow()

    suspend fun fetchNotifications(): List<NotificationItemResponse> {
        if (!authSessionStorage.hasSession()) {
            _unreadCount.value = 0
            return emptyList()
        }
        val response = notificationApi.getNotifications()
        _unreadCount.value = response.data.count { !it.isRead }
        return response.data
    }

    suspend fun refreshUnreadCount(): Int {
        if (!authSessionStorage.hasSession()) {
            _unreadCount.value = 0
            return 0
        }
        val count = notificationApi.getUnreadCount().data.unreadCount.toInt()
        _unreadCount.value = count
        return count
    }

    suspend fun markAsRead(notificationSeq: Long) {
        if (!authSessionStorage.hasSession()) {
            return
        }
        notificationApi.markAsRead(notificationSeq)
        refreshUnreadCount()
    }

    suspend fun markAllAsRead() {
        if (!authSessionStorage.hasSession()) {
            return
        }
        notificationApi.markAllAsRead()
        _unreadCount.value = 0
    }
}
