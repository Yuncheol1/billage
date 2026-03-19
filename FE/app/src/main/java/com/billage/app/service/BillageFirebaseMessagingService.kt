package com.billage.app.service

import android.content.Context
import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import androidx.core.content.edit

class BillageFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM_TOKEN", "Refreshed token: $token")
        // TODO: Send token to your app server.
        // This will be handled in MainActivity or Login ViewModel when user is authenticated.
        // We save it locally here if needed, or rely on FirebaseMessaging.getInstance().token later.
        val sharedPref = getSharedPreferences("fcm_prefs", Context.MODE_PRIVATE)
        sharedPref.edit {
            putString("fcm_token", token)
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
            // Firebase handles displaying this automatically if the app is in the background.
            // If the app is in the foreground, we would normally display a custom notification here.
        }
    }
}
