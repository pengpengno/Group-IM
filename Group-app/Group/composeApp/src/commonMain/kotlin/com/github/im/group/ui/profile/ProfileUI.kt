package com.github.im.group.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.github.im.group.manager.LoginStateManager
import com.github.im.group.ui.Login
import com.github.im.group.ui.UserAvatar
import com.github.im.group.viewmodel.UserViewModel
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileUI(
    navHostController: NavHostController
) {
    val userViewModel: UserViewModel = koinViewModel()
    val loginStateManager: LoginStateManager = koinInject()
    val currentUser by userViewModel.currentLocalUserInfo.collectAsState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("个人资料", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = { navHostController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(paddingValues)
        ) {
            // --- 头部：用户基本信息 ---
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 1.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(contentAlignment = Alignment.BottomEnd) {
                        UserAvatar(
                            username = currentUser?.username ?: "",
                            size = 100
                        )
                        // 相机图标（暗示可点击更换头像）
                        Surface(
                            modifier = Modifier
                                .size(32.dp)
                                .offset(x = 4.dp, y = 4.dp),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                            tonalElevation = 2.dp
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.CameraAlt,
                                    contentDescription = "更换头像",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = currentUser?.username ?: "加载中...",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = currentUser?.email ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // 在线状态标签
                    Surface(
                        color = Color(0xFFE8F5E9),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF4CAF50))
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "在线",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF2E7D32),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // --- 功能分组：账号安全 ---
            SettingSection(title = "账号设置") {
                ProfileSettingItem(
                    icon = Icons.Default.AccountCircle,
                    iconColor = Color(0xFF42A5F5),
                    title = "个人信息",
                    subtitle = "昵称、二维码、更多资料",
                    onClick = { /* TODO */ }
                )
                SettingDivider()
                ProfileSettingItem(
                    icon = Icons.Default.Lock,
                    iconColor = Color(0xFFFFA726),
                    title = "账号与安全",
                    subtitle = "密码、手机绑定、设备管理",
                    onClick = { /* TODO */ }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- 功能分组：应用设置 ---
            SettingSection(title = "通用设置") {
                ProfileSettingItem(
                    icon = Icons.Default.PrivacyTip,
                    iconColor = Color(0xFF66BB6A),
                    title = "隐私管理",
                    subtitle = "加好友验证、黑名单",
                    onClick = { navHostController.navigate("PrivacySettings") }
                )
                SettingDivider()
                ProfileSettingItem(
                    icon = Icons.Default.Notifications,
                    iconColor = Color(0xFFEF5350),
                    title = "消息通知",
                    subtitle = "提醒方式与免打扰设置",
                    onClick = { /* TODO */ }
                )
                SettingDivider()
                ProfileSettingItem(
                    icon = Icons.Default.Info,
                    iconColor = Color(0xFF78909C),
                    title = "关于 IM",
                    subtitle = "当前版本 v1.0.5",
                    onClick = { /* TODO */ }
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // --- 退出登录按钮 ---
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        loginStateManager.setLoggedOut()
                        navHostController.navigate(Login) {
                            popUpTo("Profile") { inclusive = true }
                        }
                    },
                color = MaterialTheme.colorScheme.surface
            ) {
                Box(
                    modifier = Modifier.padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "退出登录",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun SettingSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            modifier = Modifier.padding(start = 20.dp, bottom = 8.dp),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(content = content)
        }
    }
}

@Composable
fun SettingDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 68.dp), // 对齐文字
        thickness = 0.5.dp,
        color = MaterialTheme.colorScheme.outlineVariant
    )
}

@Composable
fun ProfileSettingItem(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 彩色图标容器
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(iconColor.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.outline,
            modifier = Modifier.size(20.dp)
        )
    }
}
