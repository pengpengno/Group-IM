package com.github.im.group.ui

import androidx.compose.runtime.Composable
import com.github.im.group.ui.chat.AndroidFilePickerPanel
import com.github.im.group.sdk.File

@Composable
actual fun PlatformFilePickerPanel(
    onDismiss: () -> Unit,
    onFileSelected: (List<File>) -> Unit
) {


    AndroidFilePickerPanel(
        onDismiss = onDismiss,
        onFileSelected = onFileSelected
    )
}