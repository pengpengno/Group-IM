package com.github.im.group.ui.chat

import androidx.compose.runtime.Composable
import com.github.im.group.sdk.PickedFile

@Composable
expect fun MediaPickerScreen(
    onDismiss: () -> Unit,
    onMediaSelected: (List<PickedFile>) -> Unit,
    mediaType: String = "image" // "image" or "video"
)