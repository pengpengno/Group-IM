package com.github.im.group

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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

        startKoin {
            androidLogger()
            androidContext(this@MainActivity)
            modules(appmodule,commonModule )

        }
        
        setContent {
            KoinContext {
                App()
            }
        }
    }
}