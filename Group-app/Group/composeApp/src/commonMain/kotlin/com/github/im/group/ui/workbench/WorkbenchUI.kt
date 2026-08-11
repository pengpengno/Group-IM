package com.github.im.group.ui.workbench

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * 移动端工作台是既有业务入口的聚合页，不在此处保存会话或会议状态。
 * 所有操作均上抛到主导航，确保模块状态仍由各自 ViewModel 管理。
 */
@Composable
fun WorkbenchUI(
    onOpenChats: () -> Unit,
    onOpenContacts: () -> Unit,
    onOpenMeetings: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val modules = listOf(
        WorkbenchModule("会话协作", "处理私聊、群聊和消息沟通", Icons.AutoMirrored.Filled.Chat, onOpenChats),
        WorkbenchModule("在线会议", "发起或加入音视频会议", Icons.Default.Videocam, onOpenMeetings),
        WorkbenchModule("组织通讯录", "查找同事并快速开始沟通", Icons.Default.Contacts, onOpenContacts),
        WorkbenchModule("个人设置", "管理通知与账号偏好", Icons.Default.Settings, onOpenSettings)
    )

    Column(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("常用协作", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text("快速进入常用工作能力", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        modules.forEach { module -> WorkbenchModuleCard(module) }
    }
}

private data class WorkbenchModule(
    val title: String,
    val description: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val onClick: () -> Unit
)

@Composable
private fun WorkbenchModuleCard(module: WorkbenchModule) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = module.onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Icon(module.icon, contentDescription = null, modifier = Modifier.size(28.dp), tint = MaterialTheme.colorScheme.primary)
            Column {
                Text(module.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(module.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
