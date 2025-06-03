package com.github.im.group

import androidx.compose.runtime.Composable
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
@Preview
fun App() {
    LoginScreen {
        loginResponse ->
        println("登录成功：用户ID=${loginResponse.userId}, Token=${loginResponse.token?.take(10)}...")
//        GlobalCredentialProvider.getStorage().saveUserInfo(loginResponse)
        // 这里可以添加更多登录成功后的逻辑，比如跳转到主界面
    }

}