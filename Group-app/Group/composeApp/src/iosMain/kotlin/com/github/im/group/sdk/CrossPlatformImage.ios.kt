package com.github.im.group.sdk

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.UIKitView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.github.im.group.GlobalCredentialProvider
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.NSMutableURLRequest
import platform.Foundation.NSURL
import platform.Foundation.NSURLRequestReloadIgnoringLocalCacheData
import platform.Foundation.setValue
import platform.UIKit.UIImage
import platform.UIKit.UIImageView
import platform.UIKit.UIViewContentModeScaleAspectFit
import platform.UIKit.UIViewContentModeScaleAspectFill
import platform.UIKit.clipsToBounds
import platform.UIKit.imageWithData
import platform.posix.memcpy

@Composable
actual fun CrossPlatformImage(
    file: File,
    modifier: Modifier,
    onLongClick: (() -> Unit)?
) {
    val path = remember(file) { file.dataPath() }
    val isRemotePath = remember(path) { path.startsWith("http://") || path.startsWith("https://") }
    var showFullScreen by remember(path) { mutableStateOf(false) }

    Box(
        modifier = modifier
            .padding(3.dp)
            .combinedClickable(
                onClick = { showFullScreen = true },
                onLongClick = { onLongClick?.invoke() }
            )
    ) {
        IOSImageContent(
            path = path,
            isRemotePath = isRemotePath,
            modifier = Modifier.fillMaxSize(),
            contentMode = UIViewContentModeScaleAspectFill
        )
    }

    if (showFullScreen) {
        IOSFullScreenImage(
            imagePath = path,
            onDismiss = { showFullScreen = false }
        )
    }
}

@Composable
private fun IOSFullScreenImage(
    imagePath: String,
    onDismiss: () -> Unit
) {
    val isRemotePath = remember(imagePath) {
        imagePath.startsWith("http://") || imagePath.startsWith("https://")
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            IOSImageContent(
                path = imagePath,
                isRemotePath = isRemotePath,
                modifier = Modifier.fillMaxSize(),
                contentMode = UIViewContentModeScaleAspectFit
            )

            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
            ) {
                Button(onClick = onDismiss) {
                    Text("关闭")
                }
            }
        }
    }
}

@Composable
internal fun IOSPreviewImageContent(
    file: File,
    modifier: Modifier,
    contentMode: platform.UIKit.UIViewContentMode
) {
    val path = remember(file) { file.dataPath() }
    val isRemotePath = remember(path) { path.startsWith("http://") || path.startsWith("https://") }
    IOSImageContent(
        path = path,
        isRemotePath = isRemotePath,
        modifier = modifier,
        contentMode = contentMode
    )
}

@Composable
private fun IOSImageContent(
    path: String,
    isRemotePath: Boolean,
    modifier: Modifier,
    contentMode: platform.UIKit.UIViewContentMode
) {
    var imageBytes by remember(path) { mutableStateOf<ByteArray?>(null) }
    var isLoading by remember(path) { mutableStateOf(true) }
    var isError by remember(path) { mutableStateOf(false) }

    LaunchedEffect(path) {
        isLoading = true
        isError = false
        imageBytes = runCatching { loadImageBytes(path, isRemotePath) }
            .onFailure { isError = true }
            .getOrNull()
        isLoading = false
    }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        UIKitView(
            factory = {
                UIImageView().apply {
                    this.contentMode = contentMode
                    clipsToBounds = true
                }
            },
            modifier = Modifier.fillMaxSize(),
            update = { imageView ->
                imageView.contentMode = contentMode
                val bytes = imageBytes
                imageView.image = if (bytes != null) UIImage.imageWithData(bytes.toNSData()) else null
            }
        )

        if (isLoading && !isError) {
            CircularProgressIndicator(
                color = Color.White,
                strokeWidth = 3.dp,
                modifier = Modifier.size(40.dp)
            )
        } else if (isError) {
            Text(
                text = if (isRemotePath) "图片准备中..." else "加载失败",
                color = Color.White
            )
        }
    }
}

internal suspend fun loadImageBytes(path: String, isRemotePath: Boolean): ByteArray {
    return if (isRemotePath) {
        loadRemoteImageBytes(path)
    } else {
        val url = resolveIosUrl(path) ?: error("Unsupported image path: $path")
        NSData.dataWithContentsOfURL(url)?.toByteArray()
            ?: error("Unable to read local image: $path")
    }
}

private suspend fun loadRemoteImageBytes(path: String): ByteArray =
    kotlinx.coroutines.suspendCancellableCoroutine { continuation ->
        val url = NSURL.URLWithString(path)
        if (url == null) {
            continuation.resumeWith(Result.failure(IllegalArgumentException("Invalid image URL: $path")))
            return@suspendCancellableCoroutine
        }

        val request = NSMutableURLRequest.requestWithURL(
            url = url,
            cachePolicy = NSURLRequestReloadIgnoringLocalCacheData,
            timeoutInterval = 30.0
        )
        val token = GlobalCredentialProvider.currentToken
        if (token.isNotEmpty()) {
            request.setValue("Bearer $token", forHTTPHeaderField = "Authorization")
            request.setValue("Basic $token", forHTTPHeaderField = "Proxy-Authorization")
        }

        val task = platform.Foundation.NSURLSession.sharedSession.dataTaskWithRequest(request) { data, _, error ->
            if (error != null) {
                continuation.resumeWith(Result.failure(IllegalStateException(error.localizedDescription ?: "Image request failed")))
                return@dataTaskWithRequest
            }
            val bytes = data?.toByteArray()
            if (bytes == null) {
                continuation.resumeWith(Result.failure(IllegalStateException("Empty image response")))
            } else {
                continuation.resume(bytes) {}
            }
        }

        continuation.invokeOnCancellation {
            task.cancel()
        }
        task.resume()
    }

internal fun File.dataPath(): String {
    return when (val data = this.data) {
        is FileData.Path -> data.path
        is FileData.Bytes -> saveTempImage(data.data, name)
        FileData.None -> ""
    }
}

internal fun saveTempImage(bytes: ByteArray, name: String): String {
    val tempDir = platform.Foundation.NSTemporaryDirectory()
    val fileName = if (name.isNotBlank()) name else "tmp_image_${platform.Foundation.NSUUID().UUIDString}.img"
    val path = tempDir + fileName
    val url = NSURL.fileURLWithPath(path)
    bytes.toNSData().writeToURL(url, atomically = true)
    return path
}

internal fun resolveIosUrl(path: String): NSURL? {
    return when {
        path.startsWith("file://") -> NSURL.URLWithString(path)
        path.startsWith("/") -> NSURL.fileURLWithPath(path)
        path.startsWith("http://") || path.startsWith("https://") -> NSURL.URLWithString(path)
        else -> NSURL.fileURLWithPath(path)
    }
}

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
internal fun ByteArray.toNSData(): NSData = usePinned {
    NSData.create(bytes = it.addressOf(0), length = size.toULong())
}

@OptIn(ExperimentalForeignApi::class)
internal fun NSData.toByteArray(): ByteArray {
    val result = ByteArray(length.toInt())
    if (result.isEmpty()) return result
    result.usePinned { pinned ->
        memcpy(pinned.addressOf(0), bytes, length)
    }
    return result
}
