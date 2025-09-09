package com.github.im.group.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.InsertEmoticon
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.im.group.sdk.AudioPlayer
import com.github.im.group.sdk.VoiceRecordingResult
import com.github.im.group.sdk.WithRecordPermission
import com.github.im.group.sdk.getPlatformFilePicker
import com.github.im.group.sdk.playAudio
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
                filePicker = filerPicker,
                onDismiss = { showMorePanel = false }
            )
        }
    }
}
@Composable
fun RecordingOverlay(
    show: Boolean,
    amplitude: Int,  // 来自 VoiceRecorder 的振幅
    isCanceling: () -> Unit = {}
) {
    if (!show) return

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x55000000)), // 半透明背景
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xDD222222)) // 深灰背景
                .padding(32.dp)
        ) {
            // 波纹效果 + 麦克风
            Box(contentAlignment = Alignment.Center) {
                RippleAnimation(amplitude = amplitude)
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = "Mic",
                    tint = Color.White,
                    modifier = Modifier.size(64.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 提示语
            Text(
                text = "上滑取消",
                color = Color.White,
                fontSize = 14.sp
            )
        }
    }
}


/**
 * 动态波纹动画
 */
@Composable
fun RippleAnimation(amplitude: Int) {
    val infiniteTransition = rememberInfiniteTransition(label = "ripple")

    // 波纹半径动画
    val radius by infiniteTransition.animateFloat(
        initialValue = 40f,
        targetValue = 80f + (amplitude / 50f).coerceAtMost(100f),
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = "radiusAnim"
    )

    // 波纹透明度动画
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = "alphaAnim"
    )

    Canvas(modifier = Modifier.size(160.dp)) {
        drawCircle(
            color = Color.Green.copy(alpha = alpha),
            radius = radius
        )
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
 * 回放录音
 */
@Composable
fun RecordingPlaybackOverlay(
    audioPlayer: AudioPlayer,
    filePath: String,
    onSend: () -> Unit,
    onCancel: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var currentPosition by remember { mutableStateOf(0L) }
    var isPlaying by remember { mutableStateOf(false) }

    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            audioPlayer.play(filePath)
            scope.launch {
                while (isPlaying && currentPosition < audioPlayer.duration) {
                    delay(100)
                    currentPosition = audioPlayer.currentPosition
                }
                if (currentPosition >= audioPlayer.duration) {
                    isPlaying = false
                }
            }
        } else {
            audioPlayer.pause()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x55000000)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xDD222222))
                .padding(24.dp)
                .width(300.dp)
        ) {
            Text("语音回放", color = Color.White, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(16.dp))

            // 播放/暂停按钮
            IconButton(onClick = { isPlaying = !isPlaying }) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(48.dp)
                )
            }

            // 进度条可拖动
            Slider(
                value = currentPosition.toFloat(),
                onValueChange = { newPos ->
                    currentPosition = newPos.toLong()
                    audioPlayer.seekTo(currentPosition)
                },
                valueRange = 0f..audioPlayer.duration.toFloat(),
                modifier = Modifier.fillMaxWidth(),
                colors = SliderDefaults.colors(
                    thumbColor = Color.Green,
                    activeTrackColor = Color.Green
                )
            )

            Text("${(currentPosition/1000)}s / ${(audioPlayer.duration/1000)}s",
                color = Color.LightGray, fontSize = 14.sp)

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "取消",
                    tint = Color.Red,
                    modifier = Modifier
                        .size(40.dp)
                        .clickable {
                            audioPlayer.stop()
                            onCancel()
                        }
                )
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "发送",
                    tint = Color.Green,
                    modifier = Modifier
                        .size(40.dp)
                        .clickable {
                            audioPlayer.stop()
                            onSend()
                        }
                )
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