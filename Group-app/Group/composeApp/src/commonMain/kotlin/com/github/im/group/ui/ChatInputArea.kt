package com.github.im.group.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.InsertEmoticon
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp

@Composable
fun ChatInputArea(
    modifier: Modifier = Modifier,
    onSendText: (String) -> Unit,
    onSendVoice: () -> Unit,
    onSelectFile: () -> Unit,
    onTakePhoto: () -> Unit,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
    onEmojiSelected: (String) -> Unit
) {
    var messageText by remember { mutableStateOf("") }
    var isVoiceMode by remember { mutableStateOf(false) }
    var showMorePanel by remember { mutableStateOf(false) }
    var showEmojiPanel by remember { mutableStateOf(false) }

    Column(modifier = modifier.background(Color.White)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth()
        ) {
            IconButton(onClick = { showEmojiPanel = !showEmojiPanel }) {
                Icon(Icons.Default.InsertEmoticon, contentDescription = "Emoji")
            }

            IconButton(onClick = { isVoiceMode = !isVoiceMode }) {
                Icon(
                    if (isVoiceMode) Icons.Default.Keyboard else Icons.Default.Mic,
                    contentDescription = "Toggle input mode"
                )
            }

            if (isVoiceMode) {
                VoiceRecordButton(
                    onStart = onStartRecording,
                    onStop = onStopRecording
                )
            } else {
                OutlinedTextField(
                    value = messageText,
                    onValueChange = { messageText = it },
                    placeholder = { Text("输入消息...") },
                    modifier = Modifier.weight(1f),
                    maxLines = 4,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send)
                )
            }

            IconButton(onClick = {
                if (messageText.isNotBlank()) {
                    onSendText(messageText)
                    messageText = ""
                }
            }) {
                Icon(Icons.Default.Send, contentDescription = "Send")
            }

            IconButton(onClick = { showMorePanel = !showMorePanel }) {
                Icon(Icons.Default.Add, contentDescription = "More")
            }
        }

        if (showEmojiPanel) {
            EmojiPanel(onEmojiSelected = {
                messageText += it
                showEmojiPanel = false
            })
        }

        if (showMorePanel) {
            FunctionPanel(
                onSelectFile = onSelectFile,
                onTakePhoto = onTakePhoto,
                onRecordAudio = onSendVoice,
                onDismiss = { showMorePanel = false }
            )
        }
    }
}

@Composable
fun VoiceRecordButton(onStart: () -> Unit, onStop: () -> Unit) {
    var isRecording by remember { mutableStateOf(false) }

    Surface(
        tonalElevation = 1.dp,
        modifier = Modifier
            .background(Color(0xFFEEEEEE))
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isRecording = true
                        onStart()
                        tryAwaitRelease()
                        isRecording = false
                        onStop()
                    }
                )
            }
    ) {
        Text(
            text = if (isRecording) "松开发送" else "按住说话",
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            color = Color.Black
        )
    }
}

@Composable
fun EmojiPanel(onEmojiSelected: (String) -> Unit) {
    val emojis = listOf("😀", "😂", "😍", "😎", "😢", "👍", "🙏", "💯")
    Row(modifier = Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
        emojis.forEach {
            Text(
                text = it,
                fontSize = MaterialTheme.typography.headlineSmall.fontSize,
                modifier = Modifier
                    .padding(4.dp)
                    .clickable { onEmojiSelected(it) }
            )
        }
    }
}

@Composable
fun FunctionPanel(
    onSelectFile: () -> Unit,
    onTakePhoto: () -> Unit,
    onRecordAudio: () -> Unit,
    onDismiss: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0xFFF5F5F5)
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier.padding(8.dp)
        ) {
            IconTextButton(Icons.Default.InsertDriveFile, "文件", onSelectFile)
            IconTextButton(Icons.Default.PhotoCamera, "拍照", onTakePhoto)
            IconTextButton(Icons.Default.Mic, "语音", onRecordAudio)
        }
    }
}

@Composable
fun IconTextButton(icon: ImageVector, text: String, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable(onClick = onClick)) {
        Icon(icon, contentDescription = text, modifier = Modifier.size(32.dp))
        Text(text, style = MaterialTheme.typography.bodySmall)
    }
}
