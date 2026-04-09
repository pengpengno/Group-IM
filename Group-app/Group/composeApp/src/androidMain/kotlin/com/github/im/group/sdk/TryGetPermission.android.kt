package com.github.im.group.sdk

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.accompanist.permissions.rememberPermissionState

private data class PermissionGuideContent(
    val title: String,
    val message: String,
    val icon: ImageVector
)

@OptIn(ExperimentalPermissionsApi::class)
@Composable
actual fun TryGetMultiplePermissions(
    permissions: List<String>,
    onAllGranted: () -> Unit,
    onRequest: () -> Unit,
    onAnyDenied: () -> Unit
) {
    var hasRequestedOnce by remember(permissions) { mutableStateOf(false) }
    var showGuideDialog by remember(permissions) { mutableStateOf(false) }
    val context = LocalContext.current

    val permissionsState = rememberMultiplePermissionsState(
        permissions = permissions,
        onPermissionsResult = { results ->
            if (results.values.all { it }) {
                showGuideDialog = false
                onAllGranted()
            } else {
                showGuideDialog = hasRequestedOnce
                onAnyDenied()
            }
        }
    )

    LaunchedEffect(permissionsState.allPermissionsGranted) {
        if (permissionsState.allPermissionsGranted) {
            showGuideDialog = false
            onAllGranted()
            return@LaunchedEffect
        }
        if (!hasRequestedOnce) {
            hasRequestedOnce = true
            onRequest()
            permissionsState.launchMultiplePermissionRequest()
        }
    }

    if (showGuideDialog) {
        PermissionGuideDialog(
            content = guideContentForPermissions(permissions),
            onOpenSettings = {
                showGuideDialog = false
                onAnyDenied()
                openAppSettings(context)
            },
            onDismiss = {
                showGuideDialog = false
                onAnyDenied()
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
    var hasRequestedOnce by remember(permission) { mutableStateOf(false) }
    var showGuideDialog by remember(permission) { mutableStateOf(false) }
    val context = LocalContext.current

    val permissionState = rememberPermissionState(
        permission = permission,
        onPermissionResult = { granted ->
            if (granted) {
                showGuideDialog = false
                onGranted()
            } else {
                showGuideDialog = hasRequestedOnce
                onDenied()
            }
        }
    )

    LaunchedEffect(permissionState.status.isGranted) {
        if (permissionState.status.isGranted) {
            showGuideDialog = false
            onGranted()
            return@LaunchedEffect
        }
        if (!hasRequestedOnce) {
            hasRequestedOnce = true
            onRequest()
            permissionState.launchPermissionRequest()
        }
    }

    if (showGuideDialog) {
        PermissionGuideDialog(
            content = guideContentForPermission(permission),
            onOpenSettings = {
                showGuideDialog = false
                onDenied()
                openAppSettings(context)
            },
            onDismiss = {
                showGuideDialog = false
                onDenied()
            }
        )
    }
}

@Composable
actual fun TryGetVideoCallPermissions(
    onAllGranted: () -> Unit,
    onRequest: () -> Unit,
    onAnyDenied: () -> Unit
) {
    TryGetMultiplePermissions(
        permissions = listOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        ),
        onAllGranted = onAllGranted,
        onRequest = onRequest,
        onAnyDenied = onAnyDenied
    )
}

@Composable
private fun PermissionGuideDialog(
    content: PermissionGuideContent,
    onOpenSettings: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = true)
    ) {
        Card(modifier = Modifier.padding(16.dp)) {
            Column(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = content.icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(52.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = content.title,
                    style = MaterialTheme.typography.titleLarge
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = content.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = onOpenSettings,
                    modifier = Modifier.fillMaxWidth(0.85f)
                ) {
                    Icon(imageVector = Icons.Default.Settings, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("前往设置开启")
                }

                Spacer(modifier = Modifier.height(8.dp))

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(0.85f)
                ) {
                    Text(
                        text = "稍后再说",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

private fun openAppSettings(context: android.content.Context) {
    val intent = Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.fromParts("package", context.packageName, null)
    )
    context.startActivity(intent)
}

private fun guideContentForPermission(permission: String): PermissionGuideContent = when (permission) {
    Manifest.permission.RECORD_AUDIO -> PermissionGuideContent(
        title = "需要麦克风权限",
        message = "发送语音和语音输入都依赖麦克风权限。你刚刚拒绝了系统授权，现在可以前往系统设置重新开启。",
        icon = Icons.Default.Mic
    )
    Manifest.permission.CAMERA -> PermissionGuideContent(
        title = "需要相机权限",
        message = "拍照和视频通话都依赖相机权限。你刚刚拒绝了系统授权，现在可以前往系统设置重新开启。",
        icon = Icons.Default.VideoCameraBack
    )
    Manifest.permission.READ_MEDIA_VIDEO,
    Manifest.permission.READ_MEDIA_IMAGES,
    Manifest.permission.READ_EXTERNAL_STORAGE -> PermissionGuideContent(
        title = "需要相册权限",
        message = "选择图片和视频需要访问相册。你刚刚拒绝了系统授权，现在可以前往系统设置重新开启。",
        icon = Icons.Default.Image
    )
    else -> PermissionGuideContent(
        title = "需要权限",
        message = "当前功能依赖对应的系统权限。你刚刚拒绝了系统授权，现在可以前往系统设置重新开启。",
        icon = Icons.Default.Info
    )
}

private fun guideContentForPermissions(permissions: List<String>): PermissionGuideContent {
    val hasCamera = permissions.contains(Manifest.permission.CAMERA)
    val hasMic = permissions.contains(Manifest.permission.RECORD_AUDIO)
    val hasMedia = permissions.any {
        it == Manifest.permission.READ_MEDIA_VIDEO ||
            it == Manifest.permission.READ_MEDIA_IMAGES ||
            it == Manifest.permission.READ_EXTERNAL_STORAGE
    }

    return when {
        hasCamera && hasMic -> PermissionGuideContent(
            title = "需要相机和麦克风权限",
            message = "发起视频通话需要同时使用相机和麦克风。你刚刚拒绝了系统授权，现在可以前往系统设置重新开启。",
            icon = Icons.Default.VideoCameraBack
        )
        hasMic -> guideContentForPermission(Manifest.permission.RECORD_AUDIO)
        hasCamera -> guideContentForPermission(Manifest.permission.CAMERA)
        hasMedia -> guideContentForPermission(Manifest.permission.READ_MEDIA_IMAGES)
        else -> PermissionGuideContent(
            title = "需要权限",
            message = "当前功能依赖多项系统权限。你刚刚拒绝了系统授权，现在可以前往系统设置重新开启。",
            icon = Icons.Default.Info
        )
    }
}
