package com.github.im.group.ui

import ProxyScreen
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.github.im.group.GlobalCredentialProvider
import com.github.im.group.GlobalErrorHandler
import com.github.im.group.repository.UserRepository
import com.github.im.group.ui.chat.ChatRoomScreen
import com.github.im.group.ui.contacts.AddFriendScreen
import com.github.im.group.ui.contacts.ContactsUI
import com.github.im.group.viewmodel.UserViewModel
import io.github.aakira.napier.Napier
import kotlinx.coroutines.flow.collectLatest
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

/**
 * 应用启动时的初始路由界面，用于判断用户登录状态并导航到相应页面
 */
@Composable
fun AppStartScreen() {
    val navController: NavHostController = rememberNavController()
    val userViewModel: UserViewModel = koinViewModel()
    val userRepository: UserRepository = koinInject()
    var appStartState by remember { mutableStateOf<AppStartState>(AppStartState.CheckingCredentials) }
    
    val userState by userRepository.userState.collectAsState()

    // 初始化全局错误处理器
    LaunchedEffect(Unit) {
        GlobalErrorHandler.initialize(userRepository, userViewModel.loginStateManager)
    }

    // 检查本地是否有登录凭据并决定导航
    LaunchedEffect(Unit) {
        val userInfo = GlobalCredentialProvider.storage.getUserInfo()
        Napier.d { "userInfo $userInfo" }

        if (userInfo!=null) {
            // 如果本地有登录凭据，立即跳转到主页，然后在后台进行自动登录
            appStartState = AppStartState.Authenticated
            Napier.d { "appStartState $appStartState" }

//            userViewModel.autoLogin()
        } else {
            // 如果没有本地登录凭据，直接进入未认证状态
            appStartState = AppStartState.Unauthenticated
        }

    }

    Napier.d { "appStartState $appStartState globalUser $GlobalCredentialProvider.storage.getUserInfo()" }

    // 监听全局未认证事件
    LaunchedEffect(Unit) {
        GlobalErrorHandler.unauthorizedEvents.collectLatest {
            appStartState = AppStartState.Unauthenticated
        }
    }


    Napier.d { "appStartState $appStartState, userState: $userState" }
    
    // 根据应用启动状态决定显示内容
    when (appStartState) {
        AppStartState.CheckingCredentials -> {
            // 检查本地凭据状态 - 这个状态应该很快过去
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
                Text("检查登录状态...", modifier = Modifier.padding(top = 40.dp))
            }
        }
        AppStartState.Authenticated -> {
            // 已登录，直接导航到主页
            androidx.navigation.compose.NavHost(
                navController = navController,
                startDestination = Home
            ) {
                composable<Login> {
                    LoginScreenUI(navController = navController)
                }
                composable<Home> {
                    ChatMainScreen(
                        navHostController = navController
                    )
                }
                composable<ProxySetting> {
                    ProxyScreen(
                        navHostController = navController,
                    )
                }
                composable<Contacts> {
                    ContactsUI(
                        navHostController = navController,
                    )
                }
                composable<Search> {
                    SearchScreen(
                        navHostController = navController
                    )
                }
                
                composable<AddFriend> {
                    AddFriendScreen(
                        navHostController = navController
                    )
                }


                composable<ChatRoom>{ backStackEntry ->
                    val chatRoom : ChatRoom = backStackEntry.toRoute()
                    ChatRoomScreen(
                        chatRoom = chatRoom,
//                        conversationId = chatRoom.conversationId,
                        onBack = {
                            navController.popBackStack()
                        },
                        navHostController = navController
                    )
                }
//                composable<ChatRoom.CreatePrivate>{ backStackEntry ->
//                    val chatRoom : ChatRoom.CreatePrivate = backStackEntry.toRoute()
//                    ChatRoomScreen(
//                        chatRoom = chatRoom,
////                        conversationId = chatRoom.conversationId,
//                        onBack = {
//                            navController.popBackStack()
//                        },
//                        navHostController = navController
//                    )
//                }
            }
        }
        AppStartState.Unauthenticated -> {
            // 未登录，显示登录页面
            androidx.navigation.compose.NavHost(
                navController = navController,
                startDestination = Login
            ) {
                composable<Login> {
                    LoginScreenUI(navController = navController)
                }
                composable<Home> {
                    ChatMainScreen(
                        navHostController = navController
                    )
                }
                composable<ProxySetting> {
                    ProxyScreen(
                        navHostController = navController,
                    )
                }
                composable<Contacts> {
                    ContactsUI(
                        navHostController = navController,
                    )
                }
                composable<Search> {
                    SearchScreen(
                        navHostController = navController
                    )
                }
                
                composable<AddFriend> {
                    AddFriendScreen(
                        navHostController = navController
                    )
                }
//
//                composable<ChatRoom.Conversation>{ backStackEntry ->
//                    val chatRoom : ChatRoom.Conversation = backStackEntry.toRoute()
//                    ChatRoomScreen(
//                        chatRoom = chatRoom,
////                        conversationId = chatRoom.conversationId,
//                        onBack = {
//                            navController.popBackStack()
//                        },
//                        navHostController = navController
//                    )
//                }
                composable<ChatRoom>{ backStackEntry ->
                    val chatRoom : ChatRoom = backStackEntry.toRoute()
                    ChatRoomScreen(
                        chatRoom = chatRoom,
//                        conversationId = chatRoom.conversationId,
                        onBack = {
                            navController.popBackStack()
                        },
                        navHostController = navController
                    )
                }
            }
        }
    }
}

// 定义应用启动状态枚举
sealed class AppStartState {
    object CheckingCredentials : AppStartState()  // 检查本地凭据状态（很短暂）
    object Authenticated : AppStartState()
    object Unauthenticated : AppStartState()
}

// 定义启动路由
object AppStartRoute