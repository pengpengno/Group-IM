package com.github.im.group.ui.chat

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.github.im.group.model.UserInfo
import com.github.im.group.ui.ChatRoom
import com.github.im.group.ui.UserAvatar
import com.github.im.group.viewmodel.ChatViewModel
import com.github.im.group.viewmodel.ConversationDisplayState
import com.github.im.group.viewmodel.UserViewModel
import org.koin.compose.viewmodel.koinViewModel

/**
 * 聊天消息
 */
@Composable
fun ChatUI(
    navHostController: NavHostController,
) {
    val chatViewModel: ChatViewModel = koinViewModel()
    val userViewModel: UserViewModel = koinViewModel()

    // 会话列表
    val conversations by chatViewModel.conversationState.collectAsState()
    val loginState by userViewModel.loginState.collectAsState()
    val userInfo  by  remember {mutableStateOf(userViewModel.getCurrentUser()) }
    var userSearchQuery by remember { mutableStateOf("") }
    val searchResults by userViewModel.searchResults.collectAsState()

    LaunchedEffect(userInfo) {

        if (userInfo?.userId != 0L) {
            userInfo?.userId?.let {
                // 获取用户的所有会话消息
                chatViewModel.getConversations(it) }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp)
    ) {
        if (userSearchQuery.isNotBlank()) {
            OutlinedTextField(
                value = userSearchQuery,
                onValueChange = {
                    userSearchQuery = it
                    userViewModel.searchUser(it)
                },
                label = { Text("搜索用户") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            )

            // 显示搜索结果
            if (userSearchQuery.isNotBlank()) {
                LazyColumn {
                    items(searchResults) { user ->
                        UserSearchItem(
                            user = user,
                            currentUser = userInfo,
                            onAddFriend = { friendId ->
                                userInfo?.userId?.let { userId ->
//                                    userViewModel.addFriend(userId, friendId)
                                }
                            }
                        )
                    }
                }
            }
        } else {
            // 显示会话列表
            conversations.forEach { conversation ->
                ChatItem(
                    conversation = conversation,
                    userInfo = userInfo,
                    onClick = {
                        navHostController.navigate(ChatRoom.Conversation(conversation.conversation.conversationId))
                    }
                )
            }
        }
    }
}

@Composable
fun ChatItem(conversation: ConversationDisplayState, userInfo: UserInfo, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(12.dp)
    ) {
        UserAvatar(username = conversation.conversation.getName(userInfo), size = 56)
        Spacer(Modifier.width(12.dp))
        Column {
            // 展示用户
            Text(
                conversation.conversation.getName(userInfo),
                style = androidx.compose.material3.MaterialTheme.typography.titleMedium
            )
            //展示消息
            Text(
                text = conversation.lastMessage.takeIf { e -> e.isNotEmpty() } ?: "暂无消息",
                style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                color = androidx.compose.ui.graphics.Color.Gray
            )
            //   展示时间
            Text(
                text = conversation.displayDateTime,
                style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                color = androidx.compose.ui.graphics.Color.Gray
            )
        }
    }
}

@Composable
fun UserSearchItem(
    user: UserInfo,
    currentUser: UserInfo?,
    onAddFriend: (Long) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
    ) {
        UserAvatar(username = user.username, size = 56)
        Spacer(Modifier.width(12.dp))
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = user.username,
                style = androidx.compose.material3.MaterialTheme.typography.titleMedium
            )
            Text(
                text = user.email,
                style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                color = androidx.compose.ui.graphics.Color.Gray
            )
        }
        if (currentUser?.userId != user.userId) {
            androidx.compose.material3.Button(
                onClick = { onAddFriend(user.userId) },
                modifier = Modifier
                    .padding(4.dp)
            ) {
                Text("添加")
            }
        }
    }
}