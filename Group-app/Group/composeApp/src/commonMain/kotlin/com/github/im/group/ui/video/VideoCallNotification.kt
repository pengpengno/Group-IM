package com.github.im.group.ui.video

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.github.im.group.model.UserInfo
import com.github.im.group.ui.UserAvatar

/** 视频通话来电通知组件 (Incoming Video Call) */
@Composable
fun VideoCallIncomingNotification(
        caller: UserInfo,
        onAccept: () -> Unit,
        onReject: () -> Unit,
        onSwipeToReject: () -> Unit = onReject,
        onSwipeToAccept: () -> Unit = onAccept
) {
    Dialog(
            onDismissRequest = onReject,
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
                // Caller Avatar
                UserAvatar(username = caller.username, size = 120)

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                        text = caller.username,
                        color = Color.White,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                        text = "邀请你进行视频通话...",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Normal
                )

                Spacer(modifier = Modifier.height(64.dp))

                // Action Buttons
                Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                ) {
                    // Reject Button
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        FloatingActionButton(
                                onClick = onReject,
                                containerColor = Color(0xFFFF3B30),
                                contentColor = Color.White,
                                shape = CircleShape,
                                modifier = Modifier.size(72.dp)
                        ) {
                            Icon(
                                    imageVector = Icons.Default.CallEnd,
                                    contentDescription = "拒绝",
                                    modifier = Modifier.size(32.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("拒绝", color = Color.White, fontSize = 14.sp)
                    }

                    // Accept Button
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        FloatingActionButton(
                                onClick = onAccept,
                                containerColor = Color(0xFF34C759),
                                contentColor = Color.White,
                                shape = CircleShape,
                                modifier = Modifier.size(72.dp)
                        ) {
                            Icon(
                                    imageVector = Icons.Default.Call,
                                    contentDescription = "接听",
                                    modifier = Modifier.size(32.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("接听", color = Color.White, fontSize = 14.sp)
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
