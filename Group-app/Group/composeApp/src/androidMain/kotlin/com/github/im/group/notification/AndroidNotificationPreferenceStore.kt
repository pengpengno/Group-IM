package com.github.im.group.notification

import android.content.Context
import androidx.core.content.edit
import com.github.im.group.manager.MessageNotificationPreferences
import com.github.im.group.manager.NotificationPreferenceStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

class AndroidNotificationPreferenceStore(
    context: Context
) : NotificationPreferenceStore {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val _preferences = MutableStateFlow(loadPreferences())
    override val preferences: StateFlow<MessageNotificationPreferences> = _preferences.asStateFlow()

    override fun getSnapshot(): MessageNotificationPreferences = _preferences.value

    override suspend fun updatePreferences(
        transform: (MessageNotificationPreferences) -> MessageNotificationPreferences
    ) {
        withContext(Dispatchers.IO) {
            val updated = transform(_preferences.value)
            prefs.edit {
                putBoolean(KEY_ENABLE_NOTIFICATIONS, updated.enableNotifications)
                putBoolean(KEY_ENABLE_SOUND, updated.enableSound)
                putBoolean(KEY_ENABLE_PREVIEW, updated.enablePreview)
            }
            _preferences.value = updated
        }
    }

    private fun loadPreferences(): MessageNotificationPreferences {
        return MessageNotificationPreferences(
            enableNotifications = prefs.getBoolean(KEY_ENABLE_NOTIFICATIONS, true),
            enableSound = prefs.getBoolean(KEY_ENABLE_SOUND, true),
            enablePreview = prefs.getBoolean(KEY_ENABLE_PREVIEW, true)
        )
    }

    companion object {
        private const val PREFS_NAME = "notification_preferences"
        private const val KEY_ENABLE_NOTIFICATIONS = "enable_notifications"
        private const val KEY_ENABLE_SOUND = "enable_sound"
        private const val KEY_ENABLE_PREVIEW = "enable_preview"
    }
}
