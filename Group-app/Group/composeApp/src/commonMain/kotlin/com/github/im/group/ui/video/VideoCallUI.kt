package com.github.im.group.ui.video

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Minimize
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.github.im.group.model.UserInfo
import com.github.im.group.sdk.TryGetVideoCallPermissions
import com.github.im.group.sdk.VideoScreenView
import com.github.im.group.ui.UserAvatar
import org.koin.compose.viewmodel.koinViewModel
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * 视频通话启动器组件 - 封装了完整的视频通话流程
 *
 * 此组件负责管理视频通话的整个生命周期，包括：
 * - 发起视频通话
 * - 显示来电通知
 * - 显示拨出通知
 * - 显示活跃通话界面
 * - 处理通话控制（摄像头、麦克风、扬声器等）
 *
 * @param remoteUser 通话的远程用户信息，不能为空
 * @param onCallEnded 通话结束时的回调函数，用于通知父组件关闭视频通话界面
 */
@Composable
fun VideoCallLauncher(remoteUser: UserInfo, onCallEnded: () -> Unit = {}) {
    val videoCallViewModel: VideoCallViewModel = koinViewModel()
    val videoCallState by videoCallViewModel.videoCallState.collectAsState()

    // 首先检查权限，只有权限被授予后才开始视频通话流程
    TryGetVideoCallPermissions(
            onAllGranted = {
                // 权限已授予，可以安全地启动视频通话
                // 启动视频通话
                videoCallViewModel.startCall(remoteUser)
            },
            onAnyDenied = {
                // 权限被拒绝，结束通话
                onCallEnded()
            }
    )

    // 检测到通话结束状态时，通知调用方关闭界面
    LaunchedEffect(videoCallState.callStatus) {
        if (videoCallState.callStatus == VideoCallStatus.ENDED) {
            // 等待状态重置后再通知界面关闭
            kotlinx.coroutines.delay(600) // 略长于ViewModel中的延迟时间
            onCallEnded()
        }
    }

    // 根据通话状态显示不同界面
    when (videoCallState.callStatus) {
        VideoCallStatus.IDLE -> {
            // 等待权限处理后启动视频通话
            // 实际启动在权限检查中进行
        }
        VideoCallStatus.INCOMING -> {
            // 显示来电通知
            VideoCallIncomingNotification(
                    caller = remoteUser,
                    onAccept = { videoCallViewModel.acceptCall() },
                    onReject = {
                        videoCallViewModel.endCall()
                        onCallEnded()
                    }
            )
        }
        VideoCallStatus.OUTGOING -> {
            // 显示拨出通知的同时，也显示基础控制界面
            OutgoingVideoCallUI(
                    remoteUser = remoteUser,
                    videoCallViewModel = videoCallViewModel,
                    onEndCall = {
                        videoCallViewModel.endCall()
                        onCallEnded()
                    }
            )
        }
        VideoCallStatus.CONNECTING -> {
            // 显示连接中界面的同时，也显示基础控制界面
            ConnectingVideoCallUI(
                    remoteUser = remoteUser,
                    videoCallViewModel = videoCallViewModel,
                    onEndCall = {
                        videoCallViewModel.endCall()
                        onCallEnded()
                    }
            )
        }
        VideoCallStatus.ACTIVE -> {
            // 显示活跃通话界面
            ActiveVideoCallUI(
                    remoteUser = remoteUser,
                    videoCallViewModel = videoCallViewModel,
                    onEndCall = {
                        videoCallViewModel.endCall()
                        onCallEnded()
                    }
            )
        }
        VideoCallStatus.ENDED, VideoCallStatus.ERROR -> {
            // 通话结束或错误，显示结束状态，稍后会自动重置
            // 通过LaunchedEffect处理状态重置
        }
        else -> {
            // 其他状态，什么都不做
        }
    }
}

/**
 * 拨出视频通话界面组件（正在呼叫对方）
 *
 * 在等待对方接听时显示此界面，包含：
 * - 呼叫状态提示
 * - 基础通话控制按钮
 * - 用户信息
 *
 * @param remoteUser 通话的远程用户信息
 * @param videoCallViewModel 视频通话的ViewModel，提供状态和控制方法
 * @param onEndCall 挂断通话时的回调函数
 */
@Composable
private fun OutgoingVideoCallUI(
        remoteUser: UserInfo,
        videoCallViewModel: VideoCallViewModel,
        onEndCall: () -> Unit
) {
    val videoCallState by videoCallViewModel.videoCallState.collectAsState()
    val localStream by videoCallViewModel.localMediaStream.collectAsState()

    LaunchedEffect(remoteUser) { videoCallViewModel.startCall(remoteUser) }

    Dialog(
            onDismissRequest = { /* 不允许通过点击外部关闭 */},
            properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
            // 背景内容
            Box(modifier = Modifier.fillMaxSize().background(Color(0xFF2D2D2D))) {
                // 显示本地视频小窗口（即使在呼叫阶段也可以预览自己）
                Box(
                        modifier =
                                Modifier.size(120.dp)
                                        .align(Alignment.TopEnd)
                                        .padding(16.dp)
                                        .background(
                                                Color(0xFF1E1E1E),
                                                shape =
                                                        androidx.compose.foundation.shape
                                                                .RoundedCornerShape(8.dp)
                                        )
                ) {
                    val localVideoTrack = localStream?.videoTracks?.firstOrNull()
                    val localAudioTrack = localStream?.audioTracks?.firstOrNull()

                    if (localVideoTrack != null && videoCallState.isLocalVideoEnabled) {
                        VideoScreenView(
                                modifier = Modifier.fillMaxSize(),
                                videoTrack = localVideoTrack,
                                audioTrack = localAudioTrack
                        )
                    } else {
                        // 显示本地用户占位符
                        Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = "本地视频",
                                    tint = Color.Gray
                            )
                        }
                    }
                }

                // 呼叫状态和用户信息
                Column(
                        modifier = Modifier.align(Alignment.Center).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    UserAvatar(username = remoteUser.username ?: "未知", size = 120)

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                            text = remoteUser.username ?: "未知用户",
                            color = Color.White,
                            fontSize = 28.sp,
                            style = MaterialTheme.typography.titleLarge
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(text = "正在呼叫...", color = Color.White.copy(alpha = 0.7f), fontSize = 16.sp)
                }
            }

            // 控制按钮区域
            CallControlPanel(
                    videoCallState = videoCallState,
                    videoCallViewModel = videoCallViewModel,
                    onEndCall = onEndCall,
                    modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

/**
 * 连接中视频通话界面组件（正在建立连接）
 *
 * 在通话已接通但仍在建立连接时显示此界面，包含：
 * - 连接状态提示
 * - 基础通话控制按钮
 * - 用户信息
 *
 * @param remoteUser 通话的远程用户信息
 * @param videoCallViewModel 视频通话的ViewModel，提供状态和控制方法
 * @param onEndCall 挂断通话时的回调函数
 */
@Composable
private fun ConnectingVideoCallUI(
        remoteUser: UserInfo,
        videoCallViewModel: VideoCallViewModel,
        onEndCall: () -> Unit
) {
    val videoCallState by videoCallViewModel.videoCallState.collectAsState()
    val localStream by videoCallViewModel.localMediaStream.collectAsState()

    Dialog(
            onDismissRequest = { /* 不允许通过点击外部关闭 */},
            properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
            // 背景内容
            Box(modifier = Modifier.fillMaxSize().background(Color(0xFF2D2D2D))) {
                // 显示本地视频小窗口
                Box(
                        modifier =
                                Modifier.size(120.dp)
                                        .align(Alignment.TopEnd)
                                        .padding(16.dp)
                                        .background(
                                                Color(0xFF1E1E1E),
                                                shape =
                                                        androidx.compose.foundation.shape
                                                                .RoundedCornerShape(8.dp)
                                        )
                ) {
                    val localVideoTrack = localStream?.videoTracks?.firstOrNull()
                    val localAudioTrack = localStream?.audioTracks?.firstOrNull()

                    if (localVideoTrack != null && videoCallState.isLocalVideoEnabled) {
                        VideoScreenView(
                                modifier = Modifier.fillMaxSize(),
                                videoTrack = localVideoTrack,
                                audioTrack = localAudioTrack
                        )
                    } else {
                        // 显示本地用户占位符
                        Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = "本地视频",
                                    tint = Color.Gray
                            )
                        }
                    }
                }

                // 连接状态和用户信息
                Column(
                        modifier = Modifier.align(Alignment.Center).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    UserAvatar(username = remoteUser.username ?: "未知", size = 120)

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                            text = remoteUser.username ?: "未知用户",
                            color = Color.White,
                            fontSize = 28.sp,
                            style = MaterialTheme.typography.titleLarge
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(text = "正在连接...", color = Color.White.copy(alpha = 0.7f), fontSize = 16.sp)

                    Spacer(modifier = Modifier.height(32.dp))

                    CircularProgressIndicator(color = Color.Yellow, strokeWidth = 4.dp)
                }
            }

            // 控制按钮区域
            CallControlPanel(
                    videoCallState = videoCallState,
                    videoCallViewModel = videoCallViewModel,
                    onEndCall = onEndCall,
                    modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

/**
 * 活跃视频通话界面组件
 *
 * 显示正在进行的视频通话界面，包含：
 * - 远程视频画面
 * - 本地视频小窗口
 * - 通话控制按钮
 * - 用户信息和通话时长
 *
 * @param remoteUser 通话的远程用户信息
 * @param videoCallViewModel 视频通话的ViewModel，提供状态和控制方法
 * @param onEndCall 挂断通话时的回调函数
 */
@OptIn(ExperimentalTime::class)
@Composable
private fun ActiveVideoCallUI(
        remoteUser: UserInfo,
        videoCallViewModel: VideoCallViewModel,
        onEndCall: () -> Unit
) {
    val videoCallState by videoCallViewModel.videoCallState.collectAsState()
    val remoteVideoTrack by videoCallViewModel.remoteVideo.collectAsState()
    val remoteAudioTrack by videoCallViewModel.remoteAudio.collectAsState()
    val localStream by videoCallViewModel.localMediaStream.collectAsState()

    Dialog(
            onDismissRequest = { /* 不允许通过点击外部关闭 */},
            properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
            // 远程视频显示区域
            Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
                // 显示远程视频流
                if (remoteVideoTrack != null && videoCallState.isRemoteVideoEnabled) {
                    VideoScreenView(
                            modifier = Modifier.fillMaxSize(),
                            videoTrack = remoteVideoTrack,
                            audioTrack = remoteAudioTrack
                    )
                } else {
                    // 显示占位符或用户信息
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        UserAvatar(username = remoteUser.username ?: "未知", size = 160)
                    }
                }

                // 顶部和底部渐变遮盖层，确保文字清晰可见
                Box(
                        modifier =
                                Modifier.fillMaxWidth()
                                        .height(180.dp)
                                        .align(Alignment.TopCenter)
                                        .background(
                                                Brush.verticalGradient(
                                                        colors =
                                                                listOf(
                                                                        Color.Black.copy(
                                                                                alpha = 0.7f
                                                                        ),
                                                                        Color.Transparent
                                                                )
                                                )
                                        )
                )
                Box(
                        modifier =
                                Modifier.fillMaxWidth()
                                        .height(240.dp)
                                        .align(Alignment.BottomCenter)
                                        .background(
                                                Brush.verticalGradient(
                                                        colors =
                                                                listOf(
                                                                        Color.Transparent,
                                                                        Color.Black.copy(
                                                                                alpha = 0.8f
                                                                        )
                                                                )
                                                )
                                        )
                )

                // 本地视频小窗口
                Box(
                        modifier =
                                Modifier.size(120.dp)
                                        .align(Alignment.TopEnd)
                                        .padding(16.dp)
                                        .background(
                                                Color(0xFF1E1E1E),
                                                shape =
                                                        androidx.compose.foundation.shape
                                                                .RoundedCornerShape(8.dp)
                                        )
                ) {
                    val localVideoTrack = localStream?.videoTracks?.firstOrNull()
                    val localAudioTrack = localStream?.audioTracks?.firstOrNull()

                    if (localVideoTrack != null && videoCallState.isLocalVideoEnabled) {
                        VideoScreenView(
                                modifier = Modifier.fillMaxSize(),
                                videoTrack = localVideoTrack,
                                audioTrack = localAudioTrack
                        )
                    } else {
                        // 显示本地用户占位符
                        Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = "本地视频",
                                    tint = Color.Gray
                            )
                        }
                    }
                }

                // 用户名和状态显示
                Column(modifier = Modifier.align(Alignment.TopStart).padding(16.dp)) {
                    Text(
                            text = remoteUser.username ?: "未知用户",
                            color = Color.White,
                            fontSize = 18.sp,
                            style = MaterialTheme.typography.titleLarge
                    )

                    // 显示通话时长
                    videoCallState.callStartTime?.let { startTime ->
                        val duration by remember {
                            //TODO  epochMilliseconds
                            mutableStateOf((Clock.System.now().toEpochMilliseconds() - startTime) / 1000)
                        }
                        Text(text = formatDuration(duration), color = Color.White, fontSize = 14.sp)
                    }
                }

                // 网络状态指示器
                if (videoCallState.errorMessage != null) {
                    Box(
                            modifier =
                                    Modifier.align(Alignment.TopCenter)
                                            .padding(top = 16.dp)
                                            .background(
                                                    Color.Red,
                                                    shape =
                                                            androidx.compose.foundation.shape
                                                                    .RoundedCornerShape(4.dp)
                                            )
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) { Text(text = "网络问题", color = Color.White, fontSize = 12.sp) }
                }
            }

            // 控制按钮区域
            CallControlPanel(
                    videoCallState = videoCallState,
                    videoCallViewModel = videoCallViewModel,
                    onEndCall = onEndCall,
                    modifier = Modifier.align(Alignment.BottomCenter)
            )

            // 最小化按钮（右上角）
            IconButton(
                    onClick = {
                        videoCallViewModel.minimizeCall()
                        onEndCall()
                    },
                    modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)
            ) {
                Icon(
                        imageVector = Icons.Default.Minimize,
                        contentDescription = "最小化",
                        tint = Color.White
                )
            }
        }
    }
}

/**
 * 格式化通话时长
 *
 * 将秒数格式化为 MM:SS 格式的时间字符串
 *
 * @param seconds 通话时长（秒）
 * @return 格式化的时长字符串，如 "05:23"
 */
private fun formatDuration(seconds: Long): String {
    val h = seconds / 3600
    val m = (seconds % 3600) /60
    val s = seconds % 60

    val mStr = m.toString().padStart(2, '0')
    val sStr = s.toString().padStart(2, '0')

    return if (h > 0) {
        "$h:$mStr:$sStr"
    } else {
        "$mStr:$sStr"
    }
}
/** 统一的视频通话底部控制面板 */
@Composable
private fun CallControlPanel(
        videoCallState: VideoCallState,
        videoCallViewModel: VideoCallViewModel,
        onEndCall: () -> Unit,
        modifier: Modifier = Modifier
) {
    Row(
            modifier =
                    modifier.padding(horizontal = 24.dp, vertical = 32.dp)
                            .fillMaxWidth()
                            .background(
                                    color = Color(0x66000000),
                                    shape =
                                            androidx.compose.foundation.shape.RoundedCornerShape(
                                                    36.dp
                                            )
                            )
                            .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
    ) {
        // Switch Camera
        IconButton(
                onClick = { videoCallViewModel.switchCamera() },
                modifier = Modifier.size(48.dp)
        ) {
            Icon(
                    imageVector = Icons.Default.Cameraswitch,
                    contentDescription = "切换摄像头",
                    tint = Color.White
            )
        }

        // Microphone
        FloatingActionButton(
                onClick = { videoCallViewModel.toggleMicrophone() },
                containerColor =
                        if (videoCallState.isMicrophoneEnabled) Color(0xFF4A4A4A) else Color.White,
                contentColor = if (videoCallState.isMicrophoneEnabled) Color.White else Color.Black,
                elevation = FloatingActionButtonDefaults.elevation(0.dp),
                modifier = Modifier.size(56.dp),
                shape = androidx.compose.foundation.shape.CircleShape
        ) {
            Icon(
                    imageVector =
                            if (videoCallState.isMicrophoneEnabled) Icons.Default.Mic
                            else Icons.Default.MicOff,
                    contentDescription = "麦克风"
            )
        }

        // End Call
        FloatingActionButton(
                onClick = onEndCall,
                containerColor = Color(0xFFFF3B30),
                contentColor = Color.White,
                elevation = FloatingActionButtonDefaults.elevation(4.dp),
                modifier = Modifier.size(64.dp),
                shape = androidx.compose.foundation.shape.CircleShape
        ) {
            Icon(
                    imageVector = Icons.Default.CallEnd,
                    contentDescription = "挂断",
                    modifier = Modifier.size(32.dp)
            )
        }

        // Camera
        FloatingActionButton(
                onClick = { videoCallViewModel.toggleCamera() },
                containerColor =
                        if (videoCallState.isLocalVideoEnabled) Color(0xFF4A4A4A) else Color.White,
                contentColor = if (videoCallState.isLocalVideoEnabled) Color.White else Color.Black,
                elevation = FloatingActionButtonDefaults.elevation(0.dp),
                modifier = Modifier.size(56.dp),
                shape = androidx.compose.foundation.shape.CircleShape
        ) {
            Icon(
                    imageVector =
                            if (videoCallState.isLocalVideoEnabled) Icons.Default.Videocam
                            else Icons.Default.VideocamOff,
                    contentDescription = "摄像头"
            )
        }

        // Speaker
        IconButton(
                onClick = { videoCallViewModel.toggleSpeaker() },
                modifier = Modifier.size(48.dp)
        ) {
            Icon(
                    imageVector =
                            if (videoCallState.isSpeakerEnabled) Icons.Default.VolumeUp
                            else Icons.Default.VolumeOff,
                    contentDescription = "扬声器",
                    tint = Color.White
            )
        }
    }
}
