package com.github.im.group.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VideoCall
import androidx.compose.material3.DividerDefaults
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

/**
 * 侧边栏
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

//    val userRepository : UserRepository = koinInject<UserRepository>()
//
//
//    var userInfo : UserInfo? by remember { mutableStateOf(null) }
//
//    LaunchedEffect(userInfo){
//        userInfo = GlobalCredentialProvider.storage.getUserInfo()
//    }

//    val userInfoState = userRepository.userState.collectAsState()
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .width(260.dp)
            .background(Color(0xFF111B21))
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
            UserAvatar(username = userInfo?.username ?: "游客", size = 56)
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = userInfo?.username ?: "加载中...",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
                Text(
                    text = if (userInfo != null) "点击查看个人资料" else "未登录用户",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFA3A3A3)
                )
            }
        }

        HorizontalDivider(
            modifier = Modifier.padding(vertical = 8.dp),
            thickness = DividerDefaults.Thickness,
            color = Color(0xFF202C33)
        )

        // 菜单项区域
        DrawerItem("联系人", Icons.Default.People, onContactsClick, Color.White)
//        DrawerItem("群组", Icons.Default.Group, onGroupsClick, Color.White)
        DrawerItem("会议", Icons.Default.VideoCall, onMeetingsClick, Color.White)
        DrawerItem("个人资料", Icons.Default.Person, onProfileClick, Color.White)
        DrawerItem("设置", Icons.Default.Settings, onSettingsClick, Color.White)

        Spacer(modifier = Modifier.weight(1f)) // 底部对齐

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color(0xFF202C33))

        // 退出 & 版本
        DrawerItem("退出登录", Icons.AutoMirrored.Filled.ExitToApp, onLogout, Color.Red)

        Text(
            text = "版本号：$appVersion",
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF667781),
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
            .padding(vertical = 14.dp)
    ) {
        Icon(
            icon,
            contentDescription = label,
            tint = iconTint,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(24.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White
        )
    }
}