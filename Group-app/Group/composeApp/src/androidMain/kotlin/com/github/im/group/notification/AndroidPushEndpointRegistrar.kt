package com.github.im.group.notification

import android.content.Context
import com.github.im.group.AndroidCredentialStorage
import com.github.im.group.GlobalCredentialProvider
import com.github.im.group.api.PushApi
import com.github.im.group.api.PushEndpointUpsertRequest
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.UUID

class AndroidPushEndpointRegistrar(
    private val context: Context
) {

    companion object {
        private const val PREFS_NAME = "push_endpoint_registry"
        private const val KEY_ENDPOINT_ID = "android_endpoint_id"
        private const val KEY_DEVICE_ID = "android_device_id"
        private const val KEY_LAST_FCM_TOKEN = "android_last_fcm_token"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun syncTokenAsync(token: String) {
        if (token.isBlank()) {
            return
        }

        prefs.edit().putString(KEY_LAST_FCM_TOKEN, token).apply()
        scope.launch {
            registerTokenInternal(token)
        }
    }

    fun syncStoredTokenIfAvailable() {
        val token = prefs.getString(KEY_LAST_FCM_TOKEN, null) ?: return
        syncTokenAsync(token)
    }

    private suspend fun registerTokenInternal(token: String) {
        try {
            val storage = AndroidCredentialStorage(context)
            GlobalCredentialProvider.storage = storage
            val userInfo = storage.getUserInfo()

            if (userInfo == null || userInfo.userId <= 0L || userInfo.token.isBlank()) {
                Napier.i("Skip push endpoint sync because user session is unavailable.")
                return
            }

            GlobalCredentialProvider.currentToken = userInfo.token
            GlobalCredentialProvider.currentUserId = userInfo.userId
            GlobalCredentialProvider.companyId = userInfo.currentLoginCompanyId

            val endpointResponse = PushApi.upsertEndpoint(
                PushEndpointUpsertRequest(
                    endpointId = getOrCreateStableId(KEY_ENDPOINT_ID),
                    platform = "ANDROID",
                    provider = "FCM",
                    deviceId = getOrCreateStableId(KEY_DEVICE_ID),
                    token = token,
                    locale = Locale.getDefault().toLanguageTag(),
                    appVersion = appVersionName(),
                    enabled = true
                )
            )

            prefs.edit()
                .putString(KEY_ENDPOINT_ID, endpointResponse.endpointId)
                .putString(KEY_LAST_FCM_TOKEN, token)
                .apply()

            Napier.i(
                "Push endpoint synced successfully. endpointId=${endpointResponse.endpointId}, userId=${userInfo.userId}"
            )
        } catch (error: Exception) {
            Napier.e("Failed to sync Android push endpoint", error)
        }
    }

    private fun getOrCreateStableId(key: String): String {
        val existing = prefs.getString(key, null)
        if (!existing.isNullOrBlank()) {
            return existing
        }

        val value = UUID.randomUUID().toString()
        prefs.edit().putString(key, value).apply()
        return value
    }

    private fun appVersionName(): String {
        return runCatching {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: "android"
        }.getOrDefault("android")
    }
}
