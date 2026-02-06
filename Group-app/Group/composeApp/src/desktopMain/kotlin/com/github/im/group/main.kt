package com.github.im.group

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.github.im.group.db.AppDatabase
import com.github.im.group.db.DatabaseDriverFactory
import com.github.im.group.db.DesktopDatabaseDriverFactory
import com.github.im.group.sdk.DesktopFilePicker
import com.github.im.group.sdk.DesktopVoiceRecorder
import com.github.im.group.sdk.FilePicker
import com.github.im.group.sdk.VoiceRecorder
import org.koin.core.context.startKoin
import org.koin.dsl.module

fun main() = application {
    startKoin {
        modules(mergedDesktopModule)
    }

    Window(
        onCloseRequest = ::exitApplication,
        title = "Group",
        undecorated = true
    ) {

        MaterialTheme { // ← 这里全局包裹 MaterialTheme
            App()
        }
    }
}

/**
 * 桌面端特定模块
 * 包含桌面平台特有的实现
 */
val desktopModule = module {
    // 桌面端特定的数据库工厂
    single<DatabaseDriverFactory> { DesktopDatabaseDriverFactory() }
    single<AppDatabase> { DesktopDatabaseDriverFactory().createDatabase() }

    // 桌面端特定的文件选择器
    single<FilePicker> { DesktopFilePicker() }
    
    // 桌面端特定的音频录制
    single<VoiceRecorder> { DesktopVoiceRecorder() }
}

/**
 * 合并后的桌面端模块
 * 按照依赖层次合并各个模块
 */
val mergedDesktopModule = module {
    // 底层：数据访问层
    includes(dataModule)        // 通用数据模块
    includes(desktopModule)     // 桌面端平台实现
    
    // 中层：业务能力层
    includes(sdkModule)         // SDK能力模块
    includes(jvmModule)         // JVM平台网络模块
    includes(managerModule)     // 业务管理层模块
    
    // 上层：UI业务层
    includes(viewModelModule)   // ViewModel模块
}