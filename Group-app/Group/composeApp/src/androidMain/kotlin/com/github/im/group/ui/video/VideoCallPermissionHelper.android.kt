package com.github.im.group.ui.video

import com.github.im.group.sdk.TryGetMultiplePermissions
import androidx.compose.runtime.Composable
import android.Manifest

@Composable
fun AndroidVideoCallPermissionHelper(
    onAllGranted: () -> Unit,
    onAnyDenied: () -> Unit = {}
) {
    VideoCallPermissionHelper(
        onAllGranted = onAllGranted,
        onAnyDenied = onAnyDenied
    )
}

@Composable
fun VideoCallPermissionHelper(
    onAllGranted: () -> Unit,
    onAnyDenied: () -> Unit = {}
) {
    TryGetMultiplePermissions(
        permissions = listOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO),
        onAllGranted = onAllGranted,
        onAnyDenied = onAnyDenied
    )
}