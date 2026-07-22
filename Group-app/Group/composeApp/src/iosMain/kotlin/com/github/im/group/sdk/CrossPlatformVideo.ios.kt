package com.github.im.group.sdk

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
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
import platform.AVFoundation.AVPlayer
import platform.AVFoundation.AVPlayerItem
import platform.AVFoundation.AVURLAsset
import platform.AVFoundation.play
import platform.AVKit.AVPlayerViewController
import platform.Foundation.NSMutableDictionary
import platform.Foundation.NSURL
import platform.Foundation.NSUUID
import platform.UIKit.UIViewContentModeScaleAspectFill

@Composable
actual fun CrossPlatformVideo(
    file: File,
    modifier: Modifier,
    onClose: (() -> Unit)?
) {
    VideoThumbnail(
        file = file,
        modifier = modifier,
        previewImagePath = null,
        onClick = { VideoPlayerManager.play(file) },
        onLongClick = null
    )
}

@Composable
actual fun VideoThumbnail(
    file: File,
    modifier: Modifier,
    previewImagePath: String?,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)?
) {
    val previewFile = remember(file, previewImagePath) {
        previewImagePath?.let { path ->
            File(
                name = "${file.name}_preview.jpg",
                path = "",
                mimeType = "image/jpeg",
                size = 0,
                data = FileData.Path(path)
            )
        }
    }

    Box(
        modifier = modifier
            .combinedClickable(
                onClick = onClick,
                onLongClick = { onLongClick?.invoke() }
            ),
        contentAlignment = Alignment.Center
    ) {
        if (previewFile != null) {
            IOSPreviewImageContent(
                file = previewFile,
                modifier = Modifier.fillMaxSize(),
                contentMode = UIViewContentModeScaleAspectFill
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            )
        }

        Icon(
            imageVector = Icons.Filled.PlayArrow,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier
                .size(48.dp)
                .background(Color.Black.copy(alpha = 0.6f))
        )
    }
}

actual object VideoPlayerManager {
    private var currentFile by mutableStateOf<File?>(null)
    private var dialogVisible by mutableStateOf(false)

    actual fun play(file: File) {
        currentFile = file
        dialogVisible = true
    }

    actual fun release() {
        currentFile = null
    }

    @Composable
    actual fun Render() {
        val file = currentFile
        if (dialogVisible && file != null) {
            Dialog(
                onDismissRequest = {
                    dialogVisible = false
                    release()
                },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                ) {
                    IOSVideoPlayerView(
                        file = file,
                        modifier = Modifier.fillMaxSize()
                    )

                    Box(
                        modifier = Modifier.align(Alignment.TopEnd)
                    ) {
                        IconButton(
                            onClick = {
                                dialogVisible = false
                                release()
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = null,
                                tint = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun IOSVideoPlayerView(
    file: File,
    modifier: Modifier
) {
    UIKitView(
        factory = {
            AVPlayerViewController().apply {
                showsPlaybackControls = true
                player = createPlayer(file)
                player?.play()
            }
        },
        modifier = modifier,
        update = { controller ->
            val player = createPlayer(file)
            if (controller.player !== player) {
                controller.player?.pause()
                controller.player = player
                controller.player?.play()
            }
        }
    )
}

private fun createPlayer(file: File): AVPlayer {
    val item = createPlayerItem(file)
    return AVPlayer.playerWithPlayerItem(item)
}

private fun createPlayerItem(file: File): AVPlayerItem {
    val url = when (val data = file.data) {
        is FileData.Path -> resolveVideoUrl(data.path)
        is FileData.Bytes -> NSURL.fileURLWithPath(saveTempVideo(data.data, file.name))
        FileData.None -> resolveVideoUrl(file.path)
    } ?: error("Unsupported video path")

    if (url.absoluteString?.startsWith("http://") == true || url.absoluteString?.startsWith("https://") == true) {
        val options = NSMutableDictionary()
        val token = GlobalCredentialProvider.currentToken
        if (token.isNotEmpty()) {
            val headers = NSMutableDictionary()
            headers.setValue("Bearer $token", forKey = "Authorization")
            headers.setValue("Basic $token", forKey = "Proxy-Authorization")
            options.setValue(headers, forKey = "AVURLAssetHTTPHeaderFieldsKey")
        }
        val asset = AVURLAsset.URLAssetWithURL(url, options = options)
        return AVPlayerItem.playerItemWithAsset(asset)
    }

    return AVPlayerItem.playerItemWithURL(url)
}

private fun resolveVideoUrl(path: String): NSURL? {
    return when {
        path.startsWith("file://") -> NSURL.URLWithString(path)
        path.startsWith("/") -> NSURL.fileURLWithPath(path)
        path.startsWith("http://") || path.startsWith("https://") -> NSURL.URLWithString(path)
        else -> NSURL.fileURLWithPath(path)
    }
}

private fun saveTempVideo(bytes: ByteArray, name: String): String {
    val tempDir = platform.Foundation.NSTemporaryDirectory()
    val fileName = if (name.isNotBlank()) name else "tmp_video_${NSUUID().UUIDString}.mp4"
    val path = tempDir + fileName
    val url = NSURL.fileURLWithPath(path)
    bytes.toNSData().writeToURL(url, atomically = true)
    return path
}
