package com.github.im.group.ui.chat

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Mic
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.github.im.group.sdk.AndroidFilePicker
import com.github.im.group.sdk.FilePicker
import com.github.im.group.sdk.PickedFile
import com.github.im.group.sdk.rememberFilePickerLauncher
import com.github.im.group.sdk.rememberTakePictureLauncher
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.launch

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun AndroidFilePickerPanel(
    filePicker: FilePicker,
    onDismiss: () -> Unit,
    onFileSelected: (List<PickedFile>) -> Unit
) {
    val filePickerLauncher = rememberFilePickerLauncher()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var displayMediaPicker by remember { mutableStateOf(false) }
    // 使用 Accompanist 权限库请求相机权限
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)
    
    // 创建拍照启动器
    val takePictureLauncher = rememberTakePictureLauncher { success ->
        // 处理拍照结果
        if (filePicker is AndroidFilePicker) {
            filePicker.onTakePictureResult(success)
        }
    }
    
    // 将filePickerLauncher和takePictureLauncher设置到AndroidFilePicker实例中
    if (filePicker is AndroidFilePicker) {
        filePicker.setFilePickerLauncher(filePickerLauncher)
        filePicker.setTakePictureLauncher(takePictureLauncher)
    }
    
//    if (displayMediaPicker) {
//        Dialog(
//            onDismissRequest = { displayMediaPicker = false },
//            properties = DialogProperties(usePlatformDefaultWidth = false)
//        ) {
//            MediaPickerScreen(
//                onDismiss = {
//                    displayMediaPicker = false
//                },
//                onMediaSelected = { files ->
//                    onFileSelected(files)
//                    displayMediaPicker = false
//                },
//                mediaType = "all"
//            )
//        }
//    }
    
    Surface(
        color = Color.White,
        tonalElevation = 8.dp,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .padding(8.dp)
    ) {
    if (displayMediaPicker) {

            UnifiedMediaPicker (
                onDismiss = {
                    displayMediaPicker = false
                },
                onMediaSelected = { files ->
                    onFileSelected(files)
                    displayMediaPicker = false
                },
            )
        }else{
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                IconTextButton(Icons.AutoMirrored.Filled.InsertDriveFile, "文件", {
                    displayMediaPicker = true
                })

                IconTextButton(Icons.Default.PhotoCamera, "拍照", {
                    // 检查是否已有相机权限
                    if (cameraPermissionState.status.isGranted) {
                        // 已有权限，直接拍照
                        scope.launch {
                            try {
                                takePhotoAndHandleResult(filePicker, onFileSelected, onDismiss)
                            } catch (e: SecurityException) {
                                Toast.makeText(context, "没有相机权限，请在设置中开启", Toast.LENGTH_LONG).show()
                            } catch (e: Exception) {
                                Toast.makeText(context, "拍照失败: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } else {
                        // 请求相机权限
                        cameraPermissionState.launchPermissionRequest()
                    }
                })
            }
        }

    }
}

private suspend fun takePhotoAndHandleResult(
    filePicker: FilePicker,
    onFileSelected: (List<PickedFile>) -> Unit,
    onDismiss: () -> Unit
) {
    try {
        val photo = filePicker.takePhoto()
        if (photo != null) {
            onFileSelected(listOf(photo))
        }
        onDismiss()
    } catch (e: Exception) {
        e.printStackTrace()
        throw e
    }
}


@Composable
fun IconTextButton(icon: ImageVector, text: String, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .padding(12.dp)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, contentDescription = text, modifier = Modifier.size(32.dp))
        Text(text, style = MaterialTheme.typography.bodySmall)
    }
}