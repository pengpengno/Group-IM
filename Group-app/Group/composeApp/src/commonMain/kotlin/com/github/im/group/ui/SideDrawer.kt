package com.github.im.group.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VideoCall
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.github.im.group.model.UserInfo

@Composable
fun SideDrawer(
    userInfo: UserInfo,
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
            .fillMaxSize()
            .background(Color.White)
            .padding(16.dp)
    ) {
        // 顶部：用户信息
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onProfileClick() }
                .padding(bottom = 24.dp)
        ) {
            UserAvatar(username = userInfo.username)
//            Icon(
//                imageVector = Icons.Default.Person,
//                contentDescription = "用户头像",
//                tint = Color(0xFF0088CC),
//                modifier = Modifier
//                    .background(Color(0x220088CC), shape = MaterialTheme.shapes.medium)
//                    .padding(12.dp)
//            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = userInfo.username,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "点击查看个人资料",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
        }

        Divider(modifier = Modifier.padding(vertical = 8.dp))

        // 菜单项区域
        DrawerItem("联系人", Icons.Default.People, onContactsClick)
        DrawerItem("群组", Icons.Default.Group, onGroupsClick)
        DrawerItem("会议", Icons.Default.VideoCall, onMeetingsClick)
        DrawerItem("设置", Icons.Default.Settings, onSettingsClick)

        Spacer(modifier = Modifier.weight(1f)) // 底部对齐

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        // 退出 & 版本
        DrawerItem("退出登录", Icons.AutoMirrored.Filled.ExitToApp, onLogout, Color.Red)

        Text(
            text = "版本号：$appVersion",
            style = MaterialTheme.typography.bodySmall,
            color = Color.LightGray,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(top = 8.dp)
        )
    }
}

@Composable
fun DrawerItem(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    iconTint: Color = Color(0xFF0088CC)
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp)
    ) {
        Icon(
            icon,
            contentDescription = label,
            tint = iconTint
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}
