package com.github.im.group.sdk

import androidx.compose.runtime.Composable

@Composable
actual fun TryGetPermission(
    permission: String = listOf(),
    onGranted: () -> Unit,
    onRequest: () -> Unit,
    onDenied: () -> Unit
) {
}