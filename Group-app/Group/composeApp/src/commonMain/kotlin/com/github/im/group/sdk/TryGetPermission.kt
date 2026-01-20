package com.github.im.group.sdk

import androidx.compose.runtime.Composable

/**
 * 用于音频记录的权限处理
 */
@Composable
expect fun TryGetPermission(
    permission: String,
    onGranted: () -> Unit,
    onRequest:() -> Unit,
    onDenied: () -> Unit
)

@Composable
expect fun TryGetMultiplePermissions(
    permissions: List<String>,
    onAllGranted: () -> Unit,
    onRequest: () -> Unit = {},
    onAnyDenied: () -> Unit = {}
)


/**
 * 视频通话权限处理
 */
@Composable
expect fun TryGetVideoCallPermissions(
    onAllGranted: () -> Unit,
    onRequest: () -> Unit = {},
    onAnyDenied: () -> Unit = {}
)
