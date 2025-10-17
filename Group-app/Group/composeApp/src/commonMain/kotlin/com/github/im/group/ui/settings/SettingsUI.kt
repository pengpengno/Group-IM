package com.github.im.group.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController

@Composable
fun SettingsUI(
    navHostController: NavHostController
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // 账户与安全设置
        SettingsSection(
            title = "账户与安全"
        ) {
            SettingItem(
                icon = Icons.Default.Lock,
                title = "密码修改",
                description = "修改登录密码",
                onClick = { /* TODO: 导航到密码修改页面 */ }
            )
            
            Divider()
            
            SettingItem(
                icon = Icons.Default.PhoneAndroid,
                title = "设备管理",
                description = "管理已登录的设备",
                onClick = { /* TODO: 导航到设备管理页面 */ }
            )
        }
        
        // 通知设置
        SettingsSection(
            title = "通知"
        ) {
            var enableNotifications by remember { mutableStateOf(true) }
            var enableSound by remember { mutableStateOf(true) }
            
            SettingToggleItem(
                icon = Icons.Default.Notifications,
                title = "启用通知",
                checked = enableNotifications,
                onCheckedChange = { enableNotifications = it }
            )
            
            Divider()
            
            SettingToggleItem(
                icon = Icons.Default.VolumeUp,
                title = "声音提醒",
                checked = enableSound,
                onCheckedChange = { enableSound = it }
            )
        }
        
        // 隐私设置
        SettingsSection(
            title = "隐私"
        ) {
            var allowFindMe by remember { mutableStateOf(true) }
            var showOnlineStatus by remember { mutableStateOf(true) }
            
            SettingToggleItem(
                icon = Icons.Default.People,
                title = "允许通过手机号找到我",
                checked = allowFindMe,
                onCheckedChange = { allowFindMe = it }
            )
            
            Divider()
            
            SettingToggleItem(
                icon = Icons.Default.Visibility,
                title = "显示在线状态",
                checked = showOnlineStatus,
                onCheckedChange = { showOnlineStatus = it }
            )
        }
        
        // 关于
        SettingsSection(
            title = "关于"
        ) {
            SettingItem(
                icon = Icons.Default.Info,
                title = "关于我们",
                onClick = { /* TODO: 显示关于我们信息 */ }
            )
            
            Divider()
            
            SettingItem(
                icon = Icons.Default.Update,
                title = "检查更新",
                description = "当前版本 v1.2.3",
                onClick = { /* TODO: 检查更新逻辑 */ }
            )
        }
    }
}

@Composable
fun SettingsSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp)
        )
        
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            content()
        }
    }
}

@Composable
fun SettingItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
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
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp)
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge
            )
            
            if (description != null) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        Icon(
            imageVector = Icons.Default.ArrowForwardIos,
            contentDescription = null,
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
fun SettingToggleItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp)
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
        
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}