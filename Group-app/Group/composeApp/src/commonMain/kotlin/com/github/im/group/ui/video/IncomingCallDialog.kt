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
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.github.im.group.model.UserInfo

/**
 * 视频通话来电弹窗
 *
 * @param caller 来电者信息
 * @param onAccept 接受通话
 * @param onReject 拒绝通话
 */
@Composable
fun VideoCallIncomingNotification(
    caller: UserInfo,
    onAccept: () -> Unit,
    onReject: () -> Unit
) {
    Dialog(
        onDismissRequest = { /* 不允许通过点击外部关闭 */ },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.7f))
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1F1F1F)),
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .wrapContentHeight()
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    // 用户头像占位符
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF333333)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = caller.username.firstOrNull()?.uppercaseChar()?.toString() ?: "U",
                            fontSize = 32.sp,
                            color = Color.White
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // 来电标题
                    Text(
                        text = "视频通话",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    // 来电者信息
                    Text(
                        text = caller.username,
                        fontSize = 18.sp,
                        color = Color.White
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // 操作按钮
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(48.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // 拒绝按钮
                        FloatingActionButton(
                            onClick = onReject,
                            containerColor = Color.Red,
                            shape = CircleShape,
                            modifier = Modifier.size(68.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CallEnd,
                                contentDescription = "拒绝",
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        
                        // 接受按钮
                        FloatingActionButton(
                            onClick = onAccept,
                            containerColor = Color.Green,
                            shape = CircleShape,
                            modifier = Modifier.size(68.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Call,
                                contentDescription = "接受",
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}