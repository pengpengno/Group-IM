package com.github.im.group.manager

import kotlinx.coroutines.flow.StateFlow

data class MessageNotificationPreferences(
    val enableNotifications: Boolean = true,
    val enableSound: Boolean = true,
    val enablePreview: Boolean = true
)

interface NotificationPreferenceStore {
    val preferences: StateFlow<MessageNotificationPreferences>

    fun getSnapshot(): MessageNotificationPreferences

    suspend fun updatePreferences(transform: (MessageNotificationPreferences) -> MessageNotificationPreferences)
}
