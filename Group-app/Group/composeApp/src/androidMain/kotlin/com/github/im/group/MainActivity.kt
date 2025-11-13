package com.github.im.group

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.github.im.group.sdk.VoiceRecorderFactory
import com.github.im.group.sdk.initAndroidContext
import com.shepeliev.webrtckmp.WebRtc
import kotlinx.coroutines.CoroutineExceptionHandler
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.compose.KoinContext
import org.koin.core.context.startKoin
import org.webrtc.Logging


/**
 * 主程序入口
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        GlobalCredentialProvider.storage = AndroidCredentialStorage(applicationContext)
        VoiceRecorderFactory.context = applicationContext

        initAndroidContext(applicationContext)

        val globalHandler = CoroutineExceptionHandler { _, throwable ->
            if (throwable is IllegalStateException && throwable.message == "用户未登录") {
                Handler(Looper.getMainLooper()).post {
                    Toast.makeText(this, "请先登录", Toast.LENGTH_SHORT).show()
                    // 跳转登录
                }
            } else {
                Log.e("GlobalException", throwable.message, throwable)
            }
        }
        startKoin {
            androidLogger()
            androidContext(this@MainActivity)
            modules(commonModule,appModule, )

        }
        
        setContent {
            KoinContext {
                App()
            }
        }
    }
}