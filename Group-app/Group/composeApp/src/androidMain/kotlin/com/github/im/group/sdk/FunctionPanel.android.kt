package com.github.im.group.ui

import androidx.compose.runtime.Composable
import com.github.im.group.ui.chat.AndroidFilePickerPanel
import com.github.im.group.sdk.FilePicker
import com.github.im.group.sdk.PickedFile

@Composable
actual fun PlatformFilePickerPanel(
    onDismiss: () -> Unit,
    onFileSelected: (List<PickedFile>) -> Unit
) {


    AndroidFilePickerPanel(
        onDismiss = onDismiss,
        onFileSelected = onFileSelected
    )
}