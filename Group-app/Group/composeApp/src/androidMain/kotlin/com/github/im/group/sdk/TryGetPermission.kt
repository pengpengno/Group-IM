package com.github.im.group.sdk

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VideoCameraBack
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.MultiplePermissionsState
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import io.github.aakira.napier.Napier

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun TryGetMultiplePermissions(
    permissions: List<String>,
    onAllGranted: () -> Unit,
    onRequest: () -> Unit = {},
    onAnyDenied: () -> Unit = {}
) {
    val multiplePermissionsState = rememberMultiplePermissionsState(permissions)
    var showPermissionScreen by remember { mutableStateOf(false) }

    LaunchedEffect(multiplePermissionsState.permissions) {
        val allGranted = multiplePermissionsState.permissions.all { it.status.isGranted }
        if (allGranted) {
            showPermissionScreen = false
            onAllGranted()
        } else {
            val anyPermanentlyDenied = multiplePermissionsState.permissions.any { !it.status.isGranted && !it.status.shouldShowRationale }
            if (anyPermanentlyDenied) {
                showPermissionScreen = true
                onRequest()
            } else {
                showPermissionScreen = true
                onRequest()
            }
        }
    }
    
    LaunchedEffect(Unit) {
        // 初始时请求权限
        if (!multiplePermissionsState.permissions.all { it.status.isGranted }) {
            multiplePermissionsState.launchMultiplePermissionRequest()
        } else {
            onAllGranted()
        }
    }
    
    // 如果有任何权限被拒绝，显示权限请求界面
    val anyDenied = multiplePermissionsState.permissions.any { !it.status.isGranted }
    if (showPermissionScreen && anyDenied) {
        MultiplePermissionRequestScreen(
            permissions = permissions,
            onPermissionResult = { granted ->
                if (granted) {
                    val allGranted = multiplePermissionsState.permissions.all { it.status.isGranted }
                    if (allGranted) {
                        showPermissionScreen = false
                        onAllGranted()
                    } else {
                        // 仍然有权限未被授予，继续显示
                        showPermissionScreen = true
                    }
                } else {
                    // 保持显示权限请求屏幕
                    showPermissionScreen = true
                    onAnyDenied()
                }
            }
        )
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
actual fun TryGetPermission(
    permission: String,
    onGranted: () -> Unit,
    onRequest: () -> Unit,
    onDenied: () -> Unit
) {
    val recordPermissionState = rememberPermissionState(permission)
    var showPermissionScreen by remember { mutableStateOf(false) }

    LaunchedEffect(recordPermissionState.status) {
        when {
            recordPermissionState.status.isGranted -> {
                Napier.d("权限已授权")
                showPermissionScreen = false
                onGranted()
            }
            !recordPermissionState.status.isGranted && !recordPermissionState.status.shouldShowRationale -> {
                // 权限被永久拒绝
                Napier.d ("权限被永久拒绝,申请获取权限")
                recordPermissionState.launchPermissionRequest()
                showPermissionScreen = true
                onRequest()

            }
            else -> {
                Napier.d ("权限被拒绝")
                recordPermissionState.launchPermissionRequest()
                showPermissionScreen = true
                onRequest()

            }
        }
    }
    
    LaunchedEffect(Unit) {
        // 初始时检查权限状态
        if (!recordPermissionState.status.isGranted) {
            recordPermissionState.launchPermissionRequest()
        } else {
            onGranted()
        }
    }
    
    // 权限被拒绝 就 页面提示并且 尝试 手动引导获取
    if (showPermissionScreen) {

            PermissionRequestScreen(
                permission = permission,
                onPermissionResult = { granted ->
                    if (granted) {
                        showPermissionScreen = false
                        onGranted()
                    } else {
                        // 保持显示权限请求屏幕
                        showPermissionScreen = true
                    }
                }
            )
    }
}

/**
 * 权限申请页面
 */
@Composable
fun PermissionRequestScreen(
    permission: String,
    onPermissionResult: (Boolean) -> Unit
) {
    val context = LocalContext.current
    Napier.d("权限申请页面")

    val needPermissionText  = when(permission) {
        Manifest.permission.RECORD_AUDIO -> "需要麦克风权限"
        Manifest.permission.READ_MEDIA_VIDEO -> "需要相册权限"
        Manifest.permission.READ_MEDIA_IMAGES -> "需要相册权限"
        Manifest.permission.READ_EXTERNAL_STORAGE -> "需要相册权限"
        else -> "需要权限"
    }

    val infoMessage =  when(permission) {
        Manifest.permission.RECORD_AUDIO -> "请允许访问麦克风以录制语音消息"
        Manifest.permission.READ_MEDIA_VIDEO -> "请允许访问相册以选择视频"
        Manifest.permission.READ_MEDIA_IMAGES -> "请允许访问相册以选择图片"
        Manifest.permission.READ_EXTERNAL_STORAGE -> "请允许访问相册以选择图片"
        else -> "请允许权限"
    }

    val icon = when(permission) {
        Manifest.permission.RECORD_AUDIO -> Icons.Default.Mic
        Manifest.permission.READ_MEDIA_VIDEO -> Icons.Default.VideoCameraBack
        Manifest.permission.READ_MEDIA_IMAGES -> Icons.Default.Image
        Manifest.permission.READ_EXTERNAL_STORAGE -> Icons.Default.Image
        else -> Icons.Default.Info
    }

    Dialog(onDismissRequest = { onPermissionResult(false) }
        , properties = DialogProperties( usePlatformDefaultWidth = false )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(64.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = needPermissionText,
                style = MaterialTheme.typography.headlineSmall
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = infoMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    // 引导用户到应用设置页面
                    val intent = Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.fromParts("package", context.packageName, null)
                    )
                    context.startActivity(intent)
                    onPermissionResult(false)
                }
            ) {
                Icon(imageVector = Icons.Default.Settings, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("去设置")
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "提示：您需要在设置中手动授予权限才能继续使用此功能。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * 多权限申请页面
 */
@Composable
fun MultiplePermissionRequestScreen(
    permissions: List<String>,
    onPermissionResult: (Boolean) -> Unit
) {
    val context = LocalContext.current
    Napier.d("多权限申请页面")

    val permissionsText = if (permissions.any { it.contains("MEDIA") }) "需要相册权限" else "需要权限"
    val infoMessage = if (permissions.any { it.contains("MEDIA") }) {
        "请允许访问相册以选择图片和视频"
    } else if (permissions.contains(Manifest.permission.RECORD_AUDIO)) {
        "请允许访问麦克风以录制语音消息"
    } else {
        "请允许权限以继续使用功能"
    }

    Dialog(onDismissRequest = { onPermissionResult(false) }
        , properties = DialogProperties( usePlatformDefaultWidth = false )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Image,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(64.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = permissionsText,
                style = MaterialTheme.typography.headlineSmall
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = infoMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    // 引导用户到应用设置页面
                    val intent = Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.fromParts("package", context.packageName, null)
                    )
                    context.startActivity(intent)
                    onPermissionResult(false)
                }
            ) {
                Icon(imageVector = Icons.Default.Settings, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("去设置")
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "提示：您需要在设置中手动授予权限才能继续使用此功能。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}