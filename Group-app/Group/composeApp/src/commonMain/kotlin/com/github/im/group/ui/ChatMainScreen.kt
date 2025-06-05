package com.github.im.group.ui

import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import cafe.adriel.voyager.core.screen.Screen
import com.github.im.group.model.Friend
import com.github.im.group.model.UserInfo

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

        val filteredChats = if (searchQuery.isBlank()) chats
        else chats.filter { it.title.contains(searchQuery, ignoreCase = true) }

        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                SideDrawer(userInfo, onLogout)
            }
        ) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(text = "Telegram")
                        },
                        navigationIcon = {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(Icons.Default.Menu, contentDescription = "菜单")
                            }
                        },
                        actions = {
                            IconButton(onClick = {
                                // 切换搜索框显示
                                scope.launch {
                                    searchQuery = if (searchQuery.isEmpty()) " " else ""
                                }
                            }) {
                                Icon(Icons.Default.Search, contentDescription = "搜索")
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

                        filteredChats.forEach { chat ->
                            ChatItem(chat = chat, onClick = { onChatClick(chat) })
                        }
                    }

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
fun ChatItem(chat: ChatSummary, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(8.dp)
    ) {
        Icon(Icons.Default.Person, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Column {
            Text(chat.title, style = MaterialTheme.typography.bodyLarge)
            Text(chat.lastMessage, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        }
    }
}


@Composable
fun SideDrawer(userInfo: UserInfo, onLogout: () -> Unit) {
    Column(Modifier.fillMaxSize().background(Color.White).padding(16.dp)) {
        Text("用户：${userInfo.username}", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(16.dp))

        Text("联系人", modifier = Modifier.clickable { })
        Spacer(Modifier.height(8.dp))

        Text("设置", modifier = Modifier.clickable { })
        Spacer(Modifier.height(8.dp))

        Text("退出登录", modifier = Modifier.clickable(onClick = onLogout))
    }
}
