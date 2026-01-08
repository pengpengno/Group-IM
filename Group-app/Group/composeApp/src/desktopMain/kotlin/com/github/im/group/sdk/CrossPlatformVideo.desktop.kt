package com.github.im.group.sdk

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp

@Composable
actual fun CrossPlatformVideo(
    file: File,
    modifier: Modifier,
    size: Dp,
    onClose: (() -> Unit)?
) {
    Box(modifier = modifier.fillMaxSize()) {
        Text("Desktop video player not yet implemented")
    }
}