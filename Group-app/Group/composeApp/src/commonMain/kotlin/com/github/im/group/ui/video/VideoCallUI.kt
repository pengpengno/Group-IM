package com.github.im.group.ui.video

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import androidx.navigation.NavHostController
import com.github.im.group.model.UserInfo
import com.github.im.group.sdk.LocalVideoPreview
import com.github.im.group.sdk.RemoteVideoView

@Composable
fun VideoCallUI(
    navHostController: NavHostController,
    remoteUser: UserInfo?, // 添加远程用户信息参数
    localMediaStream: com.github.im.group.sdk.MediaStream?, // 本地媒体流
    onEndCall: () -> Unit = {},
    onToggleCamera: () -> Unit = {},
    onToggleMicrophone: () -> Unit = {},
    onSwitchCamera: () -> Unit = {}
) {
    var isCameraEnabled by remember { mutableStateOf(true) }
    var isMicrophoneEnabled by remember { mutableStateOf(true) }
    
    Dialog(onDismissRequest = { onEndCall() }) {
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
                RemoteVideoView(
                    modifier = Modifier.fillMaxSize()
                )
                
                // 本地视频小窗口
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                ) {
                    // 显示本地视频流
                    LocalVideoPreview(
                        modifier = Modifier.fillMaxSize()
                    )
                }
                
                // 用户名显示
                Text(
                    text = remoteUser?.username ?: "未知用户",
                    color = Color.White,
                    fontSize = 18.sp,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(16.dp)
                )
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
                    onClick = {
                        onSwitchCamera()
                    },
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
                    onClick = {
                        isMicrophoneEnabled = !isMicrophoneEnabled
                        onToggleMicrophone()
                    },
                    containerColor = if (isMicrophoneEnabled) Color(0xFF666666) else Color.Red,
                    modifier = Modifier.size(56.dp)
                ) {
                    Icon(
                        imageVector = if (isMicrophoneEnabled) Icons.Default.Mic else Icons.Default.MicOff,
                        contentDescription = if (isMicrophoneEnabled) "关闭麦克风" else "开启麦克风",
                        tint = Color.White
                    )
                }
                
                // 挂断电话
                FloatingActionButton(
                    onClick = { onEndCall() },
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
                    onClick = {
                        isCameraEnabled = !isCameraEnabled
                        onToggleCamera()
                    },
                    containerColor = if (isCameraEnabled) Color(0xFF666666) else Color.Red,
                    modifier = Modifier.size(56.dp)
                ) {
                    Icon(
                        imageVector = if (isCameraEnabled) Icons.Default.Videocam else Icons.Default.VideocamOff,
                        contentDescription = if (isCameraEnabled) "关闭摄像头" else "开启摄像头",
                        tint = Color.White
                    )
                }
                
                // 其他功能按钮占位
                FloatingActionButton(
                    onClick = { /* TODO */ },
                    containerColor = Color(0xFF666666),
                    modifier = Modifier.size(56.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "更多",
                        tint = Color.White
                    )
                }
            }
        }
    }
}