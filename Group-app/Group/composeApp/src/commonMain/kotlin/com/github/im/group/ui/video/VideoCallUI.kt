package com.github.im.group.ui.video

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Minimize
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
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
import androidx.compose.ui.draw.clip
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
        VideoCallStatus.IDLE -> {}
        VideoCallStatus.INCOMING -> {
            VideoCallIncomingNotification(
                caller = videoCallState.caller ?: remoteUser,
                onAccept = { videoCallViewModel.acceptCall() },
                onReject = {
                    videoCallViewModel.endCall()
                    onCallEnded()
                }
            )
        }
        VideoCallStatus.OUTGOING, VideoCallStatus.CONNECTING -> {
            OutgoingVideoCallUI(
                remoteUser = videoCallState.caller ?: remoteUser,
                videoCallViewModel = videoCallViewModel,
                onEndCall = {
                    videoCallViewModel.endCall()
                    onCallEnded()
                }
            )
        }
        VideoCallStatus.ACTIVE -> {
            ActiveVideoCallUI(
                remoteUser = videoCallState.caller ?: remoteUser,
                videoCallViewModel = videoCallViewModel,
                onEndCall = {
                    videoCallViewModel.endCall()
                    onCallEnded()
                }
            )
        }
        else -> {}
    }
}

/**
 * 多人会议启动器组件
 */
@Composable
fun MeetingLauncher(roomId: String, participantIds: List<String>, onCallEnded: () -> Unit = {}) {
    val videoCallViewModel: VideoCallViewModel = koinViewModel()
    val videoCallState by videoCallViewModel.videoCallState.collectAsState()

    TryGetVideoCallPermissions(
        onAllGranted = {
            videoCallViewModel.startMeeting(roomId, participantIds)
        },
        onAnyDenied = { onCallEnded() }
    )

    LaunchedEffect(videoCallState.callStatus) {
        if (videoCallState.callStatus == VideoCallStatus.ENDED) {
            kotlinx.coroutines.delay(600)
            onCallEnded()
        }
    }

    when (videoCallState.callStatus) {
        VideoCallStatus.IDLE -> {}
        VideoCallStatus.INCOMING -> {
            VideoCallIncomingNotification(
                caller = videoCallState.caller ?: UserInfo(0, "Meeting", ""),
                onAccept = { videoCallViewModel.acceptCall() },
                onReject = {
                    videoCallViewModel.endCall()
                    onCallEnded()
                }
            )
        }
        VideoCallStatus.OUTGOING, VideoCallStatus.CONNECTING, VideoCallStatus.ACTIVE -> {
            ActiveVideoCallUI(
                remoteUser = videoCallState.caller ?: UserInfo(0, "Group", ""),
                videoCallViewModel = videoCallViewModel,
                onEndCall = {
                    videoCallViewModel.endCall()
                    onCallEnded()
                }
            )
        }
        else -> {}
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
    val remoteVideoTracks by videoCallViewModel.remoteVideoTracks.collectAsState()
    val remoteAudioTracks by videoCallViewModel.remoteAudioTracks.collectAsState()
    val localStream by videoCallViewModel.localMediaStream.collectAsState()

    // 如果处于最小化状态，不显示全屏对话框
    if (videoCallState.isMinimized) {
        return
    }

    Dialog(
            onDismissRequest = { /* 不允许通过点击外部关闭 */},
            properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
            // 远程视频显示区域 - 多人网格布局
            Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
                if (remoteVideoTracks.size <= 1) {
                    // 一对一视频或尚未有人加入
                    val remoteTrackEntry = remoteVideoTracks.entries.firstOrNull()
                    val remoteTrack = remoteTrackEntry?.value
                    val remoteUserId = remoteTrackEntry?.key
                    val remoteAudio = remoteAudioTracks[remoteUserId ?: ""]

                    if (remoteTrack != null && videoCallState.isRemoteVideoEnabled) {
                        VideoScreenView(
                                modifier = Modifier.fillMaxSize(),
                                videoTrack = remoteTrack,
                                audioTrack = remoteAudio
                        )
                    } else {
                        // 显示占位符或用户信息
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            UserAvatar(username = remoteUser.username ?: "未知", size = 160)
                        }
                    }
                } else {
                    // 多人视频网格布局
                    MultiPartyVideoGrid(
                        videoTracks = remoteVideoTracks,
                        audioTracks = remoteAudioTracks,
                        participants = videoCallState.participants
                    )
                }

                // 顶部和底部渐变遮盖层
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
                ) {
                    // 顶部控制栏 (如最小化按钮)
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        IconButton(onClick = { videoCallViewModel.minimizeCall() }) {
                            Icon(Icons.Default.Minimize, contentDescription = "最小化", tint = Color.White)
                        }
                    }
                }
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
                            if (videoCallState.isSpeakerEnabled) Icons.AutoMirrored.Filled.VolumeUp
                            else Icons.AutoMirrored.Filled.VolumeOff,
                    contentDescription = "扬声器",
                    tint = Color.White
            )
        }
    }
}

/**
 * 多人视频网格布局组件
 * 根据参与者数量动态调整布局，支持圆角、静音状态指示和美观的缺省占位图。
 */
@Composable
fun MultiPartyVideoGrid(
    videoTracks: Map<String, com.github.im.group.sdk.VideoTrack>,
    audioTracks: Map<String, com.github.im.group.sdk.AudioTrack>,
    participants: List<UserInfo>,
    modifier: Modifier = Modifier
) {
    if (participants.isEmpty()) return

    // 如果少于等于 2 人，采用单列；否则采用双列网格
    val columns = if (participants.size <= 2) 1 else 2

    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        modifier = modifier.fillMaxSize().padding(top = 48.dp, bottom = 120.dp, start = 16.dp, end = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(participants, key = { it }) { participant ->
            val participantId = participant.userId.toString()
            val videoTrack = videoTracks[participantId]
            val audioTrack = audioTracks[participantId]

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(if (columns == 1) 4f / 3f else 1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF282828))
            ) {
                // 渲染视频流
                if (videoTrack != null && videoTrack.isEnabled) {
                    VideoScreenView(
                        modifier = Modifier.fillMaxSize(),
                        videoTrack = videoTrack,
                        audioTrack = audioTrack
                    )
                } else {
                    // 无视频时的占位 UI
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Participant $participantId",
                                modifier = Modifier.size(64.dp),
                                tint = Color.Gray.copy(alpha = 0.5f)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "用户 $participantId",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 14.sp
                            )
                        }
                    }
                }

                // 用户唯一标识或名字底条
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(8.dp)
                        .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "用户 $participantId",
                        color = Color.White,
                        fontSize = 12.sp
                    )
                }

                // 麦克风静音状态指示灯
                if (audioTrack == null || !audioTrack.isEnabled) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                            .padding(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MicOff,
                            contentDescription = "Muted",
                            tint = Color(0xFFFF3B30),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}
