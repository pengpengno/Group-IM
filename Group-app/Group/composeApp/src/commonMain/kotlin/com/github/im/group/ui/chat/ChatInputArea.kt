package com.github.im.group.ui.chat

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.InsertEmoticon
import androidx.compose.material.icons.filled.KeyboardVoice
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.github.im.group.sdk.File
import com.github.im.group.sdk.TryGetPermission
import com.github.im.group.ui.PlatformFilePickerPanel
import com.github.im.group.ui.theme.ThemeTokens
import com.github.im.group.viewmodel.RecorderUiState
import com.github.im.group.viewmodel.VoiceViewModel
import io.github.aakira.napier.Napier
import org.koin.compose.viewmodel.koinViewModel

private enum class InputPanel { NONE, EMOJI, MORE }

private val quickEmoji = listOf(
    "😀", "😁", "😂", "🥹", "😉", "😍", "😎", "🤔",
    "😭", "😴", "😤", "😮", "👍", "👏", "🎉", "🔥",
    "❤️", "💯", "🙏", "🥳", "👀", "🎧", "📷", "🎤"
)

@Composable
fun ChatInputArea(
    onSendText: (String) -> Unit,
    onRelease: () -> Unit = {},
    onFileSelected: (List<File>) -> Unit
) {
    var messageText by remember { mutableStateOf("") }
    var isVoiceMode by remember { mutableStateOf(false) }
    var activePanel by remember { mutableStateOf(InputPanel.NONE) }
    val focusManager = LocalFocusManager.current

    val voiceViewModel: VoiceViewModel = koinViewModel()
    val voiceRecordingState by voiceViewModel.uiState.collectAsState()
    val hasText = messageText.isNotBlank()

    Column(modifier = Modifier.background(ThemeTokens.BackgroundDark)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Color.White.copy(alpha = 0.08f))
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            InputCircleButton(
                icon = Icons.Default.InsertEmoticon,
                isActive = activePanel == InputPanel.EMOJI,
                onClick = {
                    isVoiceMode = false
                    focusManager.clearFocus()
                    activePanel = if (activePanel == InputPanel.EMOJI) InputPanel.NONE else InputPanel.EMOJI
                }
            )

            Spacer(modifier = Modifier.width(8.dp))

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(42.dp)
                    .clip(RoundedCornerShape(26.dp))
                    .background(Color.White.copy(alpha = 0.96f))
                    .padding(horizontal = 14.dp)
                    .defaultMinSize(minHeight = 42.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                if (isVoiceMode) {
                    VoiceRecordButton(
                        onPress = { voiceViewModel.startRecording() },
                        onRelease = onRelease
                    )
                } else {
                    BasicTextField(
                        value = messageText,
                        onValueChange = {
                            messageText = it
                            if (activePanel == InputPanel.EMOJI) activePanel = InputPanel.NONE
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 22.dp, max = 120.dp),
                        textStyle = TextStyle(
                            fontSize = 15.sp,
                            lineHeight = 20.sp,
                            color = ThemeTokens.TextMain
                        ),
                        cursorBrush = SolidColor(ThemeTokens.InputFocus),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(
                            onSend = {
                                if (messageText.isNotBlank()) {
                                    onSendText(messageText.trim())
                                    messageText = ""
                                    focusManager.clearFocus()
                                }
                            }
                        ),
                        decorationBox = { innerTextField ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .defaultMinSize(minHeight = 22.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                if (messageText.isEmpty()) {
                                    Text(
                                        text = "发送消息",
                                        style = TextStyle(fontSize = 15.sp, color = ThemeTokens.TextSecondary)
                                    )
                                }
                                innerTextField()
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            if (!hasText) {
                InputCircleButton(
                    icon = Icons.Default.AddCircle,
                    isActive = activePanel == InputPanel.MORE,
                    onClick = {
                        isVoiceMode = false
                        focusManager.clearFocus()
                        activePanel = if (activePanel == InputPanel.MORE) InputPanel.NONE else InputPanel.MORE
                    }
                )
                Spacer(modifier = Modifier.width(8.dp))
            }

            AnimatedContent(
                targetState = hasText,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "input_action"
            ) { showSend ->
                if (showSend) {
                    SendButton(
                        onClick = {
                            onSendText(messageText.trim())
                            messageText = ""
                            focusManager.clearFocus()
                        }
                    )
                } else {
                    InputCircleButton(
                        icon = if (isVoiceMode) Icons.Default.KeyboardVoice else Icons.Default.Mic,
                        isActive = isVoiceMode,
                        onClick = {
                            activePanel = InputPanel.NONE
                            focusManager.clearFocus()
                            isVoiceMode = !isVoiceMode
                        }
                    )
                }
            }
        }

        if (voiceRecordingState !is RecorderUiState.Recording) {
            when (activePanel) {
                InputPanel.EMOJI -> EmojiPanel(
                    onEmojiSelected = { emoji -> messageText += emoji },
                    onDismiss = { activePanel = InputPanel.NONE }
                )
                InputPanel.MORE -> PlatformFilePickerPanel(
                    onDismiss = { activePanel = InputPanel.NONE },
                    onFileSelected = { files ->
                        onFileSelected(files)
                        activePanel = InputPanel.NONE
                    }
                )
                InputPanel.NONE -> Unit
            }
        }
    }
}

@Composable
private fun InputCircleButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isActive: Boolean = false,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = if (isActive) ThemeTokens.PrimaryBlue else Color.White.copy(alpha = 0.12f),
        modifier = Modifier.size(42.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isActive) Color.White else Color.White.copy(alpha = 0.92f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun SendButton(onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = ThemeTokens.PrimaryBlue,
        modifier = Modifier.size(42.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Send,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun EmojiPanel(
    onEmojiSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    Surface(
        color = Color(0xFFF8FAFC),
        tonalElevation = 0.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "常用表情",
                    style = MaterialTheme.typography.titleSmall,
                    color = ThemeTokens.TextMain,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "收起",
                    style = MaterialTheme.typography.labelLarge,
                    color = ThemeTokens.PrimaryBlue,
                    modifier = Modifier.clickable(onClick = onDismiss)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            LazyVerticalGrid(columns = GridCells.Fixed(8), modifier = Modifier.height(170.dp)) {
                items(quickEmoji) { emoji ->
                    Box(
                        modifier = Modifier
                            .padding(4.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { onEmojiSelected(emoji) }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = emoji, fontSize = 24.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun VoiceReplay(onSend: () -> Unit) {
    val voiceViewModel: VoiceViewModel = koinViewModel()
    Dialog(onDismissRequest = { voiceViewModel.cancel() }) {
        Box(
            modifier = Modifier
                .background(Color.White, shape = RoundedCornerShape(12.dp))
                .padding(16.dp)
        ) {
            voiceViewModel.getVoiceData()?.let {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "语音消息",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    VoicePlayer(
                        duration = it.durationMillis / 1000,
                        audioBytes = it.bytes,
                        onPlay = {
                            voiceViewModel.getVoicePath()?.let { voicePath ->
                                voiceViewModel.audioPlayer.play(voicePath)
                            }
                        },
                        onPause = { voiceViewModel.audioPlayer.pause() },
                        onSeek = { position ->
                            voiceViewModel.audioPlayer.seekTo((position * 1000).toLong())
                        }
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Button(
                            onClick = { voiceViewModel.cancel() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Gray),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("取消")
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Button(
                            onClick = onSend,
                            colors = ButtonDefaults.buttonColors(containerColor = ThemeTokens.PrimaryBlue),
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
fun VoiceControlOverlayWithRipple(amplitude: Int = 50) {
    val infiniteTransition = rememberInfiniteTransition(label = "voice_overlay")
    val rippleRadius by infiniteTransition.animateFloat(
        initialValue = 60f,
        targetValue = 90f,
        animationSpec = infiniteRepeatable(
            tween(durationMillis = 800, easing = LinearEasing),
            RepeatMode.Reverse
        ),
        label = "voice_ripple"
    )

    val adjustedAmplitude = (amplitude / 4000f).coerceIn(0f, 1f)
    val totalRipple = rippleRadius + adjustedAmplitude * 60

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f))
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .background(Color(0xFF1E1E1E), RoundedCornerShape(12.dp))
                .shadow(8.dp, RoundedCornerShape(12.dp))
                .padding(horizontal = 28.dp, vertical = 22.dp)
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(120.dp)) {
                Box(
                    modifier = Modifier
                        .size(totalRipple.dp)
                        .background(Color(0xFF4CAF50).copy(alpha = 0.2f), shape = CircleShape)
                )
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = null,
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier.size(42.dp)
                )
            }
        }
    }
}

@Composable
fun VoiceRecordButton(
    onPress: () -> Unit,
    onRelease: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val voiceViewModel: VoiceViewModel = koinViewModel()
    val voiceRecordingState by voiceViewModel.uiState.collectAsState()

    val permission = remember { "android.permission.RECORD_AUDIO" }
    var permissionRequested by remember { mutableStateOf(false) }
    var hasPermission by remember { mutableStateOf(false) }

    if (permissionRequested) {
        TryGetPermission(
            permission = permission,
            onGranted = {
                hasPermission = true
                onPress()
            },
            onRequest = { Napier.d("request record audio permission") },
            onDenied = { hasPermission = false }
        )
    }

    var isRecording by remember { mutableStateOf(false) }
    var isCancelRecording by remember { mutableStateOf(false) }

    val pillColor = when {
        isCancelRecording -> Color(0xFFFFEBEE)
        isRecording -> ThemeTokens.PrimaryBlue.copy(alpha = 0.14f)
        else -> Color(0xFFF1F5F9)
    }
    val textColor = when {
        isCancelRecording -> Color(0xFFE53935)
        isRecording -> ThemeTokens.PrimaryBlueEnd
        else -> ThemeTokens.TextSecondary
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(42.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(pillColor)
            .pointerInput(hasPermission, permissionRequested) {
                detectTapGestures(
                    onPress = {
                        if (!permissionRequested) permissionRequested = true
                        if (hasPermission) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onPress()
                            isRecording = true
                        }
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent()
                                isCancelRecording = event.changes.any { change ->
                                    change.position.x < 0 || change.position.x > size.width ||
                                        change.position.y < 0 || change.position.y > size.height
                                }
                                if (event.changes.any { it.changedToUp() }) break
                            }
                        }
                        if (isRecording && !isCancelRecording) {
                            voiceViewModel.stopRecording()
                            onRelease()
                        } else if (isRecording) {
                            voiceViewModel.cancel()
                        }
                        isRecording = false
                        isCancelRecording = false
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        val label = when {
            voiceRecordingState is RecorderUiState.Recording && isCancelRecording -> "松开发送，移出取消"
            voiceRecordingState is RecorderUiState.Recording -> "正在录音，松开发送"
            else -> "按住说话"
        }
        Text(text = label, color = textColor, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}




