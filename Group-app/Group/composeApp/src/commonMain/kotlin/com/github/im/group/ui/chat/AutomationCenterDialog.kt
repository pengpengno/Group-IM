package com.github.im.group.ui.chat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.github.im.group.api.AiBotApi
import com.github.im.group.api.AutomationExecutionDto
import com.github.im.group.api.AutomationRuleDto
import kotlinx.coroutines.launch

@Composable
fun AutomationCenterDialog(conversationId: Long, onDismiss: () -> Unit) {
    val scope = rememberCoroutineScope(); var rules by remember { mutableStateOf<List<AutomationRuleDto>>(emptyList()) }
    var executions by remember { mutableStateOf<List<AutomationExecutionDto>>(emptyList()) }; var contains by remember { mutableStateOf("") }; var reply by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(true) }; var error by remember { mutableStateOf<String?>(null) }
    suspend fun load() { loading = true; error = null; runCatching { Pair(AiBotApi.listRules(), AiBotApi.listExecutions()) }.onSuccess { (r, e) -> rules = r.filter { it.conversationId == conversationId.toString() }; executions = e }.onFailure { error = "加载失败，请重试。" }; loading = false }
    LaunchedEffect(conversationId) { load() }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("自动化中心") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(contains, { contains = it }, label = { Text("触发关键词（可选）") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(reply, { reply = it }, label = { Text("自动回复") }, modifier = Modifier.fillMaxWidth())
            Button(enabled = reply.isNotBlank() && !loading, onClick = { scope.launch { runCatching { AiBotApi.createReplyRule(conversationId, contains, reply) }.onSuccess { contains = ""; reply = ""; load() }.onFailure { error = "创建规则失败。" } } }) { Text("创建规则") }
            error?.let { Text(it) }; Text("我的规则")
            LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f, false)) { items(rules, key = { it.ruleId }) { rule -> androidx.compose.foundation.layout.Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) { Text(if (rule.enabled) "已启用" else "已停用"); Switch(rule.enabled, onCheckedChange = { checked -> scope.launch { runCatching { AiBotApi.setRuleEnabled(rule.ruleId, checked) }.onSuccess { load() }.onFailure { error = "更新规则失败。" } } }) } } }
            Text("最近执行"); executions.take(5).forEach { Text("${it.status} · ${it.summary}") }
        }
    }, confirmButton = { TextButton(onClick = onDismiss) { Text("关闭") } })
}
