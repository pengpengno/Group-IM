package com.github.im.group.ui.chat

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.LinearEasing
import com.github.im.group.sdk.WithRecordPermission
import com.github.im.group.sdk.getPlatformFilePicker
import com.github.im.group.sdk.playAudio
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.im.group.sdk.PickedFile
import com.github.im.group.ui.FunctionPanel
import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.material.Divider
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.window.Dialog
import com.github.im.group.viewmodel.RecorderUiState
import com.github.im.group.viewmodel.VoiceViewModel
import io.github.aakira.napier.Napier
import org.koin.compose.viewmodel.koinViewModel
import kotlin.contracts.contract

@Composable
fun ChatInputArea(
    onSendText: (String) -> Unit,
    onRelease: (SlideDirection) -> Unit = {},
    onFileSelected: (List<PickedFile>) -> Unit
) {
    var messageText by remember { mutableStateOf("") }
    var isVoiceMode by remember { mutableStateOf(false) }
    var showMorePanel by remember { mutableStateOf(false) }
    var showEmojiPanel by remember { mutableStateOf(false) }
    val filePicker = remember { getPlatformFilePicker() }

    val voiceViewModel : VoiceViewModel = koinViewModel()

    val voiceRecordingState by voiceViewModel.uiState.collectAsState()

    Column(modifier = Modifier.background(Color.White)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth()
        ) {

            if (voiceRecordingState !is RecorderUiState.Recording){
                // 语音模式切换按钮
                IconButton(onClick = { isVoiceMode = !isVoiceMode }) {
                    Icon(
                        if (isVoiceMode) Icons.Default.Keyboard else Icons.Default.Mic,
                        contentDescription = "Toggle input mode"
                    )
                }
            }


                if (isVoiceMode) {
                    // 语音模式但未录音，显示录音按钮
                    VoiceRecordButton(
                        onPress = {
                            voiceViewModel.startRecording()
                        },
                        onRelease = onRelease
                    )

                } else {
                    // 文本输入模式
                    OutlinedTextField(
                        value = messageText,
                        onValueChange = { messageText = it },
                        placeholder = { Text("输入消息...") },
                        modifier = Modifier.weight(1f),
                        maxLines = 4,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send)
                    )
                    // 表情
                    IconButton(onClick = { showEmojiPanel = !showEmojiPanel }) {
                        Icon(Icons.Default.InsertEmoticon, contentDescription = "Emoji")
                    }
                    //   messageText不为空那么隐藏 + 附件按钮
                    if (messageText.isNotBlank()) {
                        IconButton(onClick = {
                            if (messageText.isNotBlank()) {
                                onSendText(messageText)
                                messageText = ""
                            }
                        }) {
                            //发送按钮 ， 消息栏不为空 战士发送按钮
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
                        }
                    } else {
                        // 更多信息
                        IconButton(onClick = { showMorePanel = !showMorePanel }) {
                            Icon(Icons.Default.Add, contentDescription = "More")
                        }
                    }
                }
            }
        }

        // 只有在非录音状态下才显示表情面板和更多面板
        if (voiceRecordingState !is RecorderUiState.Recording) {
            if (showEmojiPanel) {
                // 表情
                EmojiPanel(onEmojiSelected = {
                    messageText += it
                    showEmojiPanel = false
                })
            }

            if (showMorePanel) {
                // 展示  文件 拍照
                FunctionPanel(
                    filePicker = filePicker,
                    onDismiss = { showMorePanel = false },
                    onFileSelected = onFileSelected
                )
            }
       }
}



@Composable
private fun VoiceActionButton(label: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .background(color),
            contentAlignment = Alignment.Center
        ) {
            Text("◎", fontSize = 20.sp, color = Color.White)
        }
        Spacer(Modifier.height(4.dp))
        Text(label, color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp)
    }
}


@Composable
fun VoiceReplay(
    onSend: () -> Unit,
) {
    val voiceViewModel : VoiceViewModel = koinViewModel()
    Dialog(onDismissRequest = { voiceViewModel.cancel()}) {
        Box(
            modifier = Modifier
                .background(Color.White, shape = RoundedCornerShape(12.dp))
                .padding(16.dp)
        ) {
            voiceViewModel.getVoiceData()?.let {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "语音消息",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    VoicePlayer(
                        duration = it.durationMillis / 1000, // 转换为秒
                        audioBytes = it.bytes, // 传递音频数据用于生成波形
                        onPlay = {
                            // 开始播放音频
                            voiceViewModel.getVoicePath()?.let { voicePath->
                                voiceViewModel.audioPlayer.play(voicePath)
                            }
                        },
                        onPause = {
                            // 暂停播放音频
                            voiceViewModel.audioPlayer.pause()
                        },
                        onSeek = { position ->
                            // 跳转到指定位置
                            voiceViewModel.audioPlayer.seekTo((position * 1000).toLong()) // 转换为毫秒
                        }
                    )

                    // 操作按钮
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Button(
                            onClick = {
                                voiceViewModel.cancel()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Gray),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("取消")
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Button(
                            onClick = {
                                onSend()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0088CC)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("发送")
                        }
                    }
                }
            }
        }
    }
}
@Composable
fun VoiceControlOverlayWithRipple(
    amplitude: Int = 50,
    onFinish: (SlideDirection) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    var currentDirection by remember { mutableStateOf(SlideDirection.Start) }

    // 波纹动画
    val infiniteTransition = rememberInfiniteTransition(label = "")
    val rippleRadius by infiniteTransition.animateFloat(
        initialValue = 60f,
        targetValue = 90f,
        animationSpec = infiniteRepeatable(
            tween(durationMillis = 800, easing = LinearEasing),
            RepeatMode.Reverse
        ), label = ""
    )

    // 限制声音波动灵敏度
    val adjustedAmplitude = (amplitude / 4000f).coerceIn(0f, 1f)
    val totalRipple = rippleRadius + adjustedAmplitude * 60



    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f))
//            .then(dragModifier)
    ) {
        // 🎤 中心录音波纹显示
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = (-180).dp)
                .background(Color(0xFF1E1E1E), RoundedCornerShape(12.dp))
                .shadow(8.dp, RoundedCornerShape(12.dp))
                .padding(horizontal = 28.dp, vertical = 22.dp)
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(120.dp)) {

                // 扩散波纹
                Box(
                    modifier = Modifier
                        .size(totalRipple.dp)
                        .background(
                            color = when (currentDirection) {
                                SlideDirection.Start -> Color.Red.copy(alpha = 0.25f)
                                SlideDirection.Right -> Color.Green.copy(alpha = 0.25f)
                                else -> Color(0xFF4CAF50).copy(alpha = 0.2f)
                            },
                            shape = CircleShape
                        )
                )

                // 麦克风图标
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = null,
                    tint = when (currentDirection) {
                        SlideDirection.Left -> Color.Red
                        SlideDirection.Right -> Color.Green
                        else -> Color(0xFF4CAF50)
                    },
                    modifier = Modifier.size(42.dp)
                )
            }
        }

    }
}


/**
 * 语音录制按钮
 */
@Composable
fun VoiceRecordButton(
    onPress: () -> Unit,
    onRelease: (SlideDirection) -> Unit
) {
    var needPermission by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current

    val voiceViewModel : VoiceViewModel = koinViewModel()
    val voiceRecordingState by voiceViewModel.uiState.collectAsState()


    if (needPermission) {
        WithRecordPermission(
            onGranted = {
                onPress()
                needPermission = false
            },
            onDenied = {
                Napier.d("未授权")
                needPermission = false
            }
        )
    }

    Surface(
        tonalElevation = 1.dp,
        modifier = Modifier
            .background(Color.White)
            .pointerInput( Unit){
                while(true){

                    var lastHapticDirection: SlideDirection? = null
                    var slideDirection = SlideDirection.Start
                    var isStillPressed = true
                    Napier.d("isStillPressed: $isStillPressed")

                    awaitPointerEventScope {

                        while (isStillPressed) {
                            Napier.d("isStillPressed: $isStillPressed")
                            val event = awaitPointerEvent()
                            Napier.d("event: $event")
                            val change = event.changes.firstOrNull() ?: continue
//                        val change = event.changes.firstOrNull { it.id == down.id } ?: continue
                            val position = change.position

                            // 判断滑动方向
                            val currentDirection = when {
                                position.x < size.width / 4 -> SlideDirection.Left
                                position.x > size.width * 3 / 4 -> SlideDirection.Right
                                else -> SlideDirection.Start
                            }

                            if (currentDirection != lastHapticDirection &&
                                (currentDirection == SlideDirection.Left || currentDirection == SlideDirection.Right) &&
                                voiceRecordingState is RecorderUiState.Recording
                            ) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                lastHapticDirection = currentDirection
                            }

                            slideDirection = currentDirection

                            // 手指抬起或取消 → 结束循环
                            if (!change.pressed || change.changedToUp()) {
                                isStillPressed = false
                            }
                        }

                        onRelease(slideDirection)

                    }
                }

            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {

                    },
                    onLongPress = {offset ->
                        needPermission = true
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onPress()
                        // 注意：这里拿到的 offset 是触发长按那一刻的位置
                        Napier.d("LongPress triggered at $offset")

                    }
                )
            }

    ) {
        // 三个功能区域：取消 | 松开发送 | 回放 (4/1 : 2/1 : 4/1 的比例)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
//                .clip(RoundedCornerShape(30.dp))
                .background(Color.White),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {

            if (voiceRecordingState is RecorderUiState.Recording){
                // 左侧取消区域 (1/4)
                Text(
                    text = "取消",
                    color = Color.Red,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp, vertical = 12.dp)

                )
                // 分割线
                Divider(
                    color = Color.LightGray,
                    modifier = Modifier
                        .width(1.dp)
                        .fillMaxHeight()
                )
            }

            Text(
                text = if (voiceRecordingState is RecorderUiState.Recording) "松开发送..." else "按住说话",
                textAlign = TextAlign.Center,
                color = if (voiceRecordingState is RecorderUiState.Recording) Color.Black else Color.Black,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier =  if (voiceRecordingState is RecorderUiState.Recording) Modifier.weight(2f)
                            else  Modifier.fillMaxWidth()

            )

            if (voiceRecordingState is RecorderUiState.Recording) {
                // 分割线
                Divider(
                    color = Color.LightGray,
                    modifier = Modifier
                        .width(1.dp)
                        .fillMaxHeight()
                )
                // 右侧回放区域 (1/4)
                Text(
                    text = "回放",
                    color = Color.Green,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp, vertical = 12.dp)
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

