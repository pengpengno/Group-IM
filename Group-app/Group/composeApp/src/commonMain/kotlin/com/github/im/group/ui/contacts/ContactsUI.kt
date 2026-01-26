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
import com.github.im.group.ui.createPrivate
import com.github.im.group.viewmodel.ContactsViewModel
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
    
    val scope = rememberCoroutineScope()
    var contactSearchQuery by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        contactsViewModel.getOrganizationStructure()
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

            Surface(
                onClick = { contactsViewModel.getOrganizationStructure() },
                shape = RoundedCornerShape(26.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.size(52.dp)
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
        
        // --- 内容区域 ---
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            if (loading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(strokeWidth = 3.dp)
                }
            } else if (organizationTree.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Inbox, 
                            contentDescription = null, 
                            modifier = Modifier.size(48.dp), 
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("暂无组织架构数据", color = MaterialTheme.colorScheme.outline)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    items(organizationTree, key = { it.id }) { node ->
                        OrganizationNodeItem(
                            node = node,
                            depth = 0,
                            expandedDepartments = expandedDepartments,
                            onToggleExpand = { nodeId ->
                                contactsViewModel.toggleDepartmentExpanded(nodeId)
                            },
                            onUserClick = { userInfo ->

                                navHostController.navigate(createPrivate(userInfo.userId))
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
    depth: Int,
    expandedDepartments: Set<Long>,
    onToggleExpand: (Long) -> Unit,
    onUserClick: (com.github.im.group.model.UserInfo) -> Unit
) {
    val isExpanded = expandedDepartments.contains(node.id)

    Column {
        when (node.type) {
            OrgTreeNode.NodeType.DEPARTMENT -> {
                DepartmentNode(
                    node = node,
                    depth = depth,
                    isExpanded = isExpanded,
                    onToggleExpand = onToggleExpand
                )
                if (isExpanded) {
                    node.children.forEach { childNode ->
                        OrganizationNodeItem(
                            node = childNode,
                            depth = depth + 1,
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
                    depth = depth,
                    onUserClick = onUserClick
                )
            }
        }
    }
}

@Composable
fun DepartmentNode(
    node: OrgTreeNode,
    depth: Int,
    isExpanded: Boolean,
    onToggleExpand: (Long) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggleExpand(node.id) }
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .padding(start = (depth * 20).dp),
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
                text = node.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            node.departmentInfo?.description?.let { description ->
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
fun UserNode(
    node: OrgTreeNode,
    depth: Int,
    onUserClick: (com.github.im.group.model.UserInfo) -> Unit
) {
    node.userInfo?.let { userInfo ->
        UserItem(
            userInfo = userInfo,
            onClick = { onUserClick(userInfo) },
            modifier = Modifier
                .padding(start = (depth * 20 + 28).dp)
        )
    }
}
