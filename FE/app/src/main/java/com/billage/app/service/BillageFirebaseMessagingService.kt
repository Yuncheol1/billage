package com.billage.app.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import androidx.core.content.edit
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.billage.app.MainActivity
import com.billage.app.R
import com.billage.app.data.local.AuthSessionStorage
import com.billage.app.data.remote.api.FcmTokenRequest
import com.billage.app.data.remote.api.NotificationApi
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class BillageFirebaseMessagingService : FirebaseMessagingService() {

    @Inject
    lateinit var notificationApi: NotificationApi

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM_TOKEN", "Refreshed token: $token")
        AuthSessionStorage(applicationContext).saveFcmToken(token)
        val sharedPref = getSharedPreferences("fcm_prefs", Context.MODE_PRIVATE)
        sharedPref.edit {
            putString("fcm_token", token)
        }
        if (AuthSessionStorage(applicationContext).hasSession()) {
            CoroutineScope(Dispatchers.IO).launch {
                runCatching { notificationApi.registerToken(FcmTokenRequest(token)) }
                    .onFailure { Log.e("FCM_TOKEN", "Failed to sync refreshed token", it) }
            }
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d("FCM_MSG", "From: ${remoteMessage.from}")

        // Check if message contains a data payload.
        if (remoteMessage.data.isNotEmpty()) {
            Log.d("FCM_MSG", "Message data payload: ${remoteMessage.data}")
            // Handle the data payload (e.g., custom notification logic)
        }

        // Check if message contains a notification payload.
        remoteMessage.notification?.let {
            Log.d("FCM_MSG", "Message Notification Body: ${it.body}")
        }

        showLocalNotification(remoteMessage)
    }

    private fun showLocalNotification(remoteMessage: RemoteMessage) {
        createNotificationChannel()

        val contractSeq = remoteMessage.data["contractSeq"]?.toIntOrNull()
        val notificationSeq = remoteMessage.data["notificationSeq"]?.toLongOrNull()

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(MainActivity.EXTRA_OPEN_NOTIFICATIONS, true)
            contractSeq?.let { putExtra(MainActivity.EXTRA_CONTRACT_SEQ, it) }
            notificationSeq?.let { putExtra(MainActivity.EXTRA_NOTIFICATION_SEQ, it) }
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            (notificationSeq ?: System.currentTimeMillis()).toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = remoteMessage.notification?.title
            ?: remoteMessage.data["title"]
            ?: getString(R.string.app_name)
        val body = remoteMessage.notification?.body
            ?: remoteMessage.data["body"]
            ?: "새로운 알림이 도착했어요."

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.billage_logo)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        runCatching {
            NotificationManagerCompat.from(this)
                .notify((notificationSeq ?: System.currentTimeMillis()).toInt(), notification)
        }.onFailure {
            Log.e("FCM_MSG", "Failed to show local notification", it)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return
        }

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Billage 알림",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "상환, 계약, 알림함 관련 푸시 알림"
        }
        manager.createNotificationChannel(channel)
    }

    companion object {
        private const val CHANNEL_ID = "billage_notifications"
    }
}
