package com.github.im.group.sdk

import android.Manifest
import android.media.MediaRecorder
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Recomposer
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale

@OptIn(ExperimentalPermissionsApi::class)
@Composable
actual fun WithRecordPermission(
    onGranted: () -> Unit,
    onDenied: () -> Unit
) {
    val context = LocalContext.current
    val recordPermission = rememberPermissionState(Manifest.permission.RECORD_AUDIO)

    LaunchedEffect(Unit) {
        recordPermission.launchPermissionRequest()
    }

    when {
        recordPermission.status.isGranted -> onGranted()
        recordPermission.status.shouldShowRationale -> {
            Toast.makeText(context, "请授权录音权限", Toast.LENGTH_SHORT).show()
            onDenied()
        }
        else -> {
            onDenied()
        }
    }
}




