package com.github.im.group.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VideoCall
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.im.group.model.UserInfo

/**
 * 侧边栏 - 美化版
 */
@Composable
fun SideDrawer(
    userInfo: UserInfo?,
    onLogout: () -> Unit,
    onProfileClick: () -> Unit = {},
    onContactsClick: () -> Unit = {},
    onGroupsClick: () -> Unit = {},
    onMeetingsClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    appVersion: String = "v1.0.0"
) {
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        // --- 顶部用户信息区域 ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f))
                .padding(top = 48.dp, bottom = 24.dp, start = 20.dp, end = 20.dp)
        ) {
            Column {
                UserAvatar(
                    username = userInfo?.username ?: "游客", 
                    size = 64
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = userInfo?.username ?: "未登录",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = userInfo?.email ?: "登录体验更多功能",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- 菜单项列表 ---
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp)
        ) {
            DrawerItem(
                label = "联系人", 
                icon = Icons.Default.People, 
                onClick = onContactsClick,
                containerColor = Color(0xFFE3F2FD),
                iconTint = Color(0xFF2196F3)
            )
            DrawerItem(
                label = "音视频会议", 
                icon = Icons.Default.VideoCall, 
                onClick = onMeetingsClick,
                containerColor = Color(0xFFF1F8E9),
                iconTint = Color(0xFF4CAF50)
            )
            DrawerItem(
                label = "个人资料", 
                icon = Icons.Default.Person, 
                onClick = onProfileClick,
                containerColor = Color(0xFFFFF3E0),
                iconTint = Color(0xFFFF9800)
            )
            DrawerItem(
                label = "系统设置", 
                icon = Icons.Default.Settings, 
                onClick = onSettingsClick,
                containerColor = Color(0xFFF3E5F5),
                iconTint = Color(0xFF9C27B0)
            )
        }

        // --- 底部工具栏 ---
        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 20.dp),
            thickness = 0.5.dp,
            color = MaterialTheme.colorScheme.outlineVariant
        )
        
        DrawerItem(
            label = "退出登录", 
            icon = Icons.AutoMirrored.Filled.ExitToApp, 
            onClick = onLogout,
            iconTint = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Version $appVersion",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}

@Composable
fun DrawerItem(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    containerColor: Color = Color.Transparent,
    iconTint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    modifier: Modifier = Modifier.padding(vertical = 4.dp)
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = Color.Transparent,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(
                        if (containerColor == Color.Transparent) Color.Transparent else containerColor, 
                        RoundedCornerShape(8.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = iconTint,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
