package com.github.im.group.sdk

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
expect fun CrossPlatformVideo(
    file: File,
    modifier: Modifier = Modifier,
    size: Dp = 200.dp,
    onClose: (() -> Unit)? = null
)