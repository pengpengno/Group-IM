package com.github.im.group

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.github.im.group.sdk.VoiceRecorderFactory
import com.github.im.group.sdk.initAndroidContext
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin


/**
 * 主程序入口
 */
class MainActivity : ComponentActivity() {
//    private val activityResultLauncher =
//        registerForActivityResult(
//            ActivityResultContracts.RequestMultiplePermissions())
//        { permissions ->
//            // Handle Permission granted/rejected
//            var permissionGranted = true
//            permissions.entries.forEach {
//                if (it.key in REQUIRED_PERMISSIONS && it.value == false)
//                    permissionGranted = false
//            }
//            if (!permissionGranted) {
//                Toast.makeText(baseContext,
//                    "Permission request denied",
//                    Toast.LENGTH_SHORT).show()
//            } else {
//                startCamera()
//            }
//        }
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        GlobalCredentialProvider.storage = AndroidCredentialStorage(applicationContext)
        VoiceRecorderFactory.context = applicationContext


        initAndroidContext(applicationContext)
        startKoin {
            androidLogger()
            androidContext(this@MainActivity)
            modules(appModule)
        }
        setContent {
            App()
        }
    }
}

