//@file:JvmName("MainScreenKt")

package com.github.im.group.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import com.github.im.group.viewmodel.ChatViewModel
import com.github.im.group.viewmodel.UserViewModel
import com.github.im.group.ui.chat.ChatUI
import com.github.im.group.ui.contacts.ContactsUI
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
    val loading by chatViewModel.loading.collectAsState()
    val friends by userViewModel.friends.collectAsState()

    val userInfo = userViewModel.getCurrentUser()

    
    // 底部导航栏选中状态
    var selectedItem by remember { mutableIntStateOf(0) }
    
    val bottomNavItems = listOf(
        BottomNavItem("聊天", Icons.AutoMirrored.Filled.Chat),
        BottomNavItem("联系人", Icons.Default.Contacts),
        BottomNavItem("我", Icons.Default.Person)
    )

    LaunchedEffect(userInfo) {
        if(userInfo?.userId != 0L){
            userInfo?.userId?.let { chatViewModel.getConversations(it) }
            userViewModel.loadFriends()
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
                        onContactsClick = { /* 不再跳转页面 */ },
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
                            // 导航到搜索页面
                            navHostController.navigate(Search)
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
                    .background(Color(0xFFF0F0F0)
            )) {
                // 根据底部导航选择显示不同内容
                when (selectedItem) {
                    0 -> {
                        // 聊天界面
                        ChatUI(navHostController = navHostController)
                    }
                    1 -> {
                        // 联系人界面
                        ContactsUI(navHostController = navHostController)
                    }
                    2 -> {
                        // 个人界面（示例）
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = androidx.compose.ui.Alignment.Center
                        ) {
                            Text("个人页面")
                        }
                    }
                }
            }
        }
    }
}

// 底部导航项数据类
data class BottomNavItem(val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)