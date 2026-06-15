package com.github.im.group.notification

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import io.github.aakira.napier.Napier

class GroupFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Napier.i("Received new FCM token")
        AndroidPushEndpointRegistrar(applicationContext).syncTokenAsync(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        Napier.i(
            "Received FCM message: from=${message.from}, dataKeys=${message.data.keys}, hasNotification=${message.notification != null}"
        )

        val notificationManager = AndroidPushNotificationManager(applicationContext)

        when {
            message.data.isNotEmpty() -> notificationManager.showFromDataPayload(message.data)
            message.notification != null -> notificationManager.showFromDataPayload(
                mapOf(
                    "title" to (message.notification?.title ?: "Group IM"),
                    "body" to (message.notification?.body ?: "You have a new notification")
                )
            )
        }
    }
}
