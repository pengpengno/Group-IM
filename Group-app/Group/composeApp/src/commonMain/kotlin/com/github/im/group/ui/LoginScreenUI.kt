package com.github.im.group.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.draw.blur
import com.github.im.group.ui.theme.ThemeTokens
import com.github.im.group.viewmodel.UserViewModel
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreenUI(
    viewModel: UserViewModel = koinViewModel(),
    navController: NavHostController = rememberNavController(),
) {
    var username by remember { mutableStateOf("peng.wang") }
    var password by remember { mutableStateOf("12345") }
    var isLoggingIn by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var passwordVisible by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // 取消原来的渐变背景，使用深色背景+光球
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ThemeTokens.BackgroundDark)
    ) {
        // 绘制背景发光球休以匹配 Web 的 Celestial Glass 风格
        Canvas(modifier = Modifier.fillMaxSize()) {
            val blurRadius = 80.dp.toPx()
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(ThemeTokens.Sphere1.copy(alpha = 0.5f), Color.Transparent),
                    radius = size.width * 0.8f
                ),
                center = Offset(size.width * 0.2f, size.height * 0.2f),
                radius = size.width * 0.8f
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(ThemeTokens.Sphere2.copy(alpha = 0.4f), Color.Transparent),
                    radius = size.width * 0.7f
                ),
                center = Offset(size.width * 0.8f, size.height * 0.8f),
                radius = size.width * 0.7f
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(ThemeTokens.Sphere3.copy(alpha = 0.4f), Color.Transparent),
                    radius = size.width * 0.5f
                ),
                center = Offset(size.width * 0.7f, size.height * 0.4f),
                radius = size.width * 0.5f
            )
        }
        // 设置按钮在顶部
        IconButton(
            onClick = { navController.navigate(ProxySetting) },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
        ) {
            Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Color.White)
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp)
                .align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // App Logo / Title
            Surface(
                modifier = Modifier.size(80.dp),
                shape = RoundedCornerShape(20.dp),
                color = Color.White,
                shadowElevation = 8.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Chat,
                        contentDescription = "Logo",
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

//            Text(
//                text = "Group IM",
//                style = MaterialTheme.typography.headlineLarge,
//                fontWeight = FontWeight.Bold,
//                color = Color.White
//            )
//
//            Text(
//                text = "连接你的工作与社交",
//                style = MaterialTheme.typography.bodyMedium,
//                color = Color.White.copy(alpha = 0.8f)
//            )

            Spacer(modifier = Modifier.height(48.dp))

            // 登录表单卡片
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = ThemeTokens.GlassWhite,
                border = BorderStroke(1.dp, ThemeTokens.GlassBorder),
                shadowElevation = 0.dp
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "欢迎回来",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = ThemeTokens.TextMain,
                        modifier = Modifier.align(Alignment.Start)
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))

                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text("用户名/邮箱") },
                        leadingIcon = { Icon(Icons.Default.Person, null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = Color.LightGray,
                            disabledBorderColor = Color.LightGray,
                            errorBorderColor = MaterialTheme.colorScheme.error
                        )
                    )

                    Spacer(Modifier.height(16.dp))

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("密码") },
                        leadingIcon = { Icon(Icons.Default.Lock, null) },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = null
                                )
                            }
                        },
                        singleLine = true,
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = Color.LightGray,
                            disabledBorderColor = Color.LightGray,
                            errorBorderColor = MaterialTheme.colorScheme.error
                        )
                    )

                    Spacer(Modifier.height(8.dp))
                    
                    TextButton(
                        onClick = { /* Handle forgot password */ },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("忘记密码？", fontSize = 12.sp, color = ThemeTokens.PrimaryBlue, fontWeight = FontWeight.Bold)
                    }

                    Spacer(Modifier.height(16.dp))

                    Button(
                        onClick = {
                            isLoggingIn = true
                            errorMessage = null
                            scope.launch {
                                try {
                                    val loginSuccess = viewModel.login(username, password)
                                    if (loginSuccess) {
                                        navController.navigate(Home) {
                                            popUpTo(Login) { inclusive = true }
                                        }
                                    } else {
                                        errorMessage = "用户名或密码错误"
                                    }
                                } catch (e: Exception) {
                                    errorMessage = "网络连接异常: ${e.message}"
                                } finally {
                                    isLoggingIn = false
                                }
                            }
                        },
                        enabled = !isLoggingIn && username.isNotBlank() && password.isNotBlank(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .background(ThemeTokens.PrimaryGradient, RoundedCornerShape(12.dp)),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent
                        ),
                        elevation = null
                    ) {
                        if (isLoggingIn) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("登 录", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }

                    AnimatedVisibility(
                        visible = errorMessage != null,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        errorMessage?.let {
                            Text(
                                text = it,
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 14.sp,
                                modifier = Modifier.padding(top = 16.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("还没有账号？", color = Color.White.copy(alpha = 0.8f))
                TextButton(onClick = { /* Handle registration */ }) {
                    Text("免费注册", fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}
