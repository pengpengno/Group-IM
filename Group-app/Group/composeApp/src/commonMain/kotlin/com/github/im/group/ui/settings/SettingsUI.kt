package com.github.im.group.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Update
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.github.im.group.manager.NotificationPreferenceStore
import com.github.im.group.viewmodel.ChatViewModel
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsUI(
    navHostController: NavHostController
) {
    val chatViewModel: ChatViewModel = koinViewModel()
    val notificationPreferenceStore: NotificationPreferenceStore = koinInject()
    val chatListState by chatViewModel.uiState.collectAsState()
    val notificationPreferences by notificationPreferenceStore.preferences.collectAsState()
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "消息与设置",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navHostController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFFF8FBFF), Color(0xFFFFFFFF))
                    )
                )
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            MessageSummaryCard(
                unreadCount = chatListState.totalUnreadCount,
                readAllInProgress = chatListState.readAllInProgress,
                onMarkAllRead = chatViewModel::markAllConversationsRead
            )

            Spacer(modifier = Modifier.height(16.dp))

            SettingsSection(title = "消息提醒") {
                SettingToggleItem(
                    icon = Icons.Default.Notifications,
                    title = "接收新消息提醒",
                    description = "保持会话实时提示和系统通知",
                    checked = notificationPreferences.enableNotifications,
                    onCheckedChange = { checked ->
                        scope.launch {
                            notificationPreferenceStore.updatePreferences {
                                it.copy(enableNotifications = checked)
                            }
                        }
                    }
                )
                HorizontalDivider()
                SettingToggleItem(
                    icon = Icons.Default.VolumeUp,
                    title = "声音提醒",
                    description = "收到新消息时播放轻提示音",
                    checked = notificationPreferences.enableSound,
                    onCheckedChange = { checked ->
                        scope.launch {
                            notificationPreferenceStore.updatePreferences {
                                it.copy(enableSound = checked)
                            }
                        }
                    }
                )
                HorizontalDivider()
                SettingToggleItem(
                    icon = Icons.Default.DoneAll,
                    title = "通知显示消息摘要",
                    description = "在列表实时提示中显示消息预览",
                    checked = notificationPreferences.enablePreview,
                    onCheckedChange = { checked ->
                        scope.launch {
                            notificationPreferenceStore.updatePreferences {
                                it.copy(enablePreview = checked)
                            }
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            SettingsSection(title = "账号与设备") {
                SettingItem(
                    icon = Icons.Default.PhoneAndroid,
                    title = "设备管理",
                    description = "查看当前登录设备与安全状态",
                    onClick = { }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            SettingsSection(title = "关于") {
                SettingItem(
                    icon = Icons.Default.Info,
                    title = "关于我们",
                    description = "了解 Group IM 的版本和能力",
                    onClick = { }
                )
                HorizontalDivider()
                SettingItem(
                    icon = Icons.Default.Update,
                    title = "检查更新",
                    description = "当前版本 v1.0.5",
                    onClick = { }
                )
            }
        }
    }
}

@Composable
private fun MessageSummaryCard(
    unreadCount: Int,
    readAllInProgress: Boolean,
    onMarkAllRead: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = Color.Transparent,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .background(
                    Brush.linearGradient(
                        colors = listOf(Color(0xFF0F172A), Color(0xFF1D4ED8))
                    ),
                    shape = RoundedCornerShape(24.dp)
                )
                .padding(20.dp)
        ) {
            Column {
                Text(
                    text = "消息中心",
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (unreadCount > 0) {
                        "当前还有 $unreadCount 条未读消息，支持一键清理红点。"
                    } else {
                        "当前没有未读消息，主页会继续帮你保持实时提醒。"
                    },
                    color = Color.White.copy(alpha = 0.82f),
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onMarkAllRead,
                    enabled = unreadCount > 0 && !readAllInProgress,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(if (readAllInProgress) "处理中…" else "一键已读")
                }
            }
        }
    }
}

@Composable
fun SettingsSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp)
        ) {
            content()
        }
    }
}

@Composable
fun SettingItem(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit,
    description: String? = null
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
                .size(40.dp)
                .background(Color(0xFFF1F5F9), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
            )

            if (description != null) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun SettingToggleItem(
    icon: ImageVector,
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(Color(0xFFF1F5F9), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}
