package com.github.im.group.ui.chat

import androidx.compose.runtime.Composable
import com.github.im.group.sdk.File

@Composable
actual fun MediaPickerScreen(
    onDismiss: () -> Unit,
    onMediaSelected: (List<File>) -> Unit
) {
}