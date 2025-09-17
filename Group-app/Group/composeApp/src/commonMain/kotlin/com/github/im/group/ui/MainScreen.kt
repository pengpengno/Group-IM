@file:JvmName("MainScreenKt")

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
import androidx.compose.material.BottomNavigation
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.github.im.group.api.ConversationRes
import com.github.im.group.model.UserInfo
import com.github.im.group.viewmodel.ChatViewModel
import com.github.im.group.viewmodel.UserViewModel
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel
import kotlin.jvm.JvmName

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatMainScreen(
    navHostController: NavHostController,
)  {


        val chatViewModel: ChatViewModel = koinViewModel()
        val userViewModel: UserViewModel = koinViewModel()

        val drawerState = rememberDrawerState(DrawerValue.Closed)

        val scope = rememberCoroutineScope()

        var searchQuery by remember { mutableStateOf("") }

        val loading by chatViewModel.loading.collectAsState()
        val conversations by chatViewModel.uiState.collectAsState()

        val userInfo  = userViewModel.getUser()
        LaunchedEffect(userInfo) {
            if(userInfo?.userId != 0L){
                userInfo?.userId?.let { chatViewModel.getConversations(it) }
            }
        }


        CircularProgressIndicator()

        ModalNavigationDrawer(
            drawerContent = {
//                if (userInfo != null) {
//                    SideDrawer(userInfo, {println("logout")})
//                }
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
//                        .width(LocalConfiguration.current.screenWidthDp.dp * 0.75f) // 3/4 屏幕宽度
                ) {
                    if (userInfo != null) {
//                        SideDrawer(userInfo, { println("logout") })
                        SideDrawer(
                            userInfo = userInfo,
                            onLogout = { println("logout") },
                            onProfileClick = { navHostController.navigate("Profile") },
                            onContactsClick = { navHostController.navigate("Contacts") },
                            onGroupsClick = { navHostController.navigate("Groups") },
                            onMeetingsClick = { navHostController.navigate("Meetings") },
                            onSettingsClick = { navHostController.navigate("Settings") },
                            appVersion = "v1.2.3"
                        )

                    }
                }
            },
            drawerState = drawerState
        )
        {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(text = "Conversation", color = Color.White)
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
                },
                bottomBar = {
                    BottomNavigation {
                        val navBackStackEntry by navHostController.currentBackStackEntryAsState()
                        val currentDestination = navBackStackEntry?.destination
                    }

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

                        conversations.forEach { conversation ->
                            if (userInfo != null) {
                                ChatItem(conversation = conversation,userInfo = userInfo,
                                    onClick = {
                                        navHostController.navigate(ChatRoom(conversation.conversationId))
//                                        onFriendClick(conversation)

                                    })
                            }
                        }
                    }

//                    // 右侧面板：聊天预览
//                    Box(
//                        modifier = Modifier
//                            .weight(2f)
//                            .fillMaxHeight()
//                            .background(Color.White),
//                        contentAlignment = Alignment.Center
//                    ) {
//                        Text(
//                            text = "请选择一个会话开始聊天",
//                            color = Color.Gray
//                        )
//                    }
                }
            }
        }
}

@Composable
fun ChatItem(conversation: ConversationRes, userInfo: UserInfo,onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(12.dp)
    ) {
        Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFF0088CC))
        Spacer(Modifier.width(12.dp))
        Column {
            Text(conversation.getName(userInfo), style = MaterialTheme.typography.titleMedium)
            Text(
//                text = friend.lastMessage.takeIf { e-> e?.isEmpty() ?: false } ?: "暂无消息",
                text = conversation.lastMessage.takeIf { e-> e.isEmpty() ?: false } ?: "暂无消息",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
        }

    }
}
