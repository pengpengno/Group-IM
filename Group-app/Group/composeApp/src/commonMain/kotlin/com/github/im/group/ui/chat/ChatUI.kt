package com.github.im.group.ui.chat

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
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.github.im.group.model.UserInfo
import com.github.im.group.ui.UserAvatar
import com.github.im.group.ui.conversation
import com.github.im.group.viewmodel.ChatViewModel
import com.github.im.group.viewmodel.ConversationDisplayState
import com.github.im.group.viewmodel.UserViewModel
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

    LaunchedEffect(userInfo) {
        userInfo?.userId?.let {
            chatViewModel.getConversations(it)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // --- 搜索框区域 ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = userSearchQuery,
                onValueChange = {
                    userSearchQuery = it
                    userViewModel.searchUser(it)
                },
                placeholder = { Text("搜索联系人或消息...", fontSize = 14.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(20.dp)) },
                modifier = Modifier
                    .fillMaxWidth()
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
        }

        if (userSearchQuery.isNotBlank()) {
            // --- 搜索结果列表 ---
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(searchResults) { user ->
                    UserSearchItem(
                        user = user,
                        currentUser = userInfo,
                        onAddFriend = { /* logic */ }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 72.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        } else {
            // --- 聊天会话列表 ---
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            ) {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    item { Spacer(modifier = Modifier.height(8.dp)) }
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
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                            )
                        }
                    }
                }
            }
        }
    }
}

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
            
            Text(
                text = conversation.lastMessage.takeIf { it.isNotEmpty() } ?: "暂无消息",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
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
