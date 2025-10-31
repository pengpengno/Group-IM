package com.github.im.group.ui

import ProxyScreen
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import cafe.adriel.voyager.core.screen.Screen
import com.github.im.group.GlobalCredentialProvider
import com.github.im.group.listener.LoginStateManager
import com.github.im.group.ui.chat.ChatRoomScreen
import com.github.im.group.ui.contacts.AddFriendScreen
import com.github.im.group.ui.contacts.ContactsUI
import com.github.im.group.viewmodel.UserViewModel
import kotlinx.coroutines.launch
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel



class MainScreen:Screen{
    @Composable
    override fun Content() {


        LoginScreen()


    }
}

/**
 * 登录界面
 */
@Composable
@Preview
fun LoginScreen() {
    val navController: NavHostController = rememberNavController()
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

        composable<ChatRoom>{ backStackEntry ->
            val chatRoom : ChatRoom = backStackEntry.toRoute()
            ChatRoomScreen(
                conversationId = chatRoom.conversationId,
                onBack = {
                    navController.popBackStack()
                },
                navHostController = navController
            )
        }
    }
}

@Composable
fun LoginScreenUI(
    viewModel: UserViewModel = koinViewModel(),
    navController: NavHostController = rememberNavController(),
) {
    var username by remember{ mutableStateOf("wangpeng")}
    var password by remember { mutableStateOf("1") }
    var autoLogin by remember { mutableStateOf(GlobalCredentialProvider.storage.autoLoginState()) }
    var isLoggingIn by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val userInfo by viewModel.uiState.collectAsState()
    val loginStateManager = koinInject<LoginStateManager>()
    
    MaterialTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .then(Modifier),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "登录",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(32.dp))

                // 登录卡片
                androidx.compose.material3.Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    elevation = androidx.compose.material3.CardDefaults.cardElevation(6.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        OutlinedTextField(
                            value = username,
                            onValueChange = { username = it },
                            label = { Text("用户名") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(Modifier.height(16.dp))

                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("密码") },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(Modifier.height(16.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = autoLogin,
                                onCheckedChange = { autoLogin = it }
                            )
                            Text("自动登录")
                        }

                        Spacer(Modifier.height(16.dp))

                        Button(
                            onClick = {
                                isLoggingIn = true
                                errorMessage = null
                                scope.launch {
                                    try {
                                        // 检查登录结果
                                        val loginSuccess = viewModel.login(username, password)
                                        if (loginSuccess) {
                                            // 登录成功，更新状态和导航
                                            isLoggingIn = false
                                            navController.navigate(Home) {
                                                popUpTo(Login) { inclusive = true }
                                            }
                                        } else {
                                            // 登录失败，只更新状态和错误信息，不导航
                                            isLoggingIn = false
                                            errorMessage = "登录失败"
                                        }
                                    } catch (e: Exception) {
                                        // 处理未预期的异常
                                        isLoggingIn = false
                                        errorMessage = "登录失败: ${e.message}"
                                    }
                                }
                            },
                            enabled = !isLoggingIn,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(if (isLoggingIn) "正在登录..." else "登 录")
                        }

                        if (errorMessage != null) {
                            Spacer(Modifier.height(12.dp))
                            Text(errorMessage ?: "", color = Color.Red)
                        }

                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(onClick = { }) { Text("忘记密码？") }
                    TextButton(onClick = { }) { Text("注册账号") }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = { navController.navigate(ProxySetting) },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Settings,
                        contentDescription = "设置",
                        tint = Color.Gray
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("代理设置")
                }
            }
        }
    }
}