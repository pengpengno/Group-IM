package com.github.im.group.ui.chat

import android.Manifest
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.github.im.group.sdk.AndroidFilePicker
import com.github.im.group.sdk.File
import com.github.im.group.sdk.FilePicker
import com.github.im.group.sdk.TryGetPermission
import com.github.im.group.sdk.rememberFilePickerLauncher
import com.github.im.group.sdk.rememberTakePictureLauncher
import com.github.im.group.ui.theme.ThemeTokens
import io.github.aakira.napier.Napier
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@Composable
fun AndroidFilePickerPanel(
    onDismiss: () -> Unit,
    onFileSelected: (List<File>) -> Unit
) {
    val filePickerLauncher = rememberFilePickerLauncher()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var displayMediaPicker by remember { mutableStateOf(false) }
    var requestCameraPermission by remember { mutableStateOf(false) }
    val filePicker = koinInject<FilePicker>()

    val takePictureLauncher = rememberTakePictureLauncher { success ->
        if (filePicker is AndroidFilePicker) {
            filePicker.onTakePictureResult(success)
        }
    }

    if (filePicker is AndroidFilePicker) {
        filePicker.setFilePickerLauncher(filePickerLauncher)
        filePicker.setTakePictureLauncher(takePictureLauncher)
    }

    if (requestCameraPermission) {
        TryGetPermission(
            permission = Manifest.permission.CAMERA,
            onGranted = {
                requestCameraPermission = false
                scope.launch {
                    try {
                        takePhotoAndHandleResult(filePicker, onFileSelected, onDismiss)
                    } catch (e: SecurityException) {
                        Toast.makeText(context, "没有相机权限，请在系统设置中开启", Toast.LENGTH_LONG).show()
                    } catch (e: Exception) {
                        Toast.makeText(context, "拍照失败: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            onRequest = { Napier.d("request camera permission") },
            onDenied = { requestCameraPermission = false }
        )
    }

    Surface(
        color = Color(0xFFF8FAFC),
        tonalElevation = 8.dp,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
    ) {
        if (displayMediaPicker) {
            MediaPickerScreen(onDismiss = onDismiss, onMediaSelected = onFileSelected)
        } else {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                Text(
                    text = "发送内容",
                    style = MaterialTheme.typography.titleMedium,
                    color = ThemeTokens.TextMain,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "相机和媒体入口放在一起，文件入口单独保留",
                    style = MaterialTheme.typography.bodySmall,
                    color = ThemeTokens.TextSecondary,
                    modifier = Modifier.padding(top = 4.dp, bottom = 14.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    PickerActionCard(
                        icon = Icons.Default.PhotoCamera,
                        title = "相机",
                        subtitle = "拍照后直接发送",
                        containerColor = ThemeTokens.PrimaryBlue,
                        contentColor = Color.White,
                        modifier = Modifier.weight(1f),
                        onClick = { requestCameraPermission = true }
                    )
                    PickerActionCard(
                        icon = Icons.Default.Collections,
                        title = "媒体",
                        subtitle = "图片和视频预览",
                        containerColor = Color.White,
                        contentColor = ThemeTokens.TextMain,
                        modifier = Modifier.weight(1f),
                        onClick = { displayMediaPicker = true }
                    )
                    PickerActionCard(
                        icon = Icons.AutoMirrored.Filled.InsertDriveFile,
                        title = "文件",
                        subtitle = "选择文档或压缩包",
                        containerColor = Color.White,
                        contentColor = ThemeTokens.TextMain,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            scope.launch {
                                try {
                                    val files = filePicker.pickFile()
                                    if (files.isNotEmpty()) {
                                        onFileSelected(files)
                                        onDismiss()
                                    }
                                } catch (e: Exception) {
                                    Napier.e("pick file failed", e)
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

private suspend fun takePhotoAndHandleResult(
    filePicker: FilePicker,
    onFileSelected: (List<File>) -> Unit,
    onDismiss: () -> Unit
) {
    try {
        val photo = filePicker.takePhoto()
        if (photo != null) {
            onFileSelected(listOf(photo))
        }
        onDismiss()
    } catch (e: Exception) {
        Napier.e { "take photo failed: ${e.message}" }
        throw e
    }
}

@Composable
private fun PickerActionCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(22.dp),
        color = containerColor,
        shadowElevation = if (containerColor == Color.White) 1.dp else 0.dp,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        if (containerColor == Color.White) ThemeTokens.PrimaryBlue.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.18f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = if (containerColor == Color.White) ThemeTokens.PrimaryBlue else Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = contentColor,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 12.dp)
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = contentColor.copy(alpha = 0.72f),
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}
