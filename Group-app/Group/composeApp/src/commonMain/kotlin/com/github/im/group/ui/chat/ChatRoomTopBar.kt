package com.github.im.group.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.VideoCall
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.im.group.model.UserInfo
import com.github.im.group.ui.theme.ThemeTokens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatRoomTopBar(
    roomName: String,
    roomSubtitle: String,
    isGroupConversation: Boolean,
    remoteUser: UserInfo?,
    onBack: () -> Unit,
    onStartVideoCall: () -> Unit,
    onOpenAutomation: () -> Unit = {},
    showAutomationAction: Boolean = false,
    showVideoCallAction: Boolean = true
) {
    CenterAlignedTopAppBar(
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = Color.White,
            titleContentColor = ThemeTokens.TextMain,
            navigationIconContentColor = ThemeTokens.TextMain,
            actionIconContentColor = ThemeTokens.TextMain
        ),
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "返回"
                )
            }
        },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(
                            if (isGroupConversation) {
                                ThemeTokens.PrimaryBlue.copy(alpha = 0.15f)
                            } else {
                                ThemeTokens.Sphere2.copy(alpha = 0.15f)
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isGroupConversation) Icons.Default.Groups else Icons.Default.Person,
                        contentDescription = null,
                        tint = if (isGroupConversation) ThemeTokens.PrimaryBlue else ThemeTokens.Sphere2,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = roomName.ifBlank { "聊天" },
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                        color = ThemeTokens.TextMain,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (roomSubtitle.isNotBlank()) {
                        Text(
                            text = roomSubtitle,
                            fontSize = 12.sp,
                            color = ThemeTokens.TextSecondary,
                            maxLines = 1
                        )
                    }
                }
            }
        },
        actions = {
            if (isGroupConversation && showAutomationAction) {
                TextButton(onClick = onOpenAutomation) { Text("自动化") }
            }
            if (showVideoCallAction) {
                IconButton(onClick = onStartVideoCall) {
                    Icon(
                        imageVector = Icons.Default.VideoCall,
                        contentDescription = "视频通话",
                        tint = ThemeTokens.PrimaryBlue,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        },
        modifier = Modifier.fillMaxWidth()
    )
}
