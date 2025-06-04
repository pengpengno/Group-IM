package com.github.im.group.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.github.im.group.model.Friend
import com.github.im.group.model.UserInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

//@Composable
class LoginScreen :Screen{
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        var username by remember { mutableStateOf("") }
        var password by remember { mutableStateOf("") }
        var autoLogin by remember { mutableStateOf(false) }
        var isLoggingIn by remember { mutableStateOf(false) }
        var errorMessage by remember { mutableStateOf<String?>(null) }

        MaterialTheme {
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
                        CoroutineScope(Dispatchers.Default).launch {
                            try {
                                // 修改为使用 ProxyApi
//                                val response = LoginApi.login(username, password)
                                val response = UserInfo(
                                    userId = 1,
                                    username = "test",
                                    email = "test",
                                    token = "test"
                                )
                                withContext(Dispatchers.Main) {
                                    isLoggingIn = false

                                    println("登录成功: $response")
                                    // 登录成功后跳转
                                    navigator?.push(
                                        Main(
                                            userInfo = response,
                                            friends = listOf(
                                                Friend(1, "test", true),
                                                Friend(2, "peng", false)
                                            ),
                                            onFriendClick = { /* 示例中的空操作 */ },
                                            onLogout = {  }
                                        )
                                    )
//                                    onLoginSuccess(response)
                                }
                            } catch (e: Exception) {
                                withContext(Dispatchers.Main) {
                                    isLoggingIn = false
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
}
