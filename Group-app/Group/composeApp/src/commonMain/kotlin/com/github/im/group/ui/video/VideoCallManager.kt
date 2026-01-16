package com.github.im.group.ui.video

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.github.im.group.model.UserInfo
import com.github.im.group.model.defaultUserInfo
import org.koin.compose.viewmodel.koinViewModel

/**
 * 视频通话管理器
 * 统一管理视频通话的生命周期和UI状态
 */
@Composable
fun VideoCallManager(
    onCallStarted: (() -> Unit)? = null,
    onCallEnded: (() -> Unit)? = null,
    onIncomingCall: ((UserInfo) -> Unit)? = null
) {
    val videoCallViewModel = koinViewModel<VideoCallViewModel>()
    val videoCallState by videoCallViewModel.videoCallState.collectAsState()
    
    // 监听通话状态变化
    LaunchedEffect(videoCallState.callStatus) {
        when (videoCallState.callStatus) {
            VideoCallStatus.ACTIVE -> {
                onCallStarted?.invoke()
            }
            VideoCallStatus.ENDED -> {
                onCallEnded?.invoke()
            }
            VideoCallStatus.INCOMING -> {
                videoCallState.caller?.let { caller ->
                    onIncomingCall?.invoke(caller)
                }
            }
            else -> {
                // 其他状态的处理
            }
        }
    }
    
    // 根据状态显示相应UI
    when {
        videoCallState.isMinimized -> {
            // 显示悬浮窗
            val localStream by videoCallViewModel.localMediaStream.collectAsState()
            val remoteVideoTrack by videoCallViewModel.remoteVideo.collectAsState()
            val remoteAudioTrack by videoCallViewModel.remoteAudio.collectAsState()
            
            VideoCallFloatingWindow(
                remoteUser = videoCallState.caller ?: defaultUserInfo(),
                localMediaStream = localStream,
                remoteVideoTrack = remoteVideoTrack,
                remoteAudioTrack = remoteAudioTrack,
                onExpand = { videoCallViewModel.maximizeCall() },
                onEndCall = { videoCallViewModel.endCall() },
                onToggleCamera = { videoCallViewModel.toggleCamera() },
                onToggleMicrophone = { videoCallViewModel.toggleMicrophone() }
            )
        }
        else -> {
            val localStream by videoCallViewModel.localMediaStream.collectAsState()
            val remoteVideoTrack by videoCallViewModel.remoteVideo.collectAsState()
            val remoteAudioTrack by videoCallViewModel.remoteAudio.collectAsState()
            
            // 显示正常通话界面
            VideoCallUI(
                remoteUser = (videoCallState.callee ?: videoCallState.caller) ?:defaultUserInfo(),
                localMediaStream = localStream,
                videoCallState = videoCallState,
                remoteVideoTrack = remoteVideoTrack,
                remoteAudioTrack = remoteAudioTrack,
                onEndCall = { videoCallViewModel.endCall() },
                onToggleCamera = { videoCallViewModel.toggleCamera() },
                onToggleMicrophone = { videoCallViewModel.toggleMicrophone() },
                onSwitchCamera = { videoCallViewModel.switchCamera() },
                onMinimizeCall = { videoCallViewModel.minimizeCall() },
                onToggleSpeaker = { videoCallViewModel.toggleSpeaker() }
            )
        }
    }
}