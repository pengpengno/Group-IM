package com.github.im.group

import androidx.compose.runtime.Composable
import com.github.im.group.sdk.VideoPlayerManager
import com.github.im.group.ui.AppStartScreen
import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier

@Composable
fun App() {
    Napier.base(DebugAntilog())
    
    // 渲染视频播放器对话框
    VideoPlayerManager.Render()
    AppStartScreen()
}