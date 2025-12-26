package com.github.im.group.ui

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
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController

import com.github.im.group.model.UserInfo
import io.github.aakira.napier.Napier

@Composable
fun SearchScreen(
    navHostController: NavHostController
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var searchError by remember { mutableStateOf<String?>(null) }
    
    // 标签页数据
    val tabs = listOf("全部", "信息", "群组", "用户", "超链接", "视频")
    
    // 模拟搜索结果数据
    var userResults by remember { mutableStateOf<List<UserInfo>>(emptyList()) }
    var chatResults by remember { mutableStateOf<List<String>>(emptyList()) }
    var groupResults by remember { mutableStateOf<List<String>>(emptyList()) }
    var linkResults by remember { mutableStateOf<List<String>>(emptyList()) }
    var videoResults by remember { mutableStateOf<List<String>>(emptyList()) }

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
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "返回"
                )
            }
            
            // 搜索输入框
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { query ->
                    searchQuery = query
                    searchError = null // 清除之前的错误
                    // 模拟搜索逻辑
                    if (query.isNotEmpty()) {
                        try {
                            // 这里应该调用实际的搜索API
                            userResults = listOf(
                                UserInfo(userId = 1, username = "张三", email = "zhangsan@example.com"),
                                UserInfo(userId = 2, username = "李四", email = "lisi@example.com")
                            )
                            chatResults = listOf("聊天记录1", "聊天记录2")
                            groupResults = listOf("群组1", "群组2")
                            linkResults = listOf("链接1", "链接2")
                            videoResults = listOf("视频1", "视频2")
                        } catch (e: Exception) {
                            Napier.e("搜索过程中发生错误", e)
                            searchError = "搜索失败: ${e.message}"
                            // 清空结果
                            userResults = emptyList()
                            chatResults = emptyList()
                            groupResults = emptyList()
                            linkResults = emptyList()
                            videoResults = emptyList()
                        }
                    } else {
                        userResults = emptyList()
                        chatResults = emptyList()
                        groupResults = emptyList()
                        linkResults = emptyList()
                        videoResults = emptyList()
                    }
                },
                label = { Text("搜索") },
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp),
                isError = searchError != null
            )
        }
        
        // 显示搜索错误信息
        searchError?.let { error ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                Text(
                    text = error,
                    color = androidx.compose.ui.graphics.Color.Red
                )
            }
        }
        
        // 标签页
        ScrollableTabRow(
            selectedTabIndex = selectedTabIndex,
            modifier = Modifier.fillMaxWidth()
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { 
                        selectedTabIndex = index
                        searchError = null // 切换标签时清除错误
                    },
                    text = { Text(title) }
                )
            }
        }

        // 搜索结果
        LazyColumn(
            modifier = Modifier.weight(1f)
        ) {
            // 如果有错误，不显示结果
            if (searchError == null) {
                when (selectedTabIndex) {
                    0 -> {

                        // 全部标签 - 显示所有类型的结果
                        showAllResults(userResults, chatResults, groupResults, linkResults, videoResults)
                    }
                    1 -> {
                        // 信息标签
                        showChatResults(chatResults)
                    }
                    2 -> {
                        // 群组标签
                        showGroupResults(groupResults)
                    }
                    3 -> {
                        // 用户标签
                        showUserResults(userResults)
                    }
                    4 -> {
                        // 超链接标签
                        showLinkResults(linkResults)
                    }
                    5 -> {
                        // 视频标签
                        showVideoResults(videoResults)
                    }
                }
                
                // 没有搜索结果时显示提示
                if (searchQuery.isNotEmpty() && 
                    ((selectedTabIndex == 0 && userResults.isEmpty() && chatResults.isEmpty() && groupResults.isEmpty() && linkResults.isEmpty() && videoResults.isEmpty()) ||
                    (selectedTabIndex == 1 && chatResults.isEmpty()) ||
                    (selectedTabIndex == 2 && groupResults.isEmpty()) ||
                    (selectedTabIndex == 3 && userResults.isEmpty()) ||
                    (selectedTabIndex == 4 && linkResults.isEmpty()) ||
                    (selectedTabIndex == 5 && videoResults.isEmpty()))) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("没有找到相关内容")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun UserSearchResultItem(
    userInfo: UserInfo,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        UserAvatar(username = userInfo.username, size = 56)
        Spacer(Modifier.width(12.dp))
        Column {
            Text(
                text = userInfo.username,
                style = androidx.compose.material3.MaterialTheme.typography.titleMedium
            )
            Text(
                text = userInfo.email,
                style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                color = androidx.compose.ui.graphics.Color.Gray
            )
        }
    }
}

//@Composable
private fun LazyListScope.showAllResults(
    userResults: List<UserInfo>,
    chatResults: List<String>,
    groupResults: List<String>,
    linkResults: List<String>,
    videoResults: List<String>
) {
    try {
        // 用户搜索结果
        if (userResults.isNotEmpty()) {
            item {
                Text(
                    text = "用户",
                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp)
                )
            }

            items(userResults) { user ->
                UserItem(
                    userInfo = user,
                    onClick = {
                        // 导航到与该用户的聊天界面
                        // navHostController.navigate(ChatRoom(conversationId))
                    }
                )
            }
        }

        // 聊天记录搜索结果
        if (chatResults.isNotEmpty()) {
            item {
                Text(
                    text = "聊天记录",
                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp)
                )
            }

            items(chatResults, key = { chat -> chat.hashCode() }) { chat ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { /* TODO: 导航到具体的聊天记录 */ }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Message,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(text = chat)
                }
            }
        }

        // 群组搜索结果
        if (groupResults.isNotEmpty()) {
            item {
                Text(
                    text = "群组",
                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp)
                )
            }

            items(groupResults, key = { group -> group.hashCode() }) { group ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { /* TODO: 导航到群组 */ }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Group,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(text = group)
                }
            }
        }

        // 超链接搜索结果
        if (linkResults.isNotEmpty()) {
            item {
                Text(
                    text = "超链接",
                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp)
                )
            }

            items(linkResults, key = { link -> link.hashCode() }) { link ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { /* TODO: 打开链接 */ }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Link,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(text = link)
                }
            }
        }

        // 视频搜索结果
        if (videoResults.isNotEmpty()) {
            item {
                Text(
                    text = "视频",
                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp)
                )
            }

            items(videoResults, key = { video -> video.hashCode() }) { video ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { /* TODO: 播放视频 */ }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.VideoLibrary,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(text = video)
                }
            }
        }
    } catch (e: Exception) {
        Napier.e("显示搜索结果时发生错误", e)
        item {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text("显示结果时发生错误: ${e.message}")
            }
        }
    }
}

//@Composable
private fun LazyListScope.showUserResults(userResults: List<UserInfo>) {
    try {
        items(userResults, key = { user -> user.userId }) { user ->
            UserItem(
                userInfo = user,
                onClick = {
                    // 导航到与该用户的聊天界面
                    // navHostController.navigate(ChatRoom(conversationId))
                }
            )
        }
    } catch (e: Exception) {
        Napier.e("显示用户搜索结果时发生错误", e)
        item {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text("显示用户结果时发生错误: ${e.message}")
            }
        }
    }
}

private fun LazyListScope.showChatResults(chatResults: List<String>) {
    try {
        items(chatResults, key = { chat -> chat.hashCode() }) { chat ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { /* TODO: 导航到具体的聊天记录 */ }
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Message,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(12.dp))
                Text(text = chat)
            }
        }
    } catch (e: Exception) {
        Napier.e("显示聊天记录搜索结果时发生错误", e)
        item {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text("显示聊天记录结果时发生错误: ${e.message}")
            }
        }
    }
}

private fun LazyListScope.showGroupResults(groupResults: List<String>) {
    try {
        items(groupResults, key = { group -> group.hashCode() }) { group ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { /* TODO: 导航到群组 */ }
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Group,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(12.dp))
                Text(text = group)
            }
        }
    } catch (e: Exception) {
        Napier.e("显示群组搜索结果时发生错误", e)
        item {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text("显示群组结果时发生错误: ${e.message}")
            }
        }
    }
}

//@Composable
private fun LazyListScope.showLinkResults(linkResults: List<String>) {
    try {
        items(linkResults, key = { link -> link.hashCode() }) { link ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { /* TODO: 打开链接 */ }
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Link,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(12.dp))
                Text(text = link)
            }
        }
    } catch (e: Exception) {
        Napier.e("显示链接搜索结果时发生错误", e)
        item {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text("显示链接结果时发生错误: ${e.message}")
            }
        }
    }
}

private fun LazyListScope.showVideoResults(videoResults: List<String>) {
    try {
        items(videoResults, key = { video -> video.hashCode() }) { video ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { /* TODO: 播放视频 */ }
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.VideoLibrary,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(12.dp))
                Text(text = video)
            }
        }
    } catch (e: Exception) {
        Napier.e("显示视频搜索结果时发生错误", e)
        item {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text("显示视频结果时发生错误: ${e.message}")
            }
        }
    }
}