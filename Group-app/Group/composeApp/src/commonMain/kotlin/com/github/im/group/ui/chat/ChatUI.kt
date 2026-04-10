package com.github.im.group.ui.chat

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
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavHostController
import com.github.im.group.api.FriendshipDTO
import com.github.im.group.model.UserInfo
import com.github.im.group.ui.UserAvatar
import com.github.im.group.ui.conversation
import com.github.im.group.ui.theme.ThemeTokens
import com.github.im.group.viewmodel.ChatViewModel
import com.github.im.group.viewmodel.ConversationDisplayState
import com.github.im.group.viewmodel.UserViewModel
import io.github.aakira.napier.Napier
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ChatUI(
    navHostController: NavHostController,
) {
    val chatViewModel: ChatViewModel = koinViewModel()
    val userViewModel: UserViewModel = koinViewModel()

    val conversations by chatViewModel.conversationState.collectAsState()
    var userSearchQuery by remember { mutableStateOf("") }
    val searchResults by userViewModel.searchResults.collectAsState()
    val userInfo by userViewModel.currentLocalUserInfo.collectAsState()
    val loading by chatViewModel.loading.collectAsState()
    val friends by userViewModel.friends.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    var showCreateGroupDialog by remember { mutableStateOf(false) }

    DisposableEffect(lifecycleOwner, userInfo?.userId) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                userInfo?.userId?.let { chatViewModel.refreshUnreadCounts(it) }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(userInfo?.userId) {
        Napier.d { "当前的用户为 ： $userInfo" }
        userInfo?.userId?.let { userId ->
            // 只有当当前列表为空或者需要强制刷新时才触发加载
            if (conversations.isEmpty()) {
                chatViewModel.getConversations(userId)
            }
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
//                text = "正在聊天",
//                style = MaterialTheme.typography.headlineMedium,
//                color = Color.White,
//                fontWeight = FontWeight.Bold
//            )
//        }

        // --- 离线状态提示 ---
        if (loading && conversations.isEmpty()) {
            OfflineStatusBanner()
        }

        Surface(
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
            color = Color.White.copy(alpha = 0.98f),
            modifier = Modifier.fillMaxSize()
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 20.dp)) {
                // --- 搜索框区域 ---
                OutlinedTextField(
                    value = userSearchQuery,
                    onValueChange = {
                        userSearchQuery = it
                        userViewModel.searchUser(it)
                    },
                    placeholder = { Text("搜索联系人或消息...", fontSize = 14.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = ThemeTokens.TextMuted, modifier = Modifier.size(20.dp)) },
                    trailingIcon = {
                        IconButton(
                            onClick = {
                                userViewModel.loadFriendsIfNeeded()
                                showCreateGroupDialog = true
                            }
                        ) {
                            Icon(Icons.Default.GroupAdd, contentDescription = "创建群聊", tint = ThemeTokens.PrimaryBlue)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = Color(0xFFF1F5F9),
                        focusedContainerColor = Color(0xFFF1F5F9),
                        unfocusedBorderColor = Color.Transparent,
                        focusedBorderColor = ThemeTokens.PrimaryBlue.copy(alpha = 0.4f)
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(20.dp))

                if (userSearchQuery.isNotBlank()) {
                    // --- 搜索结果列表 ---
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(searchResults) { user ->
                            UserSearchItem(
                                user = user,
                                currentUser = userInfo,
                                onAddFriend = { /* logic */ }
                            )
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 72.dp), thickness = 0.5.dp, color = Color(0xFFF1F5F9))
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(conversations) { conversation ->
                            userInfo?.let { me ->
                                ChatItem(
                                    conversation = conversation,
                                    userInfo = me,
                                    onClick = {
                                        val conversationId = conversation.conversation.conversationId
                                        navHostController.navigate(conversation(conversationId))
                                    }
                                )
                                HorizontalDivider(
                                    modifier = Modifier.padding(start = 76.dp, end = 16.dp),
                                    thickness = 0.5.dp,
                                    color = Color(0xFFF1F5F9)
                                )
                            }
                        }

                        // 如果没有会话且不在加载中，显示空状态
                        if (conversations.isEmpty() && !loading) {
                            item {
                                EmptyChatState()
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OfflineStatusBanner() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.CloudOff,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "网络连接异常，正在加载本地数据...",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

@Composable
private fun EmptyChatState() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "暂无聊天会话",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "您可以搜索联系人开始聊天",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// 保留原有的组件定义
@Composable
fun ChatItem(conversation: ConversationDisplayState, userInfo: UserInfo, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        UserAvatar(username = conversation.conversation.getName(userInfo), size = 52)
        
        Spacer(modifier = Modifier.width(14.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = conversation.conversation.getName(userInfo),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = conversation.displayDateTime,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
            
            Spacer(modifier = Modifier.height(2.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = conversation.lastMessage.takeIf { it.isNotEmpty() } ?: "暂无消息",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                if (conversation.unreadCount > 0) {
                    Box(
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .size(20.dp)
                            .background(Color(0xFFEF4444), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (conversation.unreadCount > 99) "99+" else conversation.unreadCount.toString(),
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun UserSearchItem(user: UserInfo, currentUser: UserInfo?, onAddFriend: (Long) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        UserAvatar(username = user.username, size = 48)
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(user.username, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(user.email, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        }
        if (currentUser?.userId != user.userId) {
            Button(
                onClick = { onAddFriend(user.userId) },
                contentPadding = PaddingValues(horizontal = 16.dp),
                modifier = Modifier.height(32.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("添加", fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun CreateGroupDialog(
    friends: List<FriendshipDTO>,
    onDismiss: () -> Unit,
    onCreate: (groupName: String, desc: String?, members: List<UserInfo>) -> Unit
) {
    var groupName by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    val selected = remember { mutableStateOf(setOf<Long>()) }

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 4.dp,
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("创建群聊", style = MaterialTheme.typography.titleLarge)

                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = groupName,
                    onValueChange = { groupName = it },
                    label = { Text("群名称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("描述（可选）") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(12.dp))

                Text("选择成员（至少 2 人）", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))

                val selectableFriends = friends.mapNotNull { it.friendUserInfo }
                LazyColumn(modifier = Modifier.fillMaxWidth().height(260.dp)) {
                    items(selectableFriends, key = { it.userId }) { user ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val next = selected.value.toMutableSet()
                                    if (!next.add(user.userId)) next.remove(user.userId)
                                    selected.value = next
                                }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = selected.value.contains(user.userId),
                                onCheckedChange = { checked ->
                                    val next = selected.value.toMutableSet()
                                    if (checked) next.add(user.userId) else next.remove(user.userId)
                                    selected.value = next
                                }
                            )
                            Spacer(Modifier.width(10.dp))
                            UserAvatar(username = user.username, size = 36)
                            Spacer(Modifier.width(10.dp))
                            Text(user.username, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                    }
                }

                Spacer(Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    androidx.compose.material3.TextButton(onClick = onDismiss) { Text("取消") }
                    Spacer(Modifier.width(8.dp))
                    val canCreate = groupName.isNotBlank() && selected.value.size >= 2
                    Button(
                        onClick = {
                            val members = selectableFriends.filter { selected.value.contains(it.userId) }
                            onCreate(groupName.trim(), desc.trim().takeIf { it.isNotEmpty() }, members)
                        },
                        enabled = canCreate
                    ) {
                        Text("创建")
                    }
                }
            }
        }
    }
}
