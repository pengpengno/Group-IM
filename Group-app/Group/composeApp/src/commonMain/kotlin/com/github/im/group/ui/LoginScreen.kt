package com.github.im.group.ui

import ProxySettingScreen
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import com.github.im.group.viewmodel.UserViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.viewmodel.koinViewModel


class LoginScreen :Screen{
    @Composable
    override fun Content() {
//        screen()
        loginScreen()
    }
}
@Composable
@Preview
fun loginScreen () {
    val navController: NavHostController = rememberNavController()

    androidx.navigation.compose.NavHost(
            navController = navController,
            startDestination = Login
        ) {
            composable<Login> {
                screen(navController = navController)
            }
            composable<Home> {
                ChatMainScreen(
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
@Preview
fun screen (
    viewModel: UserViewModel = koinViewModel(),
    navController: NavHostController = rememberNavController(),
) {
//    val navigator = LocalNavigator.currentOrThrow
    var username by remember { mutableStateOf("wangpeng") }
    var password by remember { mutableStateOf("1") }
    var autoLogin by remember { mutableStateOf(false) }
    var isLoggingIn by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
//    var userViewModel by remember { mutableStateOf<UserViewModel?>(UserViewModel()) }

    val scope = rememberCoroutineScope()


    val userInfo by  viewModel.uiState.collectAsState()


    MaterialTheme {
        Column(
//            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally

        ) {
            Button(
                onClick = {
                    navController.navigate(ProxySettingScreen())
                },
            ) {
                Icon(
                    imageVector = Icons.Filled.Settings,
                    contentDescription = "设置",
                    tint = Color.Gray

                )
            }

            Spacer(modifier = Modifier.size(12.dp))
        }



        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("登录", style = MaterialTheme.typography.headlineMedium)

            Spacer(Modifier.height(24.dp))

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

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = autoLogin,
                    onCheckedChange = { autoLogin = it }
                )
                Text("自动登录")
            }

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = {
                    isLoggingIn = true
                    errorMessage = null

                    // 使用 CoroutineScope 发起请求
                    scope.launch {
                        try {
                            // 修改为使用 ProxyApi
                            viewModel.login(username, password)

                            withContext(Dispatchers.Main) {
                                isLoggingIn = false
                                /**
                                 * 设置登录用户信息
                                 */
                                println("登录成功: $userInfo")

                                // 登录成功后跳转
//                                navController.navigate(Home)
                                navController.navigate(Home){
                                    popUpTo(Login) { inclusive = true } // 清空登录页栈
                                }
                            }
                        } catch (e: Exception) {
                            withContext(Dispatchers.Main) {
                                isLoggingIn = false
                                println("登录失败: ${e}")
                                errorMessage = "登录失败: ${e.message}"
                            }
                        }
                    }
                },
                enabled = !isLoggingIn,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (isLoggingIn) "正在登录..." else "登录")
            }

            errorMessage?.let {
                Spacer(Modifier.height(12.dp))
                Text(it, color = Color.Red)
            }

            Spacer(Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(onClick = { /* 忘记密码逻辑 */ }) {
                    Text("忘记密码？")
                }
                TextButton(onClick = { /* 注册逻辑 */ }) {
                    Text("注册账号")
                }
            }
        }
    }
}
