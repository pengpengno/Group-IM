package com.github.im.group.sdk

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp

import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.network.NetworkHeaders
import coil3.network.httpHeaders
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.github.im.group.GlobalCredentialProvider

@Composable
actual fun CrossPlatformImage(
    url: String,
    modifier: Modifier,
    size: Dp,
    token: String?
) {
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

    CoilImageComposable()
}
