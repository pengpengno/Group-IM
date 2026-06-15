package com.github.im.group.notification

object AndroidCallNotificationContract {
    const val ACTION_OPEN_CALL = "com.github.im.group.action.OPEN_CALL"
    const val ACTION_ACCEPT_CALL = "com.github.im.group.action.ACCEPT_CALL"
    const val ACTION_REJECT_CALL = "com.github.im.group.action.REJECT_CALL"

    const val EXTRA_ROOM_ID = "room_id"
    const val EXTRA_CALLER_ID = "caller_id"
    const val EXTRA_CALLER_NAME = "caller_name"
    const val EXTRA_DEEP_LINK = "deep_link"
    const val EXTRA_CLICK_ACTION = "click_action"
    const val EXTRA_NOTIFICATION_KIND = "notification_kind"
}
