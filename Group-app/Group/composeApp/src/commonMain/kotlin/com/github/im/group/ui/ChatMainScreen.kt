package com.github.im.group.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.Divider
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import com.github.im.group.model.Friend
import com.github.im.group.model.UserInfo
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
class ChatMainScreen(
    private val userInfo: UserInfo,
    private val friends: List<Friend>,
    private val onFriendClick: (Friend) -> Unit,
    private val onLogout: () -> Unit
) : Screen {

    @Composable
    override fun Content() {
        val drawerState = rememberDrawerState(DrawerValue.Closed)
        val scope = rememberCoroutineScope()
        var searchQuery by remember { mutableStateOf("") }

        ModalNavigationDrawer(
            drawerContent = {
                SideDrawer(userInfo, onLogout)
            },
            drawerState = drawerState
        ) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(text = "Telegram", color = Color.White)
                        },
                        navigationIcon = {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(Icons.Default.Menu, contentDescription = "菜单", tint = Color.White)
                            }
                        },
                        actions = {
                            IconButton(onClick = {
                                // 切换搜索框显示
                                searchQuery = if (searchQuery.isEmpty()) " " else ""
                            }) {
                                Icon(Icons.Default.Search, contentDescription = "搜索", tint = Color.White)
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color(0xFF0088CC),
                            titleContentColor = Color.White,
                            navigationIconContentColor = Color.White,
                            actionIconContentColor = Color.White
                        )
                    )
                }
            ) { padding ->
                Row(
                    Modifier
                        .padding(padding)
                        .fillMaxSize()
                ) {
                    // 左侧面板：会话列表
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(Color(0xFFF6F6F6))
                            .padding(8.dp)
                    ) {
                        if (searchQuery.isNotBlank()) {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                label = { Text("搜索会话") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp)
                            )
                        }

                        friends.forEach { friend ->
                            ChatItem(friend = friend, onClick = { onFriendClick(friend) })
                        }
                    }

                    // 右侧面板：聊天预览
                    Box(
                        modifier = Modifier
                            .weight(2f)
                            .fillMaxHeight()
                            .background(Color.White),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "请选择一个会话开始聊天",
                            color = Color.Gray
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ChatItem(friend: Friend, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(12.dp)
    ) {
        Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFF0088CC))
        Spacer(Modifier.width(12.dp))
        Column {
            Text(friend.name, style = MaterialTheme.typography.titleMedium)
            Text(
//                text = friend.lastMessage.takeIf { e-> e?.isEmpty() ?: false } ?: "暂无消息",
                text = friend.lastMessage.takeIf { e->e?.isEmpty() ?: false } ?: "暂无消息",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
        }
    }
}

@Composable
fun SideDrawer(userInfo: UserInfo, onLogout: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(16.dp)
    ) {
        Text("用户：${userInfo.username}", style = MaterialTheme.typography.titleLarge)
        Divider(modifier = Modifier.padding(vertical = 12.dp))

        Text("联系人", modifier = Modifier.clickable { })
        Divider(modifier = Modifier.padding(vertical = 8.dp))

        Text("设置", modifier = Modifier.clickable { })
        Divider(modifier = Modifier.padding(vertical = 8.dp))

        Text("退出登录", modifier = Modifier.clickable(onClick = onLogout))
    }
}