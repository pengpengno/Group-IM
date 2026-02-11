package com.github.im.group.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Contacts
import androidx.compose.material.icons.outlined.Person
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import io.github.aakira.napier.Napier
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import kotlin.math.roundToInt

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
    var isRefreshing by remember { mutableStateOf(false) }
    var dragOffset by remember { mutableStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }
    
    val bottomNavItems = listOf(
        BottomNavItem("消息", Icons.AutoMirrored.Outlined.Chat, Icons.AutoMirrored.Filled.Chat),
        BottomNavItem("联系人", Icons.Outlined.Contacts, Icons.Filled.Contacts),
        BottomNavItem("我的", Icons.Outlined.Person, Icons.Filled.Person)
    )

    if(loginState is LoginState.AuthenticationFailed){
        val authFailed = loginState as LoginState.AuthenticationFailed
        if (!authFailed.isNetworkError) {
            navHostController.navigate(Login)
        }
    }

    LaunchedEffect(userInfo) {
        userViewModel.hasLocalCredential().let {
            if (!it) {
                loginStateManager.setLoggedOut()
                navHostController.navigate(Login)
            }
            else{
                if (loginState !is LoginState.Authenticating  && loginState !is LoginState.Authenticated){
                    Napier.d { "尝试自动登录" }
                    userViewModel.autoLogin()
                }
            }
        }
    }
    
    fun refreshData() {
        if (!isRefreshing) {
            isRefreshing = true
            scope.launch {
                when (loginState) {
                    is LoginState.AuthenticationFailed -> {
                        if ((loginState as LoginState.AuthenticationFailed).isNetworkError) {
                            userViewModel.retryLogin()
                        } else {
                            navHostController.navigate(Login)
                        }
                    }
                    is LoginState.Authenticated -> {
                        userInfo?.userId?.let { chatViewModel.getConversations(it) }
                    }
                    is LoginState.Authenticating -> {
                        userViewModel.autoLogin()
                    }
                    else -> {
                        userViewModel.autoLogin()
                    }
                }
                kotlinx.coroutines.delay(1000) 
                isRefreshing = false
            }
        }
    }


    val topBarTitle = when (selectedItem) {
        0 -> when(loginState) {
            is LoginState.Authenticating -> "连接中..."
            is LoginState.Checking -> "正在更新..."
            is LoginState.AuthenticationFailed -> if ((loginState as LoginState.AuthenticationFailed).isNetworkError) "网络异常" else "消息"
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = {
                            if (dragOffset == 0f) {
                                isDragging = true
                            }
                        },
                        onDragEnd = {
                            isDragging = false
                            if (dragOffset > 100) {
                                refreshData()
                            }
                            dragOffset = 0f
                        },
                        onDragCancel = {
                            isDragging = false
                            dragOffset = 0f
                        },
                        onDrag = { change, dragAmount ->
                            if (isDragging || !isRefreshing) {
                                if (dragAmount.y > 0) {
                                    dragOffset += dragAmount.y * 0.5f 
                                }
                            }
                            change.consume()
                        }
                    )
                }
        ) {
            Box(
                modifier = Modifier
                    .offset { IntOffset(0, dragOffset.roundToInt()) }
                    .fillMaxSize()
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
                            tonalElevation = 0.dp,
                            modifier = Modifier.height(80.dp)
                        ) {
                            bottomNavItems.forEachIndexed { index, item ->
                                val isSelected = selectedItem == index
                                
                                val animatedScale by animateFloatAsState(
                                    targetValue = if (isSelected) 1.2f else 1.0f,
                                    animationSpec = tween(durationMillis = 300)
                                )
                                
                                val animatedColor by animateColorAsState(
                                    targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    animationSpec = tween(durationMillis = 300)
                                )

                                NavigationBarItem(
                                    icon = {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Icon(
                                                imageVector = if (isSelected) item.selectedIcon else item.icon,
                                                contentDescription = item.title,
                                                modifier = Modifier
                                                    .size(24.dp)
                                                    .scale(animatedScale),
                                                tint = animatedColor
                                            )
                                        }
                                    },
                                    label = { 
                                        Text(
                                            text = item.title,
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                fontSize = 12.sp
                                            ),
                                            color = animatedColor
                                        ) 
                                    },
                                    selected = isSelected,
                                    onClick = { selectedItem = index },
                                    colors = NavigationBarItemDefaults.colors(
                                        indicatorColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f),
                                        selectedIconColor = MaterialTheme.colorScheme.primary,
                                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        selectedTextColor = MaterialTheme.colorScheme.primary,
                                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
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
                            .background(MaterialTheme.colorScheme.surface)
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
            
            if (isRefreshing) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 80.dp)
                ) {
                    androidx.compose.material3.CircularProgressIndicator(
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }
    }
}
