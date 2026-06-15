package com.github.im.group.ui.video

import com.github.im.group.model.UserInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

enum class CallNotificationAction {
    OPEN,
    ACCEPT,
    REJECT
}

data class CallNotificationEvent(
    val action: CallNotificationAction,
    val roomId: String,
    val caller: UserInfo,
    val deepLink: String? = null
)

object CallNotificationCenter {
    private val _event = MutableStateFlow<CallNotificationEvent?>(null)
    val event: StateFlow<CallNotificationEvent?> = _event

    fun publish(event: CallNotificationEvent) {
        _event.value = event
    }

    fun clear() {
        _event.value = null
    }
}
