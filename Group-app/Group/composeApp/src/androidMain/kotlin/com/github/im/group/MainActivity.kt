package com.github.im.group

import android.os.Bundle
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.github.im.group.config.ConfigManager
import com.github.im.group.model.UserInfo
import com.github.im.group.notification.AndroidCallNotificationContract
import com.github.im.group.notification.AndroidNotificationPermissionHelper
import com.github.im.group.notification.AndroidPushEndpointRegistrar
import com.github.im.group.notification.AndroidPushNotificationManager
import com.google.firebase.messaging.FirebaseMessaging
import com.github.im.group.ui.video.CallNotificationAction
import com.github.im.group.ui.video.CallNotificationCenter
import com.github.im.group.ui.video.CallNotificationEvent
import io.github.aakira.napier.Napier
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.compose.KoinContext
import org.koin.core.context.startKoin

/**
 * 主程序入口
 */
class MainActivity : ComponentActivity() {

    private lateinit var notificationPermissionHelper: AndroidNotificationPermissionHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        initAndroidContext(applicationContext)

        GlobalCredentialProvider.storage = AndroidCredentialStorage(applicationContext)
        notificationPermissionHelper = AndroidNotificationPermissionHelper(this)
        AndroidPushNotificationManager(applicationContext).ensureChannels()

        val koinResult = startKoin {
            androidLogger()
            androidContext(this@MainActivity)
            modules(appmodule, commonModule)
        }
        val koin = koinResult.koin

        // 初始化配置管理器
        val configManager: ConfigManager = koin.get()
        MainScope().launch {
            configManager.initialize()
            // 监听配置变化并更新 ProxyConfig (向下兼容)
            if (GlobalCredentialProvider.storage.getUserInfo() != null) {
                notificationPermissionHelper.ensureNotificationPermission()
            }
        }

        val endpointRegistrar = AndroidPushEndpointRegistrar(applicationContext)
        endpointRegistrar.syncStoredTokenIfAvailable()
        FirebaseMessaging.getInstance().token
            .addOnSuccessListener { token ->
                if (token.isNullOrBlank()) {
                    return@addOnSuccessListener
                }
                endpointRegistrar.syncTokenAsync(token)
            }
            .addOnFailureListener { error ->
                Napier.e("Failed to fetch FCM token on startup", error)
            }

        handleCallNotificationIntent(intent)
        
        setContent {
            KoinContext {
                App()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleCallNotificationIntent(intent)
    }

    private fun handleCallNotificationIntent(intent: Intent?) {
        if (intent == null) {
            return
        }

        val roomId = intent.getStringExtra(AndroidCallNotificationContract.EXTRA_ROOM_ID)
        val callerId = intent.getStringExtra(AndroidCallNotificationContract.EXTRA_CALLER_ID)
        val callerName = intent.getStringExtra(AndroidCallNotificationContract.EXTRA_CALLER_NAME)
        val deepLink = intent.getStringExtra(AndroidCallNotificationContract.EXTRA_DEEP_LINK)
        val clickAction = intent.getStringExtra(AndroidCallNotificationContract.EXTRA_CLICK_ACTION)

        if (roomId.isNullOrBlank() || callerId.isNullOrBlank()) {
            return
        }

        val action = when (intent.action) {
            AndroidCallNotificationContract.ACTION_ACCEPT_CALL -> CallNotificationAction.ACCEPT
            AndroidCallNotificationContract.ACTION_REJECT_CALL -> CallNotificationAction.REJECT
            Intent.ACTION_VIEW -> if (clickAction.equals("open_meeting", ignoreCase = true)) {
                CallNotificationAction.OPEN
            } else {
                return
            }
            else -> CallNotificationAction.OPEN
        }

        CallNotificationCenter.publish(
            CallNotificationEvent(
                action = action,
                roomId = roomId,
                caller = UserInfo(
                    userId = callerId.toLongOrNull() ?: 0L,
                    username = callerName ?: "Caller",
                    email = ""
                ),
                deepLink = deepLink
            )
        )

        intent.action = null
        intent.removeExtra(AndroidCallNotificationContract.EXTRA_ROOM_ID)
        intent.removeExtra(AndroidCallNotificationContract.EXTRA_CALLER_ID)
        intent.removeExtra(AndroidCallNotificationContract.EXTRA_CALLER_NAME)
        intent.removeExtra(AndroidCallNotificationContract.EXTRA_DEEP_LINK)
        intent.removeExtra(AndroidCallNotificationContract.EXTRA_CLICK_ACTION)
        intent.removeExtra(AndroidCallNotificationContract.EXTRA_NOTIFICATION_KIND)
    }
}
