package com.github.im.group

import androidx.compose.runtime.Composable
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
@Preview
fun App() {
    LoginScreen { username, password, autoLogin ->
        println("正在尝试登录：$username / $password 自动登录：$autoLogin")
        // 调用网络请求或本地验证逻辑
    }

}