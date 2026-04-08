package com.github.im.group.ui.chat

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection
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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.InsertEmoticon
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.github.im.group.sdk.File
import com.github.im.group.sdk.TryGetPermission
import com.github.im.group.ui.PlatformFilePickerPanel
import com.github.im.group.viewmodel.RecorderUiState
import com.github.im.group.viewmodel.VoiceViewModel
import io.github.aakira.napier.Napier
import io.github.aakira.napier.log
import org.koin.compose.viewmodel.koinViewModel

// Panel state enum to track which bottom panel is open
private enum class InputPanel { NONE, EMOJI, MORE }

/**
 * Telegram-style Chat Input Area
 * Single compact row: [Mic toggle] [Rounded text field] [Emoji] [Add/Send]
 */
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
    val focusRequester = remember { FocusRequester() }

    val voiceViewModel: VoiceViewModel = koinViewModel()
    val voiceRecordingState by voiceViewModel.uiState.collectAsState()

    val inputBarBg = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    val surfaceBg = MaterialTheme.colorScheme.surface

    Column(
        modifier = Modifier
            .background(surfaceBg)
    ) {
        // Top divider
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(0.5.dp)
                .background(MaterialTheme.colorScheme.outlineVariant)
        )

        // ── Main input row ──
        Row(
            verticalAlignment = Alignment.Bottom,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            // Left: mic / keyboard toggle
            if (voiceRecordingState !is RecorderUiState.Recording) {
                IconButton(
                    onClick = {
                        isVoiceMode = !isVoiceMode
                        if (isVoiceMode) {
                            focusManager.clearFocus()
                            activePanel = InputPanel.NONE
                        }
                    },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = if (isVoiceMode) Icons.Default.Keyboard else Icons.Default.Mic,
                        contentDescription = "Toggle voice/text",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            // Center: text field OR voice button
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 4.dp)
            ) {
                if (isVoiceMode) {
                    VoiceRecordButton(
                        onPress = { voiceViewModel.startRecording() },
                        onRelease = onRelease
                    )
                } else {
                    // Rounded pill text input
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 40.dp, max = 120.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(inputBarBg)
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        BasicTextField(
                            value = messageText,
                            onValueChange = { messageText = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequester)
                                .onFocusChanged { if (it.isFocused) activePanel = InputPanel.NONE },
                            textStyle = TextStyle(
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                            keyboardActions = KeyboardActions(onSend = {
                                if (messageText.isNotBlank()) {
                                    onSendText(messageText)
                                    messageText = ""
                                    focusManager.clearFocus()
                                }
                            }),
                            decorationBox = { inner ->
                                if (messageText.isEmpty()) {
                                    Text(
                                        text = "发消息…",
                                        style = TextStyle(
                                            fontSize = 15.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    )
                                }
                                inner()
                            }
                        )
                    }
                }
            }

            // Right: emoji | send-or-add
            if (voiceRecordingState !is RecorderUiState.Recording && !isVoiceMode) {
                // Emoji button
                IconButton(
                    onClick = {
                        focusManager.clearFocus()
                        activePanel = if (activePanel == InputPanel.EMOJI) InputPanel.NONE else InputPanel.EMOJI
                    },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.InsertEmoticon,
                        contentDescription = "Emoji",
                        tint = if (activePanel == InputPanel.EMOJI)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Animated: Send (when text) or Add (when empty)
                AnimatedContent(
                    targetState = messageText.isNotBlank(),
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "send_add"
                ) { hasText ->
                    if (hasText) {
                        // Send button — filled circle  
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                                .clickable {
                                    onSendText(messageText)
                                    messageText = ""
                                    focusManager.clearFocus()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    } else {
                        // Add / attachment button
                        IconButton(
                            onClick = {
                                focusManager.clearFocus()
                                activePanel = if (activePanel == InputPanel.MORE) InputPanel.NONE else InputPanel.MORE
                            },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Attach",
                                tint = if (activePanel == InputPanel.MORE)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }
        }

        // ── Bottom panels (emoji / file picker) ──
        if (voiceRecordingState !is RecorderUiState.Recording) {
            when (activePanel) {
                InputPanel.EMOJI -> {
                    EmojiPanel(onEmojiSelected = {
                        messageText += it
                    })
                }
                InputPanel.MORE -> {
                    PlatformFilePickerPanel(
                        onDismiss = { activePanel = InputPanel.NONE },
                        onFileSelected = { files ->
                            onFileSelected(files)
                            activePanel = InputPanel.NONE
                        }
                    )
                }
                InputPanel.NONE -> { /* nothing */ }
            }
        }
    }
}


/***
 * 录音回放
 */
@Composable
fun VoiceReplay(
    onSend: () -> Unit,
) {
    val voiceViewModel: VoiceViewModel = koinViewModel()
    Dialog(onDismissRequest = { voiceViewModel.cancel() }) {
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
                        duration = it.durationMillis / 1000,
                        audioBytes = it.bytes,
                        onPlay = {
                            voiceViewModel.getVoicePath()?.let { voicePath ->
                                voiceViewModel.audioPlayer.play(voicePath)
                            }
                        },
                        onPause = {
                            voiceViewModel.audioPlayer.pause()
                        },
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
                            onClick = { onSend() },
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

/**
 * 录音遮罩 ui
 */
@Composable
fun VoiceControlOverlayWithRipple(
    amplitude: Int = 50,
) {
    val haptic = LocalHapticFeedback.current
    var currentDirection by remember { mutableStateOf(SlideDirection.Start) }

    val infiniteTransition = rememberInfiniteTransition(label = "")
    val rippleRadius by infiniteTransition.animateFloat(
        initialValue = 60f,
        targetValue = 90f,
        animationSpec = infiniteRepeatable(
            tween(durationMillis = 800, easing = LinearEasing),
            RepeatMode.Reverse
        ), label = ""
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
                .offset(y = (-180).dp)
                .background(Color(0xFF1E1E1E), RoundedCornerShape(12.dp))
                .shadow(8.dp, RoundedCornerShape(12.dp))
                .padding(horizontal = 28.dp, vertical = 22.dp)
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(120.dp)) {
                Box(
                    modifier = Modifier
                        .size(totalRipple.dp)
                        .background(
                            color = when (currentDirection) {
                                SlideDirection.Left -> Color.Red.copy(alpha = 0.25f)
                                SlideDirection.Right -> Color.Green.copy(alpha = 0.25f)
                                else -> Color(0xFF4CAF50).copy(alpha = 0.2f)
                            },
                            shape = CircleShape
                        )
                )
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
    onRelease: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val voiceViewModel: VoiceViewModel = koinViewModel()
    val voiceRecordingState by voiceViewModel.uiState.collectAsState()

    val permission by remember { mutableStateOf("android.permission.RECORD_AUDIO") }
    var permissionRequested by remember { mutableStateOf(false) }
    var hasPermission by remember { mutableStateOf(false) }

    // Only request permission on explicit user action
    if (permissionRequested) {
        TryGetPermission(
            permission = permission,
            onGranted = {
                hasPermission = true
                onPress()
            },
            onRequest = { Napier.d("onRequest") }
        ) {
            hasPermission = false
        }
    }

    var isRecording by remember { mutableStateOf(false) }
    var isCancelRecording by remember { mutableStateOf(false) }

    val pillColor = when {
        isCancelRecording -> Color(0xFFFFEBEE)
        isRecording -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val textColor = when {
        isCancelRecording -> Color(0xFFE53935)
        isRecording -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(pillColor)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = { _ ->
                        if (!permissionRequested) permissionRequested = true
                        if (hasPermission) {
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
                            log { "停止录音 执行后续" }
                            onRelease()
                        } else if (isRecording) {
                            voiceViewModel.cancel()
                        }
                        isRecording = false
                        isCancelRecording = false
                    },
                    onLongPress = {
                        if (!permissionRequested) permissionRequested = true
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        if (hasPermission) {
                            onPress()
                            isRecording = true
                        }
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = when {
                isCancelRecording -> "↑ 松开取消"
                isRecording -> "松开发送 · 上滑取消"
                else -> "按住说话"
            },
            textAlign = TextAlign.Center,
            color = textColor,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}


/**
 * Expanded Emoji panel — grid layout, stays open until user dismisses
 */
@Composable
fun EmojiPanel(onEmojiSelected: (String) -> Unit) {
    val emojiRows = listOf(
        listOf("😀", "😂", "🥲", "😍", "🥰", "😘", "😎", "🤩"),
        listOf("🥺", "😢", "😭", "😡", "🤬", "😤", "🙄", "🤔"),
        listOf("👍", "👎", "👏", "🙏", "🤝", "💪", "🫶", "❤️"),
        listOf("🔥", "💯", "✅", "❌", "🎉", "🎊", "🚀", "💡"),
    )
    val emojis = emojiRows.flatten()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(0.5.dp)
                .background(MaterialTheme.colorScheme.outlineVariant)
        )
        Spacer(modifier = Modifier.height(4.dp))
        LazyVerticalGrid(
            columns = GridCells.Fixed(8),
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp),
            contentPadding = PaddingValues(4.dp)
        ) {
            items(emojis) { emoji ->
                Text(
                    text = emoji,
                    fontSize = 24.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .padding(4.dp)
                        .clickable { onEmojiSelected(emoji) }
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
    }
}
