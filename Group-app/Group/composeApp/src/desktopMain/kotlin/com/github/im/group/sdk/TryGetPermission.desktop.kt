package com.github.im.group.sdk

import androidx.compose.runtime.Composable

@Composable
actual fun TryGetMultiplePermissions(
    permissions: List<String>,
    onAllGranted: () -> Unit,
    onRequest: () -> Unit,
    onAnyDenied: () -> Unit
) {
}

@Composable
actual fun TryGetPermission(
    permission: String ,
    onGranted: () -> Unit,
    onRequest: () -> Unit,
    onDenied: () -> Unit
) {
}

@Composable
actual fun TryGetVideoCallPermissions(
    onAllGranted: () -> Unit,
    onRequest: () -> Unit,
    onAnyDenied: () -> Unit
) {
}