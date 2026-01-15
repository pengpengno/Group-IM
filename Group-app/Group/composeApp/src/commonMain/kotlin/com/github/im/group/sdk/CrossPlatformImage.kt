// commonMain/src/commonMain/kotlin/com/example/image/ImageLoader.kt
package com.github.im.group.sdk

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * 跨平台图像组件
 */
@Composable
expect fun CrossPlatformImage(
    file: File,
    modifier: Modifier = Modifier,
    size: Int = 200,
    onLongClick: (() -> Unit)? = null
)
