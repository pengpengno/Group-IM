package com.github.im.group

import androidx.compose.runtime.Composable
import cafe.adriel.voyager.navigator.Navigator
import com.github.im.group.ui.LoginScreen
import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier

@Composable
fun App() {
    Napier.base(DebugAntilog())
    Navigator(LoginScreen())

}