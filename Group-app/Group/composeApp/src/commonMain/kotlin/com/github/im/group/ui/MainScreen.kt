package com.github.im.group.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.github.im.group.manager.LoginStateManager
import com.github.im.group.sdk.MediaStream
import com.github.im.group.viewmodel.ChatViewModel
import com.github.im.group.viewmodel.UserViewModel
import com.github.im.group.ui.chat.ChatUI
import com.github.im.group.ui.contacts.ContactsUI
import com.github.im.group.ui.profile.ProfileUI
import com.github.im.group.ui.video.DraggableVideoWindow
import com.github.im.group.ui.video.IncomingCallDialog
import com.github.im.group.viewmodel.LoginState
import io.github.aakira.napier.Napier
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

// 底部导航项数据类
data class BottomNavItem(val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatMainScreen(
    navHostController: NavHostController,
)  {
    val chatViewModel: ChatViewModel = koinViewModel()
    val userViewModel: UserViewModel = koinViewModel()
    val loginStateManager: LoginStateManager = koinInject()

    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val loading by chatViewModel.loading.collectAsState()
    val friends by userViewModel.friends.collectAsState()
    val loginState by userViewModel.loginState.collectAsState()

    // 使用 collectAsState 来监听用户状态变化
    val userInfo = userViewModel.getCurrentUser()
    // 小窗视频通话状态
    var isVideoCallMinimized by remember { mutableStateOf(false) }
    var localMediaStream: MediaStream? by remember { mutableStateOf(null) }
    
    // 底部导航栏选中状态
    var selectedItem by remember { mutableIntStateOf(0) }
    
    val bottomNavItems = listOf(
        BottomNavItem("聊天", Icons.AutoMirrored.Filled.Chat),
        BottomNavItem("联系人", Icons.Default.Contacts),
        BottomNavItem("我", Icons.Default.Person)
    )
    Napier.d("loginState: $loginState")
    if(loginState is LoginState.LoggedFailed){

        // 登录失败则跳转到登录页面
        navHostController.navigate(Login)
    }

    LaunchedEffect(userInfo) {
        if(userInfo?.userId != 0L){
            userInfo?.userId?.let { chatViewModel.getConversations(it) }
            userViewModel.loadFriends()
        }
    }
    var firstTopBarText by remember { mutableStateOf("聊天") }

    when(loginState){
        is LoginState.LoggedIn -> {
            firstTopBarText = "聊天"
        }
        is LoginState.Logging -> {
            firstTopBarText = "登录中..."
        }
        else -> {
            firstTopBarText = "登录"
        }
    }
    
    // 获取WebRTC管理器实例
    val webRTCManager = koinInject<com.github.im.group.sdk.WebRTCManager>()
    
    // 观察来电状态
    val videoCallState by webRTCManager.videoCallState.collectAsState()
    
    // 处理来电逻辑
    if (videoCallState.callStatus == com.github.im.group.ui.video.CallStatus.INCOMING) {
        videoCallState.remoteUser?.let { caller ->
            IncomingCallDialog(
                caller = caller,
                onAccept = {
                    // 接受来电
                    webRTCManager.acceptCall("")
                    
                    // 打开视频通话界面
                    isVideoCallMinimized = false
                },
                onReject = {
                    // 拒绝来电
                    webRTCManager.rejectCall("")
                }
            )
        }
    }
    
    ModalNavigationDrawer(
        drawerContent = {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
            ) {
                SideDrawer(
                    userInfo = userInfo,
                    onLogout = {
                                    loginStateManager.setLoggedOut()
                                    navHostController.navigate(Login)
                               },
                    onProfileClick = { selectedItem = 2},
                    onContactsClick = { navHostController.navigate(Contacts) },
//                    onGroupsClick = { navHostController.navigate("Groups") },
                    onMeetingsClick = { navHostController.navigate(Meetings) },
                    onSettingsClick = { navHostController.navigate(Settings) },
                    appVersion = "v1.2.3"
                )
            }
        },
        drawerState = drawerState
    ) { 
        Scaffold(
            topBar = {
                when (selectedItem) {
                    0 -> {
                        // 聊天界面的 TopAppBar，显示搜索按钮
                        TopAppBar(
                            title = {
                                Text(text = firstTopBarText, color = Color.White)
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
                            colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(
                                containerColor = Color(0xFF0088CC)
                            )
                        )
                    }
                    1 -> {
                        // 联系人界面的 TopAppBar
                        TopAppBar(
                            title = { Text(text = "联系人", color = Color.White) },
                            navigationIcon = {
                                IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                    Icon(Icons.Default.Menu, contentDescription = "菜单", tint = Color.White)
                                }
                            },
                            colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(
                                containerColor = Color(0xFF0088CC)
                            )
                        )
                    }
                    2 -> {
                        // 个人界面的 TopAppBar
                        TopAppBar(
                            title = { Text(text = "我", color = Color.White) },
                            navigationIcon = {
                                IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                    Icon(Icons.Default.Menu, contentDescription = "菜单", tint = Color.White)
                                }
                            },
                            colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(
                                containerColor = Color(0xFF0088CC)
                            )
                        )
                    }
                }
            },
            bottomBar = {
                NavigationBar(
                    containerColor = Color(0xFF0088CC)
                ) {
                    bottomNavItems.forEachIndexed { index, item ->
                        NavigationBarItem(
                            icon = {
                                Icon(
                                    item.icon,
                                    contentDescription = item.title,
                                    tint = if (selectedItem == index) Color.White else Color(0x99FFFFFF)
                                )
                            },
                            label = { 
                                Text(
                                    text = item.title,
                                    color = if (selectedItem == index) Color.White else Color(0x99FFFFFF)
                                ) 
                            },
                            selected = selectedItem == index,
                            onClick = { selectedItem = index }
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
                        // 个人界面
                        ProfileUI(navHostController = navHostController)
                    }
                }
                
                // 小窗视频通话
                if (isVideoCallMinimized) {
                    DraggableVideoWindow(
                        null,
                        Modifier.align(Alignment.BottomEnd)
                    )
                }

            }
        }
    }
}