package com.github.im.group

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import cafe.adriel.voyager.navigator.Navigator
import com.github.im.group.model.UserInfo
import com.github.im.group.ui.LoginScreen
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
@Preview
fun App() {
    var isLoggedIn by remember { mutableStateOf(false) }
    val loginResponse = remember { mutableStateOf<UserInfo?>(null) }
    Navigator(LoginScreen())

}