package com.ramen73.ramenchat

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.ramen73.ramenchat.utils.FirebaseUtils
import com.ramen73.ramenchat.utils.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class RamenMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        CoroutineScope(Dispatchers.IO).launch {
            FirebaseUtils.saveFcmToken(token)
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        val title = message.notification?.title
            ?: message.data["title"]
            ?: "RamenChat"
        val body = message.notification?.body
            ?: message.data["body"]
            ?: "Nuovo messaggio"
        NotificationHelper.showMessageNotification(this, title, body)
    }
}
