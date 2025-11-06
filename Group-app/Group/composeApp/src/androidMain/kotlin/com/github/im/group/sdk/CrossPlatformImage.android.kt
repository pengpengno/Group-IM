package com.github.im.group.sdk

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp

import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import coil3.network.NetworkHeaders
import coil3.network.httpHeaders
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.github.im.group.GlobalCredentialProvider
import com.github.im.group.ui.chat.MediaItem
import io.github.aakira.napier.Napier

@Composable
actual fun CrossPlatformImage(
    url: String,
    modifier: Modifier,
    size: Dp,
    token: String?
) {
    /**
     * 小图查看
     */
    @Composable
    fun CoilImageComposable() {
        val context = LocalContext.current
        val token = GlobalCredentialProvider.currentToken
        val headers = NetworkHeaders.Builder()

            .set("Authorization", "Bearer $token")
            .build()
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(url)
                .httpHeaders( headers)
                .crossfade(true)
                .build(),
            contentDescription = null,
            modifier = modifier
//                .size(size)
                .padding(PaddingValues(3.dp,12.dp))
        )

    }

    /**
     *  点击后的大图查看
     */
    @Composable
    fun ImagePreView(
        items: List<MediaItem>,
        currentItem: MediaItem,
        onDismiss: () -> Unit
    ) {
        var currentIndex by remember { mutableIntStateOf(items.indexOf(currentItem)) }

        Dialog(onDismissRequest = onDismiss
            , properties = DialogProperties(dismissOnBackPress = true, usePlatformDefaultWidth = false ,dismissOnClickOutside = true)
        ) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
                val media = items[currentIndex]
                if (media.mimeType.startsWith("image/")) {
                    AsyncImage(
                        model = media.uri,
                        contentDescription = media.name,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                } else if (media.mimeType.startsWith("video/")) {
                    // 使用AndroidView和ExoPlayer直接播放视频
                    Napier.d("play video ${media.uri}")
                    CrossPlatformVideo(media.uri.toString(), Modifier.fillMaxSize(), size = 200.dp)
                }

                // 左右切换
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (currentIndex > 0) {
                        Box(modifier = Modifier
                            .size(48.dp)
                            .clickable { currentIndex-- })
                    }
                    if (currentIndex < items.size - 1) {
                        Box(modifier = Modifier
                            .size(48.dp)
                            .clickable { currentIndex++ })
                    }
                }
            }
        }
    }

    CoilImageComposable()
}
