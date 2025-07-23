package com.github.im.group.ui

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.github.im.group.sdk.FilePicker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun FunctionPanel(
//    onSelectFile: () -> Unit,
//    onTakePhoto: () -> Unit,
//    onRecordAudio: () -> Unit,
    filePicker: FilePicker ,
    onDismiss: () -> Unit
) {
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
                CoroutineScope(Dispatchers.Main).launch {
                    filePicker.pickFile()
                    onDismiss()
                }
            })
            IconTextButton(Icons.Default.PhotoCamera, "拍照", {
                CoroutineScope(Dispatchers.Main).launch {
                    filePicker.takePhoto()
                    onDismiss()
                }
            })
            IconTextButton(Icons.Default.Mic, "语音", {
                CoroutineScope(Dispatchers.Main).launch {
                    filePicker.pickVideo()
                    onDismiss()
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
