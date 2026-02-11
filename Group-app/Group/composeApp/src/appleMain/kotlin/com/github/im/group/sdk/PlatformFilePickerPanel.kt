package com.github.im.group.sdk

import androidx.compose.runtime.Composable

@Composable
expect fun PlatformFilePickerPanel(
    filePicker: FilePicker,
    onDismiss: () -> Unit,
    onFileSelected: (List<PickedFile>) -> Unit
)