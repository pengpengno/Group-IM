package com.github.im.group.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.pm.PackageManager
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.github.im.group.MainActivity
import com.github.im.group.R
import io.github.aakira.napier.Napier
import org.json.JSONObject

class AndroidPushNotificationManager(
    private val context: Context
) {

    companion object {
        const val CHANNEL_CHAT = "chat_messages"
        const val CHANNEL_CALLS = "calls"
    }

    fun ensureChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return
        }

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channels = listOf(
            NotificationChannel(
                CHANNEL_CHAT,
                "Chat messages",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Chat message notifications"
            },
            NotificationChannel(
                CHANNEL_CALLS,
                "Calls and meetings",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Incoming call and meeting invite notifications"
            }
        )

        manager.createNotificationChannels(channels)
    }

    fun showFromDataPayload(data: Map<String, String>) {
        ensureChannels()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            Napier.i("Skip Android system notification because POST_NOTIFICATIONS is not granted.")
            return
        }

        val eventType = data["eventType"] ?: data["type"].orEmpty()
        val title = data["title"] ?: data["senderName"] ?: "Group IM"
        val body = data["body"] ?: data["preview"] ?: "You have a new notification"
        val deepLink = data["deepLink"]
        val roomId = data["roomId"]
        val callerId = data["senderId"]
        val callerName = data["senderName"] ?: title
        val clickAction = data["clickAction"]
        val notificationKind = data["notificationKind"]
        val channelId = if (isMeetingInvite(eventType)) CHANNEL_CALLS else CHANNEL_CHAT

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(
                if (channelId == CHANNEL_CALLS) NotificationCompat.PRIORITY_HIGH
                else NotificationCompat.PRIORITY_DEFAULT
            )
            .setCategory(
                if (channelId == CHANNEL_CALLS) NotificationCompat.CATEGORY_CALL
                else NotificationCompat.CATEGORY_MESSAGE
            )
            .setAutoCancel(true)
            .setContentIntent(
                buildCallPendingIntent(
                    action = resolveContentAction(channelId, clickAction),
                    deepLink = deepLink,
                    roomId = roomId,
                    callerId = callerId,
                    callerName = callerName,
                    clickAction = clickAction,
                    notificationKind = notificationKind
                )
            )

        if (channelId == CHANNEL_CALLS && !roomId.isNullOrBlank()) {
            builder.addAction(buildCallAction(
                title = "Accept",
                action = AndroidCallNotificationContract.ACTION_ACCEPT_CALL,
                deepLink = deepLink,
                roomId = roomId,
                callerId = callerId,
                callerName = callerName,
                clickAction = clickAction,
                notificationKind = notificationKind
            ))
            builder.addAction(buildCallAction(
                title = "Reject",
                action = AndroidCallNotificationContract.ACTION_REJECT_CALL,
                deepLink = deepLink,
                roomId = roomId,
                callerId = callerId,
                callerName = callerName,
                clickAction = clickAction,
                notificationKind = notificationKind
            ))
        }

        val notification = builder.build()

        val notificationId = (data["messageId"] ?: data["eventId"] ?: eventType).hashCode()
        NotificationManagerCompat.from(context).notify(notificationId, notification)

        Napier.i("Displayed Android push notification: eventType=$eventType, channelId=$channelId")
    }

    fun showFromJsonPayload(jsonPayload: String) {
        runCatching {
            val json = JSONObject(jsonPayload)
            val data = buildMap {
                val iterator = json.keys()
                while (iterator.hasNext()) {
                    val key = iterator.next()
                    put(key, json.optString(key))
                }
            }
            showFromDataPayload(data)
        }.onFailure {
            Napier.e("Failed to parse push payload json", it)
        }
    }

    private fun buildCallAction(
        title: String,
        action: String,
        deepLink: String?,
        roomId: String?,
        callerId: String?,
        callerName: String?,
        clickAction: String?,
        notificationKind: String?
    ): NotificationCompat.Action {
        return NotificationCompat.Action.Builder(
            0,
            title,
            buildCallPendingIntent(action, deepLink, roomId, callerId, callerName, clickAction, notificationKind)
        ).build()
    }

    private fun buildCallPendingIntent(
        action: String,
        deepLink: String?,
        roomId: String?,
        callerId: String?,
        callerName: String?,
        clickAction: String?,
        notificationKind: String?
    ): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            this.action = action
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(AndroidCallNotificationContract.EXTRA_DEEP_LINK, deepLink)
            putExtra(AndroidCallNotificationContract.EXTRA_ROOM_ID, roomId)
            putExtra(AndroidCallNotificationContract.EXTRA_CALLER_ID, callerId)
            putExtra(AndroidCallNotificationContract.EXTRA_CALLER_NAME, callerName)
            putExtra(AndroidCallNotificationContract.EXTRA_CLICK_ACTION, clickAction)
            putExtra(AndroidCallNotificationContract.EXTRA_NOTIFICATION_KIND, notificationKind)
        }

        return PendingIntent.getActivity(
            context,
            listOf(action, deepLink, roomId, callerId, callerName, clickAction, notificationKind).joinToString("#").hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun resolveContentAction(channelId: String, clickAction: String?): String {
        return when {
            clickAction.equals("open_meeting", ignoreCase = true) -> AndroidCallNotificationContract.ACTION_OPEN_CALL
            channelId == CHANNEL_CALLS -> AndroidCallNotificationContract.ACTION_OPEN_CALL
            else -> Intent.ACTION_VIEW
        }
    }

    private fun isMeetingInvite(eventType: String): Boolean {
        return eventType.equals("MEETING_INVITE_CREATED", ignoreCase = true)
            || eventType.equals("meeting.invite.created", ignoreCase = true)
    }
}
