package com.github.im.group.sdk

import androidx.compose.runtime.Composable

/**
 * 用于音频记录的权限处理
 */
@Composable
expect fun WithRecordPermission(
    onGranted: () -> Unit,
    onDenied: () -> Unit
)