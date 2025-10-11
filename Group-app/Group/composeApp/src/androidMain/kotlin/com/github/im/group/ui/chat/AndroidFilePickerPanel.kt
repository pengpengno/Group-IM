package com.github.im.group.ui.chat

import android.net.Uri
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.github.im.group.sdk.AndroidFilePicker
import com.github.im.group.sdk.FilePicker
import com.github.im.group.sdk.PickedFile
import com.github.im.group.sdk.rememberFilePickerLauncher
import com.github.im.group.sdk.rememberTakePictureLauncher
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun AndroidFilePickerPanel(
    filePicker: FilePicker,
    onDismiss: () -> Unit,
    onFileSelected: (List<PickedFile>) -> Unit
) {
    val filePickerLauncher = rememberFilePickerLauncher()
    val scope = rememberCoroutineScope()
    
    // 创建拍照启动器
    val takePictureLauncher = rememberTakePictureLauncher()
    
    // 将filePickerLauncher和takePictureLauncher设置到AndroidFilePicker实例中
    if (filePicker is AndroidFilePicker) {
        filePicker.setFilePickerLauncher(filePickerLauncher)
        filePicker.setTakePictureLauncher(takePictureLauncher)
    }
    
    Surface(
        color = Color.White,
        tonalElevation = 8.dp,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .padding(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            IconTextButton(Icons.AutoMirrored.Filled.InsertDriveFile, "文件", {
                scope.launch {
                    try {
                        val files = filePicker.pickFile()
                        onFileSelected(files)
                        onDismiss()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            })
            IconTextButton(Icons.Default.PhotoCamera, "拍照", {
                scope.launch {
                    try {
                        val photo = filePicker.takePhoto()
                        if (photo != null) {
                            onFileSelected(listOf(photo))
                        }
                        onDismiss()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            })
            IconTextButton(Icons.Default.Mic, "视频", {
                scope.launch {
                    try {
                        val videos = filePicker.pickVideo()
                        onFileSelected(videos)
                        onDismiss()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            })
        }
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