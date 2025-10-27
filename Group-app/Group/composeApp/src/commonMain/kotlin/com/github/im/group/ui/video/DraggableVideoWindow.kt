package com.github.im.group.ui.video

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.shepeliev.webrtckmp.MediaStream
import kotlin.math.roundToInt

/**
 * 可拖动的小窗视频播放组件
 * 
 * @param mediaStream 媒体流
 * @param modifier Modifier
 * @param windowSize 窗口大小
 * @param onDismissRequest 关闭窗口时的回调
 */
@Composable
fun DraggableVideoWindow(
    mediaStream: MediaStream?,
    modifier: Modifier = Modifier,
    windowSize: Dp = 120.dp,
    onDismissRequest: () -> Unit = {}
) {
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }

    Surface(
        modifier = modifier
            .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
            .pointerInput(Unit) {
                detectDragGestures { _, dragAmount ->
                    offsetX += dragAmount.x
                    offsetY += dragAmount.y
                }
            },
        shape = RoundedCornerShape(8.dp),
        color = Color.Black,
        shadowElevation = 8.dp
    ) {
//        Box(modifier = Modifier.size(windowSize)) {
//            LocalVideoPreview(
//                modifier = Modifier.matchParentSize(),
//                localMediaStream = mediaStream
//            )
//        }
    }
}