package com.github.im.group.sdk

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.github.im.group.sdk.FileData
import com.github.im.group.sdk.PickedFile
import org.jetbrains.compose.resources.rememberResource
import org.jetbrains.kotlinx.kamel.core.ImageResource
import org.jetbrains.kotlinx.kamel.core.generated.resources.rememberImageResource
import org.jetbrains.kotlinx.kamel.core.generated.resources.rememberResource
import org.jetbrains.kotlinx.kamel.imageloading.generated.resources.kamelConfig
import org.jetbrains.kotlinx.kamel.imageloading.generated.resources.rememberKamelConfig

@Composable
actual fun CrossPlatformImage(
    pickedFile: PickedFile,
    modifier: Modifier = Modifier,
    size: Int = 200
) {
    val url = when (val data = pickedFile.data) {
        is FileData.Path -> data.path
        is FileData.Uri -> data.uri
        is FileData.Bytes -> {
            // Native/Kotlin/Wasm platforms可能不直接支持字节数组转URL
            // 这里暂时返回空URL，后续可以根据实际需求实现
            pickedFile.path
        }
        else -> pickedFile.path
    }
    
    val image: ImageResource? = rememberResource(url, config = kamelConfig)
    Box(modifier = modifier.size(size.dp)) {
        if (image != null) {
            org.jetbrains.kotlinx.kamel.bom.KamelImage(
                resource = image, 
                contentDescription = null, 
                modifier = modifier.size(size.dp)
            )
        } else {
            CircularProgressIndicator(modifier = Modifier.size(50.dp))
        }
    }
}