//@file:JvmName("MainScreenKt")

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.BottomNavigation
import androidx.compose.material.BottomNavigationItem
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Contacts
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.github.im.group.api.ConversationRes
import com.github.im.group.model.UserInfo
import com.github.im.group.viewmodel.ChatViewModel
import com.github.im.group.viewmodel.UserViewModel
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

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
    
    // 底部导航栏选中状态
    var selectedItem by remember { mutableIntStateOf(0) }
    
    val bottomNavItems = listOf(
        BottomNavItem("聊天", Icons.AutoMirrored.Filled.Chat),
        BottomNavItem("联系人", Icons.Default.Contacts),
        BottomNavItem("收藏", Icons.Default.Person)
    )

    LaunchedEffect(userInfo) {
        if(userInfo?.userId != 0L){
            userInfo?.userId?.let { chatViewModel.getConversations(it) }
        }
    }

    ModalNavigationDrawer(
        drawerContent = {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
            ) {
                if (userInfo != null) {
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
    ) { 
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(text = "首页", color = Color.White)
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
                BottomNavigation(
                    backgroundColor = Color(0xFF0088CC),
                    contentColor = Color.White
                ) {
                    bottomNavItems.forEachIndexed { index, item ->
                        BottomNavigationItem(
                            icon = {
                                Icon(
                                    item.icon,
                                    contentDescription = item.title,
                                    modifier = Modifier.size(24.dp)
                                )
                            },
                            label = { 
                                Text(
                                    text = item.title,
                                    style = MaterialTheme.typography.bodySmall
                                ) 
                            },
                            selected = selectedItem == index,
                            onClick = { selectedItem = index },
                            selectedContentColor = Color.White,
                            unselectedContentColor = Color(0x99FFFFFF) // 半透明白色
                        )
                    }
                }
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .background(Color(0xFFF0F0F0))
            ) {

                Column(
                    modifier = Modifier
                        .fillMaxHeight()
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
                            ChatItem(
                                conversation = conversation,
                                userInfo = userInfo,
                                onClick = {
                                    navHostController.navigate(ChatRoom(conversation.conversationId))
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChatItem(conversation: ConversationRes, userInfo: UserInfo, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(12.dp)
    ) {
        UserAvatar(username = conversation.getName(userInfo), size = 56)
        Spacer(Modifier.width(12.dp))
        Column {
            Text(
                conversation.getName(userInfo), 
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = conversation.lastMessage.takeIf { e-> e.isNotEmpty() } ?: "暂无消息",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
        }
    }
}

// 底部导航项数据类
data class BottomNavItem(val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)