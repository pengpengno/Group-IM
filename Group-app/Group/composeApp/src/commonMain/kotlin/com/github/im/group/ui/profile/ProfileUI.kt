package com.github.im.group.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.github.im.group.manager.LoginStateManager
import com.github.im.group.ui.Login
import com.github.im.group.ui.UserAvatar
import com.github.im.group.viewmodel.UserViewModel
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ProfileUI(
    navHostController: NavHostController
) {
    val userViewModel: UserViewModel = koinViewModel()
    val loginStateManager: LoginStateManager = koinInject()
    val currentUser by userViewModel.currentLocalUserInfo.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .verticalScroll(rememberScrollState())
    ) {
        // --- 头部：漂亮的渐变背景和用户信息 ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
        ) {
            // 背景渐变
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primaryContainer,
                                MaterialTheme.colorScheme.surface
                            )
                        )
                    )
            )

            // 用户信息卡片
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .align(Alignment.BottomCenter),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Row(
                    modifier = Modifier
                        .padding(20.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    UserAvatar(
                        username = currentUser?.username ?: "User",
                        size = 72
                    )
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = currentUser?.username ?: "未设置昵称",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = currentUser?.email ?: "还没有添加邮箱",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // 在线状态标签
                        Row(verticalAlignment = Alignment.CenterVertically) {
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
                                color = Color(0xFF2E7D32)
                            )
                        }
                    }
                    
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // --- 功能菜单区域 ---
        Column(
            modifier = Modifier.padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 个人信息组
            ProfileSection(title = "账号信息") {
                ProfileMenuItem(
                    icon = Icons.Default.AccountBox,
                    title = "个人资料",
                    color = Color(0xFF42A5F5),
                    onClick = { /* TODO */ }
                )
                HorizontalDivider(modifier = Modifier.padding(start = 48.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                ProfileMenuItem(
                    icon = Icons.Default.Security,
                    title = "账号安全",
                    color = Color(0xFF66BB6A),
                    onClick = { /* TODO */ }
                )
            }

            // 系统设置组
            ProfileSection(title = "系统设置") {
                ProfileMenuItem(
                    icon = Icons.Default.NotificationsNone,
                    title = "消息通知",
                    color = Color(0xFFFFA726),
                    onClick = { /* TODO */ }
                )
                HorizontalDivider(modifier = Modifier.padding(start = 48.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                ProfileMenuItem(
                    icon = Icons.Default.Palette,
                    title = "个性装扮",
                    color = Color(0xFFAB47BC),
                    onClick = { /* TODO */ }
                )
                HorizontalDivider(modifier = Modifier.padding(start = 48.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                ProfileMenuItem(
                    icon = Icons.Default.Settings,
                    title = "通用设置",
                    color = Color(0xFF78909C),
                    onClick = { /* TODO */ }
                )
            }

            // 其他信息组
            ProfileSection(title = "其他") {
                ProfileMenuItem(
                    icon = Icons.AutoMirrored.Filled.HelpOutline,
                    title = "帮助与反馈",
                    color = Color(0xFF26A69A),
                    onClick = { /* TODO */ }
                )
            }

            // 退出登录
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable {
                        loginStateManager.setLoggedOut()
                        navHostController.navigate(Login) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f),
                contentColor = MaterialTheme.colorScheme.error
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("退出当前账号", fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(40.dp))
    }
}

@Composable
fun ProfileSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
        )
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(content = content)
        }
    }
}

@Composable
fun ProfileMenuItem(
    icon: ImageVector,
    title: String,
    color: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(color.copy(alpha = 0.1f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.outlineVariant,
            modifier = Modifier.size(20.dp)
        )
    }
}
