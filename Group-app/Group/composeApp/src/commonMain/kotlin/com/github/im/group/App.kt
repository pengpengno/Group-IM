package com.github.im.group

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.github.im.group.sdk.VideoPlayerManager
import com.github.im.group.ui.AppStartScreen
import com.github.im.group.ui.video.VideoCallLauncher
import com.github.im.group.ui.video.VideoCallViewModel
import com.github.im.group.ui.video.VideoCallStatus
import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun App() {
    Napier.base(DebugAntilog())
    
    // 渲染视频播放器对话框
    VideoPlayerManager.Render()
    
    // 渲染全局视频通话界面 (支持来电、拨出和最小化悬浮窗)
    GlobalVideoCallOverlay()
    
    AppStartScreen()
}

@Composable
fun GlobalVideoCallOverlay() {
    val videoCallViewModel: VideoCallViewModel = koinViewModel()
    val videoCallState by videoCallViewModel.videoCallState.collectAsState()
    val remoteUser = videoCallState.callee ?: videoCallState.caller 
    
    if (videoCallState.callStatus != VideoCallStatus.IDLE && remoteUser != null) {
        VideoCallLauncher(remoteUser)
    }
}