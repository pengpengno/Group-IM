package com.github.im.group.ui.video

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.github.im.group.model.UserInfo
import com.github.im.group.sdk.MediaStream
import com.github.im.group.sdk.RemoteMediaStream
import com.github.im.group.sdk.VideoScreenView
import org.koin.compose.viewmodel.koinViewModel

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
fun VideoCallLauncher(
    remoteUser: UserInfo,
    onCallEnded: () -> Unit = {}
) {
    val videoCallViewModel: VideoCallViewModel = koinViewModel()
    val videoCallState by videoCallViewModel.videoCallState.collectAsState()
    
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
            // 启动视频通话
            LaunchedEffect(remoteUser) {
                videoCallViewModel.startCall(remoteUser)
            }
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
    
    Dialog(
        onDismissRequest = { /* 不允许通过点击外部关闭 */ },
        properties = DialogProperties(
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            // 背景内容
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF2D2D2D))
            ) {
                // 显示本地视频小窗口（即使在呼叫阶段也可以预览自己）
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                        .background(Color(0xFF1E1E1E), shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
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
                    modifier = Modifier
                        .align(Alignment.Center)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "正在呼叫",
                        color = Color.White,
                        fontSize = 24.sp
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = remoteUser.username ?: "未知用户",
                        color = Color.White,
                        fontSize = 20.sp
                    )
                    
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    CircularProgressIndicator(
                        color = Color.Cyan,
                        strokeWidth = 4.dp
                    )
                }
            }
            
            // 控制按钮区域 - 与ActiveVideoCallUI一致的控制
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 32.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // 切换摄像头
                FloatingActionButton(
                    onClick = { videoCallViewModel.switchCamera() },
                    containerColor = Color(0xFF666666),
                    modifier = Modifier.size(56.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Cameraswitch,
                        contentDescription = "切换摄像头",
                        tint = Color.White
                    )
                }
                
                // 麦克风控制
                FloatingActionButton(
                    onClick = { videoCallViewModel.toggleMicrophone() },
                    containerColor = if (videoCallState.isMicrophoneEnabled) Color(0xFF666666) else Color.Red,
                    modifier = Modifier.size(56.dp)
                ) {
                    Icon(
                        imageVector = if (videoCallState.isMicrophoneEnabled) Icons.Default.Mic else Icons.Default.MicOff,
                        contentDescription = if (videoCallState.isMicrophoneEnabled) "关闭麦克风" else "开启麦克风",
                        tint = Color.White
                    )
                }
                
                // 挂断电话
                FloatingActionButton(
                    onClick = onEndCall,
                    containerColor = Color.Red,
                    modifier = Modifier.size(56.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CallEnd,
                        contentDescription = "挂断",
                        tint = Color.White
                    )
                }

                // 摄像头控制
                FloatingActionButton(
                    onClick = { videoCallViewModel.toggleCamera() },
                    containerColor = if (videoCallState.isLocalVideoEnabled) Color(0xFF666666) else Color.Red,
                    modifier = Modifier.size(56.dp)
                ) {
                    Icon(
                        imageVector = if (videoCallState.isLocalVideoEnabled) Icons.Default.Videocam else Icons.Default.VideocamOff,
                        contentDescription = if (videoCallState.isLocalVideoEnabled) "关闭摄像头" else "开启摄像头",
                        tint = Color.White
                    )
                }
                
                // 扬声器控制
                FloatingActionButton(
                    onClick = { videoCallViewModel.toggleSpeaker() },
                    containerColor = if (videoCallState.isSpeakerEnabled) Color(0xFF666666) else Color.Red,
                    modifier = Modifier.size(56.dp)
                ) {
                    Icon(
                        imageVector = if (videoCallState.isSpeakerEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                        contentDescription = if (videoCallState.isSpeakerEnabled) "关闭扬声器" else "开启扬声器",
                        tint = Color.White
                    )
                }
            }
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
        onDismissRequest = { /* 不允许通过点击外部关闭 */ },
        properties = DialogProperties(
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            // 背景内容
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF2D2D2D))
            ) {
                // 显示本地视频小窗口
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                        .background(Color(0xFF1E1E1E), shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
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
                    modifier = Modifier
                        .align(Alignment.Center)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "正在连接",
                        color = Color.White,
                        fontSize = 24.sp
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = remoteUser.username ?: "未知用户",
                        color = Color.White,
                        fontSize = 20.sp
                    )
                    
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    CircularProgressIndicator(
                        color = Color.Yellow,
                        strokeWidth = 4.dp
                    )
                }
            }
            
            // 控制按钮区域 - 与ActiveVideoCallUI一致的控制
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 32.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // 切换摄像头
                FloatingActionButton(
                    onClick = { videoCallViewModel.switchCamera() },
                    containerColor = Color(0xFF666666),
                    modifier = Modifier.size(56.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Cameraswitch,
                        contentDescription = "切换摄像头",
                        tint = Color.White
                    )
                }
                
                // 麦克风控制
                FloatingActionButton(
                    onClick = { videoCallViewModel.toggleMicrophone() },
                    containerColor = if (videoCallState.isMicrophoneEnabled) Color(0xFF666666) else Color.Red,
                    modifier = Modifier.size(56.dp)
                ) {
                    Icon(
                        imageVector = if (videoCallState.isMicrophoneEnabled) Icons.Default.Mic else Icons.Default.MicOff,
                        contentDescription = if (videoCallState.isMicrophoneEnabled) "关闭麦克风" else "开启麦克风",
                        tint = Color.White
                    )
                }
                
                // 挂断电话
                FloatingActionButton(
                    onClick = onEndCall,
                    containerColor = Color.Red,
                    modifier = Modifier.size(56.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CallEnd,
                        contentDescription = "挂断",
                        tint = Color.White
                    )
                }

                // 摄像头控制
                FloatingActionButton(
                    onClick = { videoCallViewModel.toggleCamera() },
                    containerColor = if (videoCallState.isLocalVideoEnabled) Color(0xFF666666) else Color.Red,
                    modifier = Modifier.size(56.dp)
                ) {
                    Icon(
                        imageVector = if (videoCallState.isLocalVideoEnabled) Icons.Default.Videocam else Icons.Default.VideocamOff,
                        contentDescription = if (videoCallState.isLocalVideoEnabled) "关闭摄像头" else "开启摄像头",
                        tint = Color.White
                    )
                }
                
                // 扬声器控制
                FloatingActionButton(
                    onClick = { videoCallViewModel.toggleSpeaker() },
                    containerColor = if (videoCallState.isSpeakerEnabled) Color(0xFF666666) else Color.Red,
                    modifier = Modifier.size(56.dp)
                ) {
                    Icon(
                        imageVector = if (videoCallState.isSpeakerEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                        contentDescription = if (videoCallState.isSpeakerEnabled) "关闭扬声器" else "开启扬声器",
                        tint = Color.White
                    )
                }
            }
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
        onDismissRequest = { /* 不允许通过点击外部关闭 */ },
        properties = DialogProperties(
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            // 远程视频显示区域
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF2D2D2D))
            ) {
                // 显示远程视频流
                if (remoteVideoTrack != null && videoCallState.isRemoteVideoEnabled) {
                    VideoScreenView(
                        modifier = Modifier.fillMaxSize(),
                        videoTrack = remoteVideoTrack,
                        audioTrack = remoteAudioTrack
                    )
                } else {
                    // 显示占位符或用户信息
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = remoteUser.username ?: "未知用户",
                            color = Color.White,
                            fontSize = 24.sp
                        )
                    }
                }

                // 本地视频小窗口
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                        .background(Color(0xFF1E1E1E), shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
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
                Column(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(16.dp)
                ) {
                    Text(
                        text = remoteUser.username ?: "未知用户",
                        color = Color.White,
                        fontSize = 18.sp,
                        style = MaterialTheme.typography.titleLarge
                    )
                    
                    // 显示通话时长
                    videoCallState.callStartTime?.let { startTime ->
                        val duration by remember {
                            mutableStateOf(
                                (System.currentTimeMillis() - startTime) / 1000
                            )
                        }
                        Text(
                            text = formatDuration(duration),
                            color = Color.White,
                            fontSize = 14.sp
                        )
                    }
                }
                
                // 网络状态指示器
                if (videoCallState.errorMessage != null) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 16.dp)
                            .background(Color.Red, shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "网络问题",
                            color = Color.White,
                            fontSize = 12.sp
                        )
                    }
                }
            }
            
            // 控制按钮区域
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 32.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // 切换摄像头
                FloatingActionButton(
                    onClick = { videoCallViewModel.switchCamera() },
                    containerColor = Color(0xFF666666),
                    modifier = Modifier.size(56.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Cameraswitch,
                        contentDescription = "切换摄像头",
                        tint = Color.White
                    )
                }
                
                // 麦克风控制
                FloatingActionButton(
                    onClick = { videoCallViewModel.toggleMicrophone() },
                    containerColor = if (videoCallState.isMicrophoneEnabled) Color(0xFF666666) else Color.Red,
                    modifier = Modifier.size(56.dp)
                ) {
                    Icon(
                        imageVector = if (videoCallState.isMicrophoneEnabled) Icons.Default.Mic else Icons.Default.MicOff,
                        contentDescription = if (videoCallState.isMicrophoneEnabled) "关闭麦克风" else "开启麦克风",
                        tint = Color.White
                    )
                }
                
                // 挂断电话
                FloatingActionButton(
                    onClick = onEndCall,
                    containerColor = Color.Red,
                    modifier = Modifier.size(56.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CallEnd,
                        contentDescription = "挂断",
                        tint = Color.White
                    )
                }

                // 摄像头控制
                FloatingActionButton(
                    onClick = { videoCallViewModel.toggleCamera() },
                    containerColor = if (videoCallState.isLocalVideoEnabled) Color(0xFF666666) else Color.Red,
                    modifier = Modifier.size(56.dp)
                ) {
                    Icon(
                        imageVector = if (videoCallState.isLocalVideoEnabled) Icons.Default.Videocam else Icons.Default.VideocamOff,
                        contentDescription = if (videoCallState.isLocalVideoEnabled) "关闭摄像头" else "开启摄像头",
                        tint = Color.White
                    )
                }
                
                // 扬声器控制
                FloatingActionButton(
                    onClick = { videoCallViewModel.toggleSpeaker() },
                    containerColor = if (videoCallState.isSpeakerEnabled) Color(0xFF666666) else Color.Red,
                    modifier = Modifier.size(56.dp)
                ) {
                    Icon(
                        imageVector = if (videoCallState.isSpeakerEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                        contentDescription = if (videoCallState.isSpeakerEnabled) "关闭扬声器" else "开启扬声器",
                        tint = Color.White
                    )
                }
            }
            
            // 最小化按钮（右上角）
            IconButton(
                onClick = { 
                    videoCallViewModel.minimizeCall()
                    onEndCall()
                },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
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
    val mins = seconds / 60
    val secs = seconds % 60
    return String.format("%02d:%02d", mins, secs)
}