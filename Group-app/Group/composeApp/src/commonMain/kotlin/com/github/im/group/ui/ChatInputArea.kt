package com.github.im.group.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.InsertEmoticon
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.github.im.group.sdk.VoiceRecordingResult
import com.github.im.group.sdk.WithRecordPermission
import com.github.im.group.sdk.getPlatformFilePicker
import com.github.im.group.sdk.playAudio

@Composable
fun ChatInputArea(
    modifier: Modifier = Modifier,
    onSendText: (String) -> Unit,
    onStartRecording:  () ->  Unit,
    onStopRecording: () -> Unit,
    onEmojiSelected: (String) -> Unit
) {
    var messageText by remember { mutableStateOf("") }
    var isVoiceMode by remember { mutableStateOf(false) }
    var showMorePanel by remember { mutableStateOf(false) }
    var showEmojiPanel by remember { mutableStateOf(false) }
    var filerPicker = remember {getPlatformFilePicker()}

    Column(modifier = modifier.background(Color.White)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth()
        ) {
//                语音
            IconButton(onClick = { isVoiceMode = !isVoiceMode }) {
                Icon(
                    if (isVoiceMode) Icons.Default.Keyboard else Icons.Default.Mic,
                    contentDescription = "Toggle input mode"
                )
            }
            IconButton(onClick = { showEmojiPanel = !showEmojiPanel }) {
                Icon(Icons.Default.InsertEmoticon, contentDescription = "Emoji")
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

            //   messageText不为空那么隐藏 + 附件按钮
            if(messageText.isNotBlank()){
                IconButton(onClick = {
                    if (messageText.isNotBlank()) {
                        onSendText(messageText)
                        messageText = ""
                    }
                }) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
                }
            }else{

                IconButton(onClick = { showMorePanel = !showMorePanel }) {
                    Icon(Icons.Default.Add, contentDescription = "More")
                }
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
//                onSelectFile = onSelectFile,
//                onTakePhoto = onTakePhoto,
//                onRecordAudio = onSendVoice,
                filePicker = filerPicker,
                onDismiss = { showMorePanel = false }
            )
        }
    }
}

/**
 * 语音录制按钮
 */
@Composable
fun VoiceRecordButton(onStart: () -> Unit, onStop: () -> Unit) {
    var isRecording by remember { mutableStateOf(false) }

    var needPermission by remember { mutableStateOf(false) }

    if (needPermission) {
        WithRecordPermission(
            onGranted = {
                isRecording = true
                onStart()
            },
            onDenied = {
                println("未授权")
            }
        )
        needPermission = false
    }
    // 在按下录音按钮后，生成一个遮罩层，同时想微信一样，如果录音时，拖动到指定位置那就停止录音
    val maskModifier = Modifier.fillMaxWidth().height(48.dp)


    Surface(
        tonalElevation = 1.dp,
        modifier = Modifier
            .background(Color(0xFFEEEEEE))
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        needPermission = true
                        tryAwaitRelease()
                        if(isRecording){
                            isRecording = false
                        }
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


/**
 * 音量遮罩
 */
@Composable
fun MaskVoiceButton(
    onStart: () -> Unit,
    onStop: (canceled: Boolean) -> Unit,
    overlayState: MutableState<Boolean>,
    cancelingState: MutableState<Boolean>
) {
    val touchY = remember { mutableStateOf(0f) }

    Surface(
        modifier = Modifier
            .padding(8.dp)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = {
                        touchY.value = it.y
                        overlayState.value = true
                        cancelingState.value = false
                        onStart()
                    },
                    onDrag = { change, dragAmount ->
                        val dy = dragAmount.y
                        if (dy < -50) { // 向上拖动超过一定阈值则视为取消
                            cancelingState.value = true
                        } else {
                            cancelingState.value = false
                        }
                    },
                    onDragEnd = {
                        val cancel = cancelingState.value
                        overlayState.value = false
                        cancelingState.value = false
                        onStop(cancel)
                    }
                )
            }
    ) {
        Text(
            text = "按住说话",
            modifier = Modifier.padding(16.dp),
            color = Color.Black
        )
    }
}


/**
 * 遮罩层ui
 */
@Composable
fun RecordingOverlay(
    show: Boolean,
    amplitude: Int,
//    amplitude: Flow<Int>,
    isCanceling: ()->Unit
) {
//    val amplitudeValue by amplitude.collectAsState(initial = 0)  // 👈 转成 Compose 状态

    if (show) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0x88000000)), // 半透明黑色遮罩
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("正在录音...", color = Color.White)
                Box(
                    modifier = Modifier
                        .padding(top = 16.dp)
                        .width(50.dp)
                        .height((amplitude / 500).coerceAtMost(200).dp) // 按音量动态变化
                        .background(Color.Green)
                )
                TextButton(onClick = isCanceling) {
                    Text("取消", color = Color.Red)
                }
            }
        }
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
fun AudioPlaybackControl(
    result: VoiceRecordingResult,
    onSend: () -> Unit,
    onCancel: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(8.dp)
    ) {
        Text("录音时长: ${result.durationMillis / 1000}s")
        Spacer(Modifier.width(8.dp))
        Button(onClick = { playAudio(result.bytes) }) {
            Text("播放")
        }
        Spacer(Modifier.width(8.dp))
        Button(onClick = onSend) {
            Text("发送")
        }
        Spacer(Modifier.width(8.dp))
        Button(onClick = onCancel, colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)) {
            Text("取消")
        }
    }
}