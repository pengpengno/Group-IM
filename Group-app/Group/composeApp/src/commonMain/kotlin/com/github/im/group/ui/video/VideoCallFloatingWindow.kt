package com.github.im.group.ui.video

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.github.im.group.model.UserInfo
import com.github.im.group.sdk.VideoScreenView

/**
 * 视频通话悬浮窗组件
 */
@Composable
fun VideoCallFloatingWindow(
    remoteUser: UserInfo,
    localMediaStream: com.github.im.group.sdk.MediaStream?,
    remoteVideoTrack: com.github.im.group.sdk.VideoTrack?,
    remoteAudioTrack: com.github.im.group.sdk.AudioTrack?,
    onExpand: () -> Unit,
    onEndCall: () -> Unit,
    onToggleCamera: () -> Unit,
    onToggleMicrophone: () -> Unit,
    windowSize: Dp = 120.dp
) {
    Box(
        modifier = Modifier
            .size(windowSize)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF1E1E1E))
            .zIndex(1000f) // 确保悬浮窗在最顶层
    ) {
        // 视频预览
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(2.dp)
        ) {
            // 远程视频流（如果可用）
            if (remoteVideoTrack != null) {
                VideoScreenView(
                    modifier = Modifier.fillMaxSize(),
                    videoTrack = remoteVideoTrack,
                    audioTrack = remoteAudioTrack
                )
            } else {
                // 显示用户头像或占位符
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.DarkGray),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = remoteUser?.username?.take(1) ?: "?",
                        color = Color.White,
                        style = MaterialTheme.typography.headlineMedium
                    )
                }


                // 通话信息叠加层
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(4.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // 用户名
                    Text(
                        text = remoteUser?.username ?: "未知用户",
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1
                    )

                    // 控制按钮行
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // 切换摄像头按钮
                        IconButton(
                            onClick = onToggleCamera,
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.5f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Cameraswitch,
                                contentDescription = "切换摄像头",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        // 麦克风按钮
                        IconButton(
                            onClick = onToggleMicrophone,
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.5f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = "静音/取消静音",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            // 展开按钮（右下角）
            IconButton(
                onClick = onExpand,
                modifier = Modifier
                    .size(20.dp)
                    .align(Alignment.BottomEnd)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.5f))
            ) {
                Icon(
                    imageVector = Icons.Default.Expand,
                    contentDescription = "展开通话",
                    tint = Color.White,
                    modifier = Modifier.size(12.dp)
                )
            }

            // 挂断按钮（左下角）
            IconButton(
                onClick = onEndCall,
                modifier = Modifier
                    .size(20.dp)
                    .align(Alignment.BottomStart)
                    .clip(CircleShape)
                    .background(Color.Red)
            ) {
                Icon(
                    imageVector = Icons.Default.CallEnd,
                    contentDescription = "挂断通话",
                    tint = Color.White,
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}
