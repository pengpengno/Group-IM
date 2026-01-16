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

@Composable
fun VideoCallUI(
    remoteUser: UserInfo, // 添加远程用户信息参数
    localMediaStream: MediaStream?, // 本地媒体流
    videoCallState: VideoCallState, // 视频通话状态
    remoteVideoTrack: com.github.im.group.sdk.VideoTrack?,
    remoteAudioTrack: com.github.im.group.sdk.AudioTrack?,
    onEndCall: () -> Unit = {},
    onToggleCamera: () -> Unit = {},
    onToggleMicrophone: () -> Unit = {},
    onSwitchCamera: () -> Unit = {},
    onMinimizeCall: () -> Unit = {},
    onToggleSpeaker: () -> Unit = {}
) {
    val videoCallViewModel = koinViewModel<VideoCallViewModel>()
    val localStream by videoCallViewModel.localMediaStream.collectAsState()
    
    // 根据通话状态显示不同界面
    when (videoCallState.callStatus) {
        VideoCallStatus.INCOMING -> {
            // 显示来电通知
            VideoCallIncomingNotification(
                caller = remoteUser,
                onAccept = { 
                    videoCallViewModel.acceptCall()
                },
                onReject = {
                    videoCallViewModel.rejectCall()
                }
            )
        }
        VideoCallStatus.OUTGOING -> {
            // 显示拨出通知
            VideoCallOutgoingNotification(
                callee = remoteUser,
                onCancel = {
                    videoCallViewModel.endCall()
                }
            )
        }
        VideoCallStatus.ACTIVE, VideoCallStatus.CONNECTING -> {
            // 显示活跃通话界面
            ActiveVideoCallUI(
                remoteUser = remoteUser,
                localMediaStream = localMediaStream,
                remoteVideoTrack = remoteVideoTrack,
                remoteAudioTrack = remoteAudioTrack,
                videoCallState = videoCallState,
                onEndCall = onEndCall,
                onToggleCamera = onToggleCamera,
                onToggleMicrophone = onToggleMicrophone,
                onSwitchCamera = onSwitchCamera,
                onMinimizeCall = onMinimizeCall,
                onToggleSpeaker = onToggleSpeaker
            )
        }
        else -> {
            // 默认显示空闲状态
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "视频通话",
                    color = Color.White,
                    fontSize = 24.sp
                )
            }
        }
    }
}

@Composable
private fun ActiveVideoCallUI(
    remoteUser: UserInfo,
    localMediaStream: MediaStream?,
    remoteVideoTrack: com.github.im.group.sdk.VideoTrack?,
    remoteAudioTrack: com.github.im.group.sdk.AudioTrack?,
    videoCallState: VideoCallState,
    onEndCall: () -> Unit,
    onToggleCamera: () -> Unit,
    onToggleMicrophone: () -> Unit,
    onSwitchCamera: () -> Unit,
    onMinimizeCall: () -> Unit,
    onToggleSpeaker: () -> Unit
) {
    val videoCallViewModel = koinViewModel<VideoCallViewModel>()
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
                            text = remoteUser?.username ?: "未知用户",
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
                        text = remoteUser?.username ?: "未知用户",
                        color = Color.White,
                        fontSize = 18.sp,
                        style = MaterialTheme.typography.titleLarge
                    )
                    
                    // 显示通话时长
                    if (videoCallState.callStartTime != null) {
                        val duration by remember {
                            mutableStateOf(
                                (System.currentTimeMillis() - videoCallState.callStartTime) / 1000
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
                    onClick = onSwitchCamera,
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
                    onClick = onToggleMicrophone,
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
                    onClick = onToggleCamera,
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
                    onClick = onToggleSpeaker,
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
                onClick = onMinimizeCall,
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
 */
private fun formatDuration(seconds: Long): String {
    val mins = seconds / 60
    val secs = seconds % 60
    return String.format("%02d:%02d", mins, secs)
}