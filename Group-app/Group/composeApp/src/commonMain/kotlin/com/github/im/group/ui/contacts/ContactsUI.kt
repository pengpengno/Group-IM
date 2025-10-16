package com.github.im.group.ui.contacts

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
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
import com.github.im.group.viewmodel.UserViewModel
import com.github.im.group.ui.UserAvatar
import com.github.im.group.model.UserInfo
import com.github.im.group.viewmodel.ChatViewModel
import com.github.im.group.ui.ChatRoom
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel
import io.github.aakira.napier.Napier

/**
 * 联系人面板
 */
@Composable
fun ContactsUI (
    navHostController: NavHostController,
){

    val userViewModel: UserViewModel = koinViewModel()
    val chatViewModel: ChatViewModel = koinViewModel()
    val friends by userViewModel.friends.collectAsState()
    val scope = rememberCoroutineScope()
    var contactSearchQuery by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        userViewModel.loadFriends()
        Napier.d("加载联系人列表")
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp)
    ) {
        // 联系人搜索框
        OutlinedTextField(
            value = contactSearchQuery,
            onValueChange = { contactSearchQuery = it },
            label = { Text("搜索联系人") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        )
        
        // 按字母排序的联系人列表
        val filteredAndGroupedFriends = friends
            .mapNotNull { it.friendUserInfo }
            .filter { friend ->
                contactSearchQuery.isBlank() || 
                friend.username.contains(contactSearchQuery, ignoreCase = true) ||
                friend.email.contains(contactSearchQuery, ignoreCase = true)
            }
            .sortedBy { it.username }
            .groupBy { it.username.firstOrNull()?.uppercaseChar() ?: '#' }
            .toSortedMap()
        
        if (filteredAndGroupedFriends.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                if (friends.isEmpty()) {
                    CircularProgressIndicator()
                } else {
                    Text("没有找到联系人")
                }
            }
        } else {
            LazyColumn {
                filteredAndGroupedFriends.forEach { (initial, contacts) ->
                    // 字母标题
                    item {
                        Text(
                            text = initial.toString(),
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFE0E0E0))
                                .padding(8.dp)
                        )
                    }
                    
                    // 该字母下的联系人
                    items(contacts) { friend ->
                        ContactItem(
                            friend = friend,
                            onClick = {
                                // 直接处理联系人点击事件
                                Napier.d("用户点击联系人: ${friend.username} (ID: ${friend.userId})")
                                
                                scope.launch {
                                    try {
                                        val conversation = chatViewModel.getPrivateChat(friend.userId)
                                        Napier.d("成功获取会话: ${conversation.conversationId}")
                                        navHostController.navigate(ChatRoom(conversation.conversationId))
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
fun ContactItem(
    friend: UserInfo,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        UserAvatar(username = friend.username, size = 56)
        Spacer(Modifier.width(12.dp))
        Column {
            Text(
                text = friend.username,
                style = androidx.compose.material3.MaterialTheme.typography.titleMedium
            )
            Text(
                text = friend.email,
                style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                color = androidx.compose.ui.graphics.Color.Gray
            )
        }
    }
}