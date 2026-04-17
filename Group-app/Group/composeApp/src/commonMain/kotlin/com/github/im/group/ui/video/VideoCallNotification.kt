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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.github.im.group.model.UserInfo
import com.github.im.group.ui.UserAvatar

/** 视频通话来电通知组件 (Incoming Video Call) - Premium Redesign */
@Composable
fun VideoCallIncomingNotification(
    caller: UserInfo,
    onAccept: () -> Unit,
    onReject: () -> Unit,
) {
    Dialog(
        onDismissRequest = onReject,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    ) {
        val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }

        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            // 背景渐变
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF1A1A1A),
                                Color(0xFF0D0D0D),
                                Color(0xFF000000)
                            )
                        )
                    )
            )

            // 装饰性模糊圆圈 (模拟高端质感)
            Box(
                modifier = Modifier
                    .size(300.dp)
                    .align(Alignment.TopStart)
                    .padding(top = (-100).dp, start = (-100).dp)
                    .background(Color(0xFF34C759).copy(alpha = 0.15f), CircleShape)
            )

            Box(
                modifier = Modifier
                    .size(250.dp)
                    .align(Alignment.BottomEnd)
                    .padding(bottom = (-80).dp, end = (-80).dp)
                    .background(Color(0xFFFF3B30).copy(alpha = 0.15f), CircleShape)
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 64.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // 上部分：呼叫者信息
                Column(
                    modifier = Modifier.padding(top = 48.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Avatar with pulse effect placeholder
                    Box(contentAlignment = Alignment.Center) {
                        // Pulse rings could be added here
                        UserAvatar(
                            username = caller.username,
                            size = 140
                        )
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    Text(
                        text = caller.username ?: "未知用户",
                        color = Color.White,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(Color(0xFF34C759), CircleShape)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "视频通话邀请...",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // 下部分：交互按钮
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 32.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 拒绝按钮
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .background(Color(0xFFFF3B30).copy(alpha = 0.2f), CircleShape)
                                .padding(4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            FloatingActionButton(
                                onClick = onReject,
                                containerColor = Color(0xFFFF3B30),
                                contentColor = Color.White,
                                shape = CircleShape,
                                modifier = Modifier.size(68.dp),
                                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 0.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CallEnd,
                                    contentDescription = "拒绝",
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "拒绝",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // 接听按钮
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .background(Color(0xFF34C759).copy(alpha = 0.2f), CircleShape)
                                .padding(4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            FloatingActionButton(
                                onClick = onAccept,
                                containerColor = Color(0xFF34C759),
                                contentColor = Color.White,
                                shape = CircleShape,
                                modifier = Modifier.size(68.dp),
                                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 0.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Call,
                                    contentDescription = "接听",
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "接听",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

/** 视频通话拨出通知组件 (Outgoing Video Call) */
@Composable
fun VideoCallOutgoingNotification(callee: UserInfo, onCancel: () -> Unit) {
    Dialog(
            onDismissRequest = onCancel,
            properties =
                    DialogProperties(
                            usePlatformDefaultWidth = false,
                            dismissOnBackPress = false,
                            dismissOnClickOutside = false
                    )
    ) {
        Box(
                modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.85f)),
                contentAlignment = Alignment.Center
        ) {
            Column(
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Callee Avatar
                UserAvatar(username = callee.username, size = 120)

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                        text = callee.username,
                        color = Color.White,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                        text = "正在呼叫...",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Normal
                )

                Spacer(modifier = Modifier.height(64.dp))

                // Cancel Button
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    FloatingActionButton(
                            onClick = onCancel,
                            containerColor = Color(0xFFFF3B30),
                            contentColor = Color.White,
                            shape = CircleShape,
                            modifier = Modifier.size(72.dp)
                    ) {
                        Icon(
                                imageVector = Icons.Default.CallEnd,
                                contentDescription = "取消呼叫",
                                modifier = Modifier.size(32.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("取消", color = Color.White, fontSize = 14.sp)
                }
            }
        }
    }
}
