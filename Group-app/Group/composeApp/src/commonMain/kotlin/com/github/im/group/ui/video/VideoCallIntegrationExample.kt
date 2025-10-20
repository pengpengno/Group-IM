package com.github.im.group.ui.video

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import com.github.im.group.model.UserInfo

/**
 * 视频通话集成示例
 * 展示如何在应用中集成全屏视频通话和小窗播放功能
 */
@Composable
fun VideoCallIntegrationExample(
    navController: NavHostController,
    videoCallViewModel: VideoCallViewModel
) {
    val videoCallState by videoCallViewModel.videoCallState.collectAsState()
    val localMediaStream by videoCallViewModel.localMediaStream
    
    // 示例远程用户
    val remoteUser = UserInfo(
        userId = 123456,
        username = "Test User",
        email = "test@example.com"
    )
    
    Box(modifier = Modifier.fillMaxSize()) {
        when (videoCallState.callStatus) {
            CallStatus.ACTIVE -> {
                // 全屏视频通话界面
                VideoCallUI(
                    navHostController = navController,
                    remoteUser = videoCallState.remoteUser ?: remoteUser,
                    localMediaStream = localMediaStream,
                    onEndCall = { 
                        videoCallViewModel.endCall()
                    },
                    onMinimizeCall = {
                        videoCallViewModel.minimizeCall()
                    },
                    onToggleCamera = {
                        videoCallViewModel.toggleCamera()
                    },
                    onToggleMicrophone = {
                        videoCallViewModel.toggleMicrophone()
                    },
                    onSwitchCamera = {
                        videoCallViewModel.switchCamera()
                    }
                )
            }
            
            CallStatus.MINIMIZED -> {
                // 视频通话已最小化，可以在其他界面显示小窗
                // 这里只是一个示例，实际使用时需要将状态传递给主界面
            }
            
            else -> {
                // 其他状态不显示视频通话界面
            }
        }
    }
}