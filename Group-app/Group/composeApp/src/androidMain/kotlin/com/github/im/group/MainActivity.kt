package com.github.im.group

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.github.im.group.config.ConfigManager
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

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        initAndroidContext(applicationContext)

        GlobalCredentialProvider.storage = AndroidCredentialStorage(applicationContext)

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
        }
        
        setContent {
            KoinContext {
                App()
            }
        }
    }
}
