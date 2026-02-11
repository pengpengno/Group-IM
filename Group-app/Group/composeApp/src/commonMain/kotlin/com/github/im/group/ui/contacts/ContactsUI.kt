package com.github.im.group.ui.contacts

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.github.im.group.model.OrgTreeNode
import com.github.im.group.ui.UserItem
import com.github.im.group.ui.conversation
import com.github.im.group.viewmodel.ContactsViewModel
import io.github.aakira.napier.Napier
import org.koin.compose.viewmodel.koinViewModel

/**
 * 联系人面板 - 显示组织架构
 */
@Composable
fun ContactsUI(
    navHostController: NavHostController,
) {
    val contactsViewModel: ContactsViewModel = koinViewModel()
    val organizationTree by contactsViewModel.organizationTree.collectAsState()
    val loading by contactsViewModel.loading.collectAsState()
    val expandedDepartments by contactsViewModel.expandedDepartments.collectAsState()
    val sessionCreationState by contactsViewModel.sessionCreationState.collectAsState()
    
    val scope = rememberCoroutineScope()
    var contactSearchQuery by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        contactsViewModel.getOrganizationStructure()
    }

    // 处理会话创建状态变化
    LaunchedEffect(sessionCreationState) {
        when (val state = sessionCreationState) {
            is ContactsViewModel.SessionCreationState.Success -> {
                // 会话创建成功，导航到聊天室
                Napier.d("会话创建成功，导航到聊天室: conversationId=${state.conversationId}")
                navHostController.navigate(conversation(state.conversationId))
                // 重置状态
                contactsViewModel.resetSessionCreationState()
            }
            is ContactsViewModel.SessionCreationState.Error -> {
                Napier.e("会话创建失败: ${state.message}")
                // 可以显示错误提示
            }
            else -> {
                // 其他状态不需要特殊处理
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // --- 搜索与工具栏 ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = contactSearchQuery,
                onValueChange = { contactSearchQuery = it },
                placeholder = { Text("搜索组织或人员", fontSize = 14.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(20.dp)) },
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
                shape = RoundedCornerShape(26.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedBorderColor = Color.Transparent,
                    focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                ),
                singleLine = true
            )
            
            Spacer(modifier = Modifier.width(8.dp))

            // --- 刷新按钮 ---
            Surface(
                modifier = Modifier
                    .size(40.dp)
                    .clickable { contactsViewModel.getOrganizationStructure() },
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "刷新",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // --- 组织架构树 ---
        if (loading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("加载中...", style = MaterialTheme.typography.bodyMedium)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp)
            ) {
                items(organizationTree) { node ->
                    when (node.type) {
                        OrgTreeNode.NodeType.DEPARTMENT -> {
                            DepartmentItem(
                                department = node,
                                isExpanded = expandedDepartments.contains(node.id),
                                onToggleExpand = { contactsViewModel.toggleDepartmentExpanded(node.id) }
                            )
                        }
                        OrgTreeNode.NodeType.USER -> {
                            UserItem(
                                userInfo = node.userInfo!!,
                                onClick = {
                                    // 使用会话预创建功能替代直接导航
                                    contactsViewModel.preCreateSessionAndNavigate(
                                        friendId = node.userInfo!!.userId
                                    ) { conversationId ->
                                        // 导航逻辑在ViewModel中处理
                                        Napier.d("准备导航到聊天室: conversationId=$conversationId")
                                    }
                                }
                            )
                        }

                    }
                }
            }
        }
    }

    // 显示会话创建加载状态
    when (val state = sessionCreationState) {
        is ContactsViewModel.SessionCreationState.Creating -> {
            SessionCreationDialog(friendId = state.friendId)
        }
        is ContactsViewModel.SessionCreationState.Error -> {
            SessionCreationErrorDialog(
                errorMessage = state.message,
                onDismiss = { contactsViewModel.resetSessionCreationState() }
            )
        }
        else -> {
            // 不显示对话框
        }
    }
}

@Composable
private fun DepartmentItem(
    department: OrgTreeNode,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggleExpand)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (isExpanded) Icons.Default.ExpandMore else Icons.Default.ChevronRight,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(8.dp))
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.size(32.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Business,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = department.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            department.departmentInfo?.description?.let { description ->
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
private fun SessionCreationDialog(friendId: Long) {
    AlertDialog(
        onDismissRequest = { /* 不允许取消 */ },
        title = { Text("创建会话") },
        text = { Text("正在为您和用户 $friendId 创建聊天会话...") },
        confirmButton = {
            Row {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("请稍候")
            }
        },
        dismissButton = {}
    )
}

@Composable
private fun SessionCreationErrorDialog(
    errorMessage: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("创建会话失败") },
        text = { Text(errorMessage) },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("确定")
            }
        }
    )
}
