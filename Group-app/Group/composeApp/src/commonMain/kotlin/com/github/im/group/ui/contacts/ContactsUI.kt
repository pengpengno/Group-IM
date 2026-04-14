package com.github.im.group.ui.contacts

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.ExpandMore
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
import androidx.compose.ui.draw.clip
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.github.im.group.ui.theme.ThemeTokens
import com.github.im.group.viewmodel.ContactsViewModel
import io.github.aakira.napier.Napier
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ContactsUI(
    navHostController: NavHostController,
) {
    val contactsViewModel: ContactsViewModel = koinViewModel()
    val organizationTree by contactsViewModel.organizationTree.collectAsState()
    val loading by contactsViewModel.loading.collectAsState()
    val expandedDepartments by contactsViewModel.expandedDepartments.collectAsState()
    val sessionCreationState by contactsViewModel.sessionCreationState.collectAsState()
    val isOfflineData by contactsViewModel.isOfflineData.collectAsState()

    var contactSearchQuery by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        contactsViewModel.getOrganizationStructure()
    }

    LaunchedEffect(sessionCreationState) {
        when (val state = sessionCreationState) {
            is ContactsViewModel.SessionCreationState.Success -> {
                Napier.d("Navigate to conversation ${state.conversationId}")
                navHostController.navigate(conversation(state.conversationId))
                contactsViewModel.resetSessionCreationState()
            }
            is ContactsViewModel.SessionCreationState.Error -> {
                Napier.e("Session creation failed: ${state.message}")
            }
            else -> Unit
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ThemeTokens.BackgroundDark)
            .padding(top = 16.dp)
    ) {
//        Column(modifier = Modifier.padding(horizontal = 20.dp, bottom = 12.dp)) {
//            Text(
//                text = "组织架构",
//                style = MaterialTheme.typography.headlineMedium,
//                color = Color.White,
//                fontWeight = FontWeight.Bold
//            )
//        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                .background(Color.White.copy(alpha = 0.98f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    OutlinedTextField(
                        value = contactSearchQuery,
                        onValueChange = { contactSearchQuery = it },
                        placeholder = { Text("搜索部门或成员", fontSize = 14.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = ThemeTokens.TextMuted) },
                        singleLine = true,
                        shape = RoundedCornerShape(20.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = Color.Transparent,
                            focusedBorderColor = ThemeTokens.PrimaryBlue.copy(alpha = 0.4f),
                            unfocusedContainerColor = Color(0xFFF1F5F9),
                            focusedContainerColor = Color(0xFFF1F5F9)
                        ),
                        modifier = Modifier.weight(1f).height(52.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Surface(
                        onClick = { contactsViewModel.getOrganizationStructure() },
                        shape = RoundedCornerShape(16.dp),
                        color = ThemeTokens.PrimaryBlue.copy(alpha = 0.1f)
                    ) {
                        Box(modifier = Modifier.size(52.dp), contentAlignment = Alignment.Center) {
                            if (loading) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = ThemeTokens.PrimaryBlue)
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "刷新",
                                    tint = ThemeTokens.PrimaryBlue
                                )
                            }
                        }
                    }
                }

                if (isOfflineData) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFFFEF2F2),
                        modifier = Modifier.padding(vertical = 12.dp).fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CloudOff, null, tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("离线数据模式", style = MaterialTheme.typography.labelSmall, color = Color(0xFFB91C1C))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (loading && organizationTree.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = ThemeTokens.PrimaryBlue)
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(filterNodes(organizationTree, contactSearchQuery)) { node ->
                            OrganizationNode(
                                node = node,
                                expandedDepartments = expandedDepartments,
                                depth = 0,
                                onToggleExpand = { contactsViewModel.toggleDepartmentExpanded(it) },
                                onUserClick = { userId ->
                                    contactsViewModel.preCreateSessionAndNavigate(userId) { conversationId ->
                                        Napier.d("Conversation ready: $conversationId")
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    when (val state = sessionCreationState) {
        is ContactsViewModel.SessionCreationState.Creating -> SessionCreationDialog(friendId = state.friendId)
        is ContactsViewModel.SessionCreationState.Error -> SessionCreationErrorDialog(
            errorMessage = state.message,
            onDismiss = { contactsViewModel.resetSessionCreationState() }
        )
        else -> Unit
    }
}

@Composable
private fun OrganizationNode(
    node: OrgTreeNode,
    expandedDepartments: Set<Long>,
    depth: Int,
    onToggleExpand: (Long) -> Unit,
    onUserClick: (Long) -> Unit
) {
    when (node.type) {
        OrgTreeNode.NodeType.DEPARTMENT -> {
            val expanded = expandedDepartments.contains(node.id)
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = if (depth == 0) Color(0xFFF8FAFC) else Color.Transparent,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            ) {
                Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onToggleExpand(node.id) }
                            .padding(start = (depth * 14).dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (expanded) Icons.Default.ExpandMore else Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = ThemeTokens.TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .background(ThemeTokens.PrimaryBlue.copy(alpha = 0.12f), RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Business,
                                contentDescription = null,
                                tint = ThemeTokens.PrimaryBlue,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(node.name, fontWeight = FontWeight.SemiBold, color = ThemeTokens.TextMain)
                            node.departmentInfo?.description?.takeIf { it.isNotBlank() }?.let {
                                Text(it, style = MaterialTheme.typography.bodySmall, color = ThemeTokens.TextSecondary)
                            }
                        }
                    }

                    if (expanded) {
                        node.children.forEach { child ->
                            OrganizationNode(
                                node = child,
                                expandedDepartments = expandedDepartments,
                                depth = depth + 1,
                                onToggleExpand = onToggleExpand,
                                onUserClick = onUserClick
                            )
                        }
                    }
                }
            }
        }

        OrgTreeNode.NodeType.USER -> {
            Box(modifier = Modifier.padding(start = ((depth + 1) * 14).dp, top = 6.dp)) {
                UserItem(userInfo = node.userInfo!!, onClick = { onUserClick(node.userInfo.userId) })
            }
        }
    }
}

private fun filterNodes(nodes: List<OrgTreeNode>, query: String): List<OrgTreeNode> {
    if (query.isBlank()) return nodes
    val keyword = query.trim().lowercase()
    return nodes.mapNotNull { node ->
        when (node.type) {
            OrgTreeNode.NodeType.USER -> {
                if (node.name.lowercase().contains(keyword)) node else null
            }

            OrgTreeNode.NodeType.DEPARTMENT -> {
                val filteredChildren = filterNodes(node.children, query)
                if (node.name.lowercase().contains(keyword) || filteredChildren.isNotEmpty()) {
                    node.copy(children = filteredChildren)
                } else {
                    null
                }
            }
        }
    }
}

@Composable
private fun SessionCreationDialog(friendId: Long) {
    AlertDialog(
        onDismissRequest = {},
        title = { Text("创建会话") },
        text = { Text("正在为你和用户 $friendId 创建聊天会话…") },
        confirmButton = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
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
