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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.github.im.group.viewmodel.UserViewModel
import com.github.im.group.model.UserInfo
import com.github.im.group.viewmodel.ChatViewModel
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel
import io.github.aakira.napier.Napier
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.graphics.Color
import com.github.im.group.ui.ChatRoom
import com.github.im.group.ui.UserAvatar

@Composable
fun AddFriendScreen(
    navHostController: NavHostController
) {
    val userViewModel: UserViewModel = koinViewModel()
    val chatViewModel: ChatViewModel = koinViewModel()
    val scope = rememberCoroutineScope()
    
    var searchQuery by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }
    var showUserDetails by remember { mutableStateOf(false) }
    var selectedUser by remember { mutableStateOf<UserInfo?>(null) }
    var isFriend by remember { mutableStateOf(false) }
    
    val searchResults by userViewModel.searchResults.collectAsState()
    val friends by userViewModel.friends.collectAsState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        // 顶部工具栏
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { navHostController.popBackStack() }) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "返回"
                )
            }
            
            // 搜索输入框
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { query ->
                    searchQuery = query
                    if (query.isNotEmpty()) {
                        isSearching = true
                        userViewModel.searchUser(query)
                    } else {
                        isSearching = false
                    }
                },
                label = { Text("搜索用户") },
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp)
            )
        }
        
        // 搜索结果
        if (isSearching) {
            if (searchResults.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSearching) {
                        CircularProgressIndicator()
                    } else {
                        Text("请输入用户名或邮箱搜索用户")
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f)
                ) {
                    items(searchResults) { user ->
                        // 检查是否已经是好友
                        val isFriend = friends.any { it.friendUserInfo?.userId == user.userId }
                        
                        AddFriendItem(
                            user = user,
                            isFriend = isFriend,
                            onItemClick = {
                                selectedUser = user
                                showUserDetails = true
                            },
                            onActionClick = { 
                                if (isFriend) {
                                    // 导航到聊天界面
                                    scope.launch {
                                        try {
                                            val conversation = chatViewModel.getPrivateChat(user.userId)
                                            navHostController.navigate(ChatRoom(conversation.conversationId))
                                        } catch (e: Exception) {
                                            Napier.e("创建或获取会话失败", e)
                                        }
                                    }
                                } else {
                                    // 添加好友
                                    userViewModel.addFriend(
                                        userId = userViewModel.getCurrentUser().userId,
                                        friendId = user.userId
                                    )
                                }
                            }
                        )
                    }
                }
            }
        } else {
            // 默认提示信息
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text("请输入用户名或邮箱搜索用户")
            }
        }
    }
    
    // 用户详情对话框
    if (showUserDetails && selectedUser != null) {
        UserDetailsDialog(
            user = selectedUser!!,
            isFriend = friends.any { it.friendUserInfo?.userId == selectedUser!!.userId },
            onDismiss = { showUserDetails = false },
            onActionClick = {
                if (friends.any { it.friendUserInfo?.userId == selectedUser!!.userId }) {
                    // 导航到聊天界面
                    scope.launch {
                        try {
                            val conversation = chatViewModel.getPrivateChat(selectedUser!!.userId)
                            showUserDetails = false
                            navHostController.navigate(ChatRoom(conversation.conversationId))
                        } catch (e: Exception) {
                            Napier.e("创建或获取会话失败", e)
                        }
                    }
                } else {
                    // 添加好友
                    userViewModel.addFriend(
                        userId = userViewModel.getCurrentUser().userId,
                        friendId = selectedUser!!.userId
                    )
                    showUserDetails = false
                }
            }
        )
    }
}

@Composable
fun AddFriendItem(
    user: UserInfo,
    isFriend: Boolean,
    onItemClick: () -> Unit,
    onActionClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onItemClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        UserAvatar(username = user.username, size = 56)
        Spacer(Modifier.width(12.dp))
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = user.username,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = user.email,
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
        }
        IconButton(
            onClick = onActionClick
        ) {
            Icon(
                imageVector = if (isFriend) Icons.Default.Message else Icons.Default.PersonAdd,
                contentDescription = if (isFriend) "发送消息" else "添加好友",
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
fun UserDetailsDialog(
    user: UserInfo,
    isFriend: Boolean,
    onDismiss: () -> Unit,
    onActionClick: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = user.username) },
        text = {
            Column {
                UserAvatar(
                    username = user.username,
                    size = 80,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
                Spacer(Modifier.size(16.dp))
                Text(text = "用户ID: ${user.userId}")
                Text(text = "邮箱: ${user.email}")
            }
        },
        confirmButton = {
            TextButton(onClick = onActionClick) {
                Text(if (isFriend) "发送消息" else "添加好友")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        }
    )
}