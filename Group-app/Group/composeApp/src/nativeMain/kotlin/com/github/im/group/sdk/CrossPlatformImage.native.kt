package com.github.im.group.sdk

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp

@Composable
actual fun CrossPlatformImage(
    url: String,
    modifier: Modifier = Modifier,
    size: Dp = 200.dp,
    token: String? = null
) {
    val image: ImageResource? = rememberResource(url, config = kamelConfig)
    Box(modifier = modifier.size(size)) {
        if (image != null) {
            KamelImage(resource = image, contentDescription = null, modifier = modifier.size(size))
        } else {
            CircularProgressIndicator(modifier = Modifier.size(50.dp))
        }
    }
}
