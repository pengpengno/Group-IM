package com.github.im.group.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.github.im.group.manager.LoginStateManager
import com.github.im.group.ui.chat.ChatUI
import com.github.im.group.ui.contacts.ContactsUI
import com.github.im.group.ui.profile.ProfileUI
import com.github.im.group.ui.video.DraggableVideoWindow
import com.github.im.group.ui.video.VideoCallIncomingNotification
import com.github.im.group.viewmodel.ChatViewModel
import com.github.im.group.viewmodel.LoginState
import com.github.im.group.viewmodel.UserViewModel
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

data class BottomNavItem(
    val title: String, 
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val selectedIcon: androidx.compose.ui.graphics.vector.ImageVector
)

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
    val loginState by userViewModel.loginState.collectAsState()
    val userInfo by userViewModel.currentLocalUserInfo.collectAsState()

    var isVideoCallMinimized by remember { mutableStateOf(false) }
    var selectedItem by remember { mutableIntStateOf(0) }
    
    val bottomNavItems = listOf(
        BottomNavItem("聊天", Icons.AutoMirrored.Filled.Chat, Icons.AutoMirrored.Filled.Chat),
        BottomNavItem("联系人", Icons.Default.Contacts, Icons.Default.Contacts),
        BottomNavItem("设置", Icons.Default.Person, Icons.Default.Person)
    )

    if(loginState is LoginState.AuthenticationFailed){
        val authFailed = loginState as LoginState.AuthenticationFailed
        if (!authFailed.isNetworkError) {
            navHostController.navigate(Login)
        }
    }

    LaunchedEffect(userInfo) {
        // 判断是否存在登录凭证 存在 则自动化登陆 ， 不存在则 返回到登录页面
        userViewModel.hasLocalCredential().let {
            if (!it) {
                loginStateManager.setLoggedOut()
                navHostController.navigate(Login)
            }
            else{
                // 如果状态不是登陆中 且 也不是溢价登录的状态  那么就尝试登录一下， 并且如果失败了  那么就定期尝试重试

                if (loginState !is LoginState.Authenticating  && loginState !is LoginState.Authenticated){
                    // Todo  检测失败后  定期重试一下
                    userViewModel.autoLogin()
                }


            }
        }
    }
    LaunchedEffect(userInfo) {
        if(userInfo?.userId != 0L){
            userInfo?.userId?.let { chatViewModel.getConversations(it) }
            userViewModel.loadFriends()
        }
    }

    val topBarTitle = when (selectedItem) {
        0 -> when(loginState) {
            is LoginState.Authenticating -> "连接中..."
            is LoginState.Checking -> "正在更新..."
            is LoginState.AuthenticationFailed -> if ((loginState as LoginState.AuthenticationFailed).isNetworkError) "网络异常" else "聊天"
            else -> "消息"
        }
        1 -> "联系人"
        else -> "个人中心"
    }
    
    val webRTCManager = koinInject<com.github.im.group.sdk.WebRTCManager>()
    val videoCallState by webRTCManager.videoCallState.collectAsState()
    
    if (videoCallState.callStatus == com.github.im.group.ui.video.VideoCallStatus.INCOMING) {
        videoCallState.caller?.let { caller ->
            VideoCallIncomingNotification(
                caller = caller,
                onAccept = {
                    webRTCManager.acceptCall("")
                    isVideoCallMinimized = false
                },
                onReject = {
                    webRTCManager.rejectCall("")
                }
            )
        }
    }
    
    ModalNavigationDrawer(
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = MaterialTheme.colorScheme.surface,
                modifier = Modifier.width(300.dp)
            ) {
                SideDrawer(
                    userInfo = userInfo,
                    onLogout = {
                        loginStateManager.setLoggedOut()
                        navHostController.navigate(Login)
                    },
                    onProfileClick = { 
                        selectedItem = 2
                        scope.launch { drawerState.close() }
                    },
                    onContactsClick = { 
                        selectedItem = 1
                        scope.launch { drawerState.close() }
                    },
                    onMeetingsClick = { navHostController.navigate(Meetings) },
                    onSettingsClick = { navHostController.navigate(Settings) },
                    appVersion = "v1.0.5"
                )
            }
        },
        drawerState = drawerState
    ) { 
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { 
                        Text(
                            text = topBarTitle, 
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        ) 
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "菜单")
                        }
                    },
                    actions = {
                        if (selectedItem == 0) {
                            IconButton(onClick = { navHostController.navigate(Search) }) {
                                Icon(Icons.Default.Add, contentDescription = "发起聊天")
                            }
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            },
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp
                ) {
                    bottomNavItems.forEachIndexed { index, item ->
                        val isSelected = selectedItem == index
                        NavigationBarItem(
                            icon = {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.title,
                                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            label = { 
                                Text(
                                    text = item.title,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                ) 
                            },
                            selected = isSelected,
                            onClick = { selectedItem = index },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                            )
                        )
                    }
                }
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
            ) {
                when (selectedItem) {
                    0 -> ChatUI(navHostController = navHostController)
                    1 -> ContactsUI(navHostController = navHostController)
                    2 -> ProfileUI(navHostController = navHostController)
                }
                
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
