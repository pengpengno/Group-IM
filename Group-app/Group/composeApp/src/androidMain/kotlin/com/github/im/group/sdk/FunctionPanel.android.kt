package com.github.im.group.ui

import androidx.compose.runtime.Composable
import com.github.im.group.sdk.File
import com.github.im.group.ui.chat.AndroidFilePickerPanel

@Composable
fun PlatformFilePickerPanel(
    onDismiss: () -> Unit,
    onFileSelected: (List<File>) -> Unit
) {

    AndroidFilePickerPanel(
        onDismiss = onDismiss,
        onFileSelected = onFileSelected
    )
}