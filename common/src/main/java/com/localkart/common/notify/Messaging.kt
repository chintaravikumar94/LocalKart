package com.localkart.common.notify

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.localkart.common.auth.AuthManager
import com.localkart.common.repo.FirestoreRepo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/** Receives FCM messages and shows a notification; saves new tokens to the user doc. */
class LocalKartMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        val uid = AuthManager.currentUid ?: return
        CoroutineScope(Dispatchers.IO).launch {
            runCatching { FirestoreRepo().saveFcmToken(uid, token) }
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val title = message.notification?.title ?: message.data["title"] ?: "LocalKart"
        val body = message.notification?.body ?: message.data["body"] ?: ""
        Notifications.show(this, title, body)
    }
}

object Notifications {
    const val CHANNEL_ID = "localkart_default"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val mgr = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (mgr.getNotificationChannel(CHANNEL_ID) == null) {
                mgr.createNotificationChannel(
                    NotificationChannel(CHANNEL_ID, "LocalKart", NotificationManager.IMPORTANCE_HIGH)
                )
            }
        }
    }

    fun show(context: Context, title: String, body: String) {
        ensureChannel(context)
        val notif = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        val mgr = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        mgr.notify(System.currentTimeMillis().toInt(), notif)
    }
}

/** Fetches the current FCM token and stores it on the signed-in user's doc. */
object PushTokens {
    suspend fun register() {
        val uid = AuthManager.currentUid ?: return
        runCatching {
            val token = FirebaseMessaging.getInstance().token.await()
            FirestoreRepo().saveFcmToken(uid, token)
        }
    }
}
