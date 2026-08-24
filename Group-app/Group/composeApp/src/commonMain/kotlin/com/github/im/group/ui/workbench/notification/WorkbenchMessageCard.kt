package com.github.im.group.ui.workbench.notification

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.github.im.group.api.WorkbenchTaskDetail
import com.github.im.group.viewmodel.UserViewModel
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun WorkbenchMessageCard(
    rawText: String,
    isOwnMessage: Boolean,
    modifier: Modifier = Modifier,
) {
    val parseResult = remember(rawText) { parseWorkbenchCard(rawText) }
    when (parseResult) {
        is WorkbenchCardParseResult.Fallback -> WorkbenchFallbackCard(
            text = parseResult.fallbackText,
            modifier = modifier,
        )

        is WorkbenchCardParseResult.Valid -> WorkbenchValidCard(
            card = parseResult.card,
            target = parseResult.target,
            isOwnMessage = isOwnMessage,
            modifier = modifier,
        )
    }
}

@Composable
private fun WorkbenchValidCard(
    card: WorkbenchCardEnvelopeV1,
    target: WorkbenchDeepLinkTarget,
    isOwnMessage: Boolean,
    modifier: Modifier,
) {
    val userViewModel: UserViewModel = koinViewModel()
    val scope = rememberCoroutineScope()
    var loading by remember(card.eventId) { mutableStateOf(false) }
    var navigationResult by remember(card.eventId) { mutableStateOf<WorkbenchNavigationResult?>(null) }

    val containerColor = if (isOwnMessage) Color(0xFFF8FAFC) else MaterialTheme.colorScheme.surface
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = !loading) {
                loading = true
                scope.launch {
                    navigationResult = navigateWorkbenchTarget(userViewModel, target)
                    loading = false
                }
            },
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(1.dp, Color(0xFFDDE5F0)),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Dashboard,
                    contentDescription = null,
                    tint = Color(0xFF2563EB),
                    modifier = Modifier.size(20.dp),
                )
                Text(
                    text = categoryLabel(card.category),
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFF2563EB),
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(modifier = Modifier.weight(1f))
                card.status?.takeIf { it.isNotBlank() }?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Text(
                text = card.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF0F172A),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )

            card.summary?.takeIf { it.isNotBlank() }?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF475569),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (loading) {
                    CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 1.5.dp)
                    Text("正在验证当前资源…", style = MaterialTheme.typography.labelSmall, color = Color(0xFF64748B))
                } else {
                    Text("打开工作台", style = MaterialTheme.typography.labelMedium, color = Color(0xFF2563EB))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = Color(0xFF2563EB),
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
    }

    navigationResult?.let { result ->
        when (result) {
            is WorkbenchNavigationResult.TaskLoaded -> WorkbenchTaskDetailDialog(
                detail = result.detail,
                onDismiss = { navigationResult = null },
            )

            is WorkbenchNavigationResult.Failed -> WorkbenchNavigationMessageDialog(
                title = "无法打开工作台资源",
                message = result.message,
                onDismiss = { navigationResult = null },
            )

            is WorkbenchNavigationResult.Unsupported -> WorkbenchNavigationMessageDialog(
                title = "移动端暂未开放",
                message = result.message,
                onDismiss = { navigationResult = null },
            )
        }
    }
}

@Composable
private fun WorkbenchFallbackCard(text: String, modifier: Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBEB)),
        border = BorderStroke(1.dp, Color(0xFFFDE68A)),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = Icons.Default.ErrorOutline,
                contentDescription = null,
                tint = Color(0xFFD97706),
                modifier = Modifier.size(18.dp),
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("工作台消息", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                Text(text, style = MaterialTheme.typography.bodySmall, color = Color(0xFF475569))
            }
        }
    }
}

@Composable
private fun WorkbenchTaskDetailDialog(detail: WorkbenchTaskDetail, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(detail.title, maxLines = 2, overflow = TextOverflow.Ellipsis) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("状态：${detail.status}", style = MaterialTheme.typography.labelMedium)
                    Text("优先级：${detail.priority}", style = MaterialTheme.typography.labelMedium)
                }
                Text("进度：${detail.progress}%", style = MaterialTheme.typography.bodySmall)
                detail.dueAt?.let { Text("截止：$it", style = MaterialTheme.typography.bodySmall) }
                detail.description?.takeIf { it.isNotBlank() }?.let {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(it, style = MaterialTheme.typography.bodyMedium)
                }
                Text(
                    "此详情已从服务器重新获取；消息卡片本身不会授权完成、审批等写操作。",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("关闭") } },
    )
}

@Composable
private fun WorkbenchNavigationMessageDialog(
    title: String,
    message: String,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = { TextButton(onClick = onDismiss) { Text("知道了") } },
    )
}

private fun categoryLabel(category: WorkbenchCardCategory): String = when (category) {
    WorkbenchCardCategory.TASK -> "任务"
    WorkbenchCardCategory.APPROVAL -> "审批"
    WorkbenchCardCategory.ANNOUNCEMENT -> "公告"
    WorkbenchCardCategory.SCHEDULE -> "日程"
    WorkbenchCardCategory.REPORT -> "报告"
}
