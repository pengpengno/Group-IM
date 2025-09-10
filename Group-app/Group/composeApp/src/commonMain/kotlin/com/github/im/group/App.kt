package com.github.im.group

import androidx.compose.runtime.Composable
import cafe.adriel.voyager.navigator.Navigator
import com.github.im.group.ui.LoginScreen

@Composable
fun App() {

    Navigator(LoginScreen())

}