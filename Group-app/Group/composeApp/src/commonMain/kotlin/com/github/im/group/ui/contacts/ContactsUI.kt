package com.github.im.group.ui.contacts

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.github.im.group.model.OrgTreeNode
import com.github.im.group.ui.ChatRoom
import com.github.im.group.ui.UserItem
import com.github.im.group.viewmodel.ChatViewModel
import com.github.im.group.viewmodel.ContactsViewModel
import com.github.im.group.viewmodel.UserViewModel
import io.github.aakira.napier.Napier
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

/**
 * 联系人面板 - 显示组织架构
 */
@Composable
fun ContactsUI (
    navHostController: NavHostController,
){
    val contactsViewModel: ContactsViewModel = koinViewModel()
    val userViewModel: UserViewModel = koinViewModel()
    val chatViewModel: ChatViewModel = koinViewModel()
    
    val organizationTree by contactsViewModel.organizationTree.collectAsState()
    val loading by contactsViewModel.loading.collectAsState()
    val expandedDepartments by contactsViewModel.expandedDepartments.collectAsState()
    
    val scope = rememberCoroutineScope()
    var contactSearchQuery by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        // 加载组织架构
        contactsViewModel.getOrganizationStructure()
        Napier.d("加载组织架构")
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp)
    ) {
        // 组织架构搜索框
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
        ) {
            OutlinedTextField(
                value = contactSearchQuery,
                onValueChange = { contactSearchQuery = it },
                label = { Text("搜索组织或人员") },
                modifier = Modifier
                    .weight(1f)
            )
            
            IconButton(
                onClick = { 
                    // 刷新组织架构
                    scope.launch {
                        contactsViewModel.getOrganizationStructure()
                    }
                },
                modifier = Modifier
                    .padding(start = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "刷新"
                )
            }
        }
        
        if (loading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (organizationTree.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("暂无组织架构数据")
            }
        } else {
            LazyColumn {
                organizationTree.forEach { node ->
                    item(key = node.id) {
                        OrganizationNodeItem(
                            node = node,
                            expandedDepartments = expandedDepartments,
                            onToggleExpand = { nodeId ->
                                if (node.type == OrgTreeNode.NodeType.DEPARTMENT) {
                                    contactsViewModel.toggleDepartmentExpanded(nodeId)
                                }
                            },
                            onUserClick = { userInfo ->
                                // 点击用户，创建私聊会话
                                scope.launch {
                                    try {
                                        navHostController.navigate(ChatRoom.CreatePrivate(userInfo.userId))
                                    } catch (e: Exception) {
                                        Napier.e("创建或获取会话失败", e)
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun OrganizationNodeItem(
    node: OrgTreeNode,
    expandedDepartments: Set<Long>,
    onToggleExpand: (Long) -> Unit,
    onUserClick: (com.github.im.group.model.UserInfo) -> Unit
) {
    when (node.type) {
        OrgTreeNode.NodeType.DEPARTMENT -> {
            DepartmentNode(
                node = node,
                expandedDepartments = expandedDepartments,
                onToggleExpand = onToggleExpand
            )
            // 如果部门已展开，显示其子节点
            if (expandedDepartments.contains(node.id)) {
                node.children.forEach { childNode ->
                    Spacer(modifier = Modifier.padding(start = 24.dp))
                    OrganizationNodeItem(
                        node = childNode,
                        expandedDepartments = expandedDepartments,
                        onToggleExpand = onToggleExpand,
                        onUserClick = onUserClick
                    )
                }
            }
        }
        OrgTreeNode.NodeType.USER -> {
            UserNode(
                node = node,
                onUserClick = onUserClick
            )
        }
    }
}

@Composable
fun DepartmentNode(
    node: OrgTreeNode,
    expandedDepartments: Set<Long>,
    onToggleExpand: (Long) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggleExpand(node.id) }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (expandedDepartments.contains(node.id)) {
                Icons.Default.ExpandMore
            } else {
                Icons.Default.ChevronRight
            },
            contentDescription = if (expandedDepartments.contains(node.id)) "收起" else "展开",
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Icon(
            imageVector = Icons.Default.Business,
            contentDescription = "部门",
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = node.name,
                style = MaterialTheme.typography.titleMedium
            )
            node.departmentInfo?.description?.let { description ->
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
        }
    }
}

@Composable
fun UserNode(
    node: OrgTreeNode,
    onUserClick: (com.github.im.group.model.UserInfo) -> Unit
) {
    node.userInfo?.let { userInfo ->
        UserItem(
            userInfo = userInfo,
            onClick = { onUserClick(userInfo) },
            modifier = Modifier
                .padding(start = 40.dp) // 为用户添加额外的缩进
        )
    }
}