package com.github.im.group.ui

import androidx.compose.runtime.Composable
import com.github.im.group.sdk.FilePicker
import com.github.im.group.sdk.PickedFile

@Composable
actual fun PlatformFilePickerPanel(
    filePicker: FilePicker,
    onDismiss: () -> Unit,
    onFileSelected: (List<PickedFile>) -> Unit
) {
}