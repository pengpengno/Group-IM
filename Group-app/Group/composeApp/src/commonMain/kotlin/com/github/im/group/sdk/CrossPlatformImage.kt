// commonMain/src/commonMain/kotlin/com/example/image/ImageLoader.kt
package com.github.im.group.sdk

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.github.im.group.GlobalCredentialProvider

/**
 * 公共跨平台接口
 * @param url 图片 URL
 * @param modifier Compose Modifier
 * @param size 图片显示大小
 * @param token 可选鉴权 token
 */
@Composable
expect fun CrossPlatformImage(
    url: String,
    modifier: Modifier = Modifier,
    size: Dp = 200.dp,
    token: String? = GlobalCredentialProvider.currentToken
)
