package com.github.im.group.ui

import androidx.compose.runtime.Composable
import com.github.im.group.sdk.FilePicker
import com.github.im.group.sdk.PickedFile

@Composable
fun PlatformFilePickerPanel(
    onDismiss: () -> Unit,
    onFileSelected: (List<File>) -> Unit
) {
    // Native platform implementation would go here
    Text("Native File Picker Panel")
}

@Composable
actual fun PlatformFilePickerPanel(
    filePicker: FilePicker,
    onDismiss: () -> Unit,
    onFileSelected: (List<PickedFile>) -> Unit
) {
}