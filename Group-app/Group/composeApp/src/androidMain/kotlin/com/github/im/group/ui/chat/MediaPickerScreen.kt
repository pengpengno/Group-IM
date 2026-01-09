package com.github.im.group.ui.chat

import android.Manifest
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.media3.common.util.UnstableApi
import coil3.compose.AsyncImage
import com.github.im.group.sdk.*
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import io.github.aakira.napier.Napier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class MediaItem(
    val id: Long,
    val name: String,
    val uri: Uri,
    val mimeType: String,
    val size: Long,
    val dateTaken: Long
) {
    fun toPickedFile(): File {
        return File(
            name = this.name,
            path = this.uri.toString(),
            mimeType = this.mimeType,
            size = this.size,
            data = FileData.Path(this.uri.toString())
        )
    }
}

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
actual fun MediaPickerScreen(
    onDismiss: () -> Unit,
    onMediaSelected: (List<File>) -> Unit
) {
    var permissionCheck by remember { mutableStateOf(false) }
    
    // 根据Android版本确定需要请求的权限
    val permissionsToRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        // Android 13+ 需要同时请求图片和视频权限
        listOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO)
    } else {
        // Android 12及以下只需请求存储权限
        listOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }
    
    TryGetMultiplePermissions(
        permissions = permissionsToRequest,
        onAllGranted = {
            permissionCheck = true
        },
        onRequest = {
            // 权限请求中，可以保持当前状态或更新UI
            Napier.d("permission request")
        },
        onAnyDenied = {
            // 权限被拒绝的处理
            Napier.d("permission denied")
        }
    )
    
    if(permissionCheck) {
        UnifiedMediaPicker(
            onDismiss = onDismiss,
            onMediaSelected = onMediaSelected
        )
    }
}


@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun UnifiedMediaPicker(
    onDismiss: () -> Unit,
    onMediaSelected: (List<File>) -> Unit
) {

    Napier.d("link start")

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var previewItem by remember { mutableStateOf<MediaItem?>(null) }

    var mediaItems by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    var selectedItems by remember { mutableStateOf<Set<MediaItem>>(emptySet()) }

    // 添加媒体类型选择状态
    var selectedMediaType by remember { mutableStateOf("all") } // "all", "image", "video"

    // 加载媒体
    fun loadMedia() {
        scope.launch {
            Napier.d("loadMedia permission check success !")

            loadMediaItems(context, selectedMediaType) { items ->
                Napier.d("loadMediaItems success !")
                mediaItems = items
            }
        }
    }

    LaunchedEffect(selectedMediaType) {
        loadMedia()
    }

    // BottomSheet
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false),
        dragHandle = { }
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight(0.8f)
                .padding(4.dp)
        ) {
            // 1. 标题和媒体类型选择
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Text(
                    text = "相册",
                    style = MaterialTheme.typography.titleMedium
                )
                
                // 媒体类型选择标签
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    val mediaTypes = listOf(
                        Pair("all", "全部"),
                        Pair("image", "图片"),
                        Pair("video", "视频")
                    )
                    
                    mediaTypes.forEach { (type, label) ->
                        FilterChip(
                            selected = selectedMediaType == type,
                            onClick = {
                                selectedMediaType = type
                            },
                            label = {
                                Text(label)
                            }
                        )
                    }
                }
            }

            // 2. 媒体网格
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(2.dp)
            ) {
                items(mediaItems, key = { it.id }) { media ->
                    MediaItemView(
                        mediaItem = media,
                        isSelected = selectedItems.contains(media),
                        onSelected = { selected ->
                            selectedItems = if (selected) selectedItems + media else selectedItems - media
                        },
                        onClickPreview = {
                            // 打开预览
                            previewItem = media  // 点击时更新状态

                        }
                    )
                }
            }
// 显示预览
            previewItem?.let { item ->
                MediaPreviewDialog(
                    items = mediaItems,
                    currentItem = item,
                    onDismiss = { previewItem = null },  // 点击关闭时置空状态
                    onMediaSelected = { selectedMedia ->
                        // 预览界面中选择文件后直接返回
                        val files = selectedMedia.map { media ->
                            File(media.name, media.uri.toString(), media.mimeType, media.size, FileData.Path(media.uri.toString()))
                        }
                        onMediaSelected(files)
                        onDismiss()
                    }
                )
            }
            // 3. 发送按钮
            if (selectedItems.isNotEmpty()) {
                Button(
                    onClick = {
                        val files = selectedItems.map { media ->
                            File(media.name, media.uri.toString(), media.mimeType, media.size, FileData.Path(media.uri.toString()))
                        }
                        onMediaSelected(files)
                        onDismiss()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                ) {
                    Text("发送 (${selectedItems.size})")
                }
            }
        }
    }
}

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun MediaPreviewDialog(
    items: List<MediaItem>,
    currentItem: MediaItem,
    onDismiss: () -> Unit = {},
    onMediaSelected: ((List<MediaItem>) -> Unit)? = null
) {
    var currentIndex by remember { mutableIntStateOf(items.indexOf(currentItem)) }
    var selectedItems by remember { mutableStateOf<Set<MediaItem>>(setOf(currentItem)) } // 默认选中当前项

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
                CrossPlatformVideo(media.toPickedFile(), Modifier.fillMaxSize(), size = 200.dp)
            }

            // 显示选中状态 - 在右上角
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
            ) {
                val isSelected = selectedItems.contains(media)
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clickable { 
                            selectedItems = if (isSelected) {
                                selectedItems - media
                            } else {
                                selectedItems + media
                            }
                        }
                        .background(
                            if (isSelected) Color(0xFF0088CC) else Color.Black.copy(alpha = 0.5f), 
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = if (isSelected) "取消选择" else "选择",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // 显示当前索引和总数
            Text(
                text = "${currentIndex + 1}/${items.size}",
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp)
            )

            // 左右切换按钮
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 左箭头（上一个）
                if (currentIndex > 0) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clickable { 
                                currentIndex--
                            }
                            .background(Color.Black.copy(alpha = 0.5f), shape = CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChevronLeft, // 使用左箭头图标
                            contentDescription = "上一个",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                } else {
                    // 如果是第一项，显示一个透明的占位符
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                    )
                }
                
                // 右箭头（下一个）
                if (currentIndex < items.size - 1) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clickable { 
                                currentIndex++
                            }
                            .background(Color.Black.copy(alpha = 0.5f), shape = CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChevronRight, // 使用右箭头图标
                            contentDescription = "下一个",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                } else {
                    // 如果是最后一项，显示一个透明的占位符
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                    )
                }
            }

            // 底部选择按钮
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(
                    onClick = onDismiss
                ) {
                    Text("取消", color = Color.White)
                }

                Button(
                    onClick = {
                        onMediaSelected?.invoke(selectedItems.toList())
                        onDismiss()
                    }
                ) {
                    Text("确定 (${selectedItems.size})")
                }
            }
        }
    }
}


/**
 * 图片 视频展示
 */
@Composable
fun MediaItemView(
    mediaItem: MediaItem,
    isSelected: Boolean,
    onSelected: (Boolean) -> Unit,
    onClickPreview: () -> Unit
) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .padding(2.dp)
    ) {
        if (mediaItem.mimeType.startsWith("video/")) {
            // 对于视频文件，显示视频第一帧
            VideoThumbnail(
                uri = mediaItem.uri,
                contentDescription = mediaItem.name,
                modifier = Modifier
                    .fillMaxSize()
                    .clickable { onClickPreview() }
            )
        } else {
            // 对于图片文件，直接显示
            AsyncImage(
                model = mediaItem.uri,
                contentDescription = mediaItem.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clickable { onClickPreview() }
            )
        }

        // 视频标识
        if (mediaItem.mimeType.startsWith("video/")) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(4.dp),
                contentAlignment = Alignment.BottomStart
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(Color.Black.copy(alpha = 0.5f), shape = CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "视频",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // 多选选中状态 - 显示在右上角
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clickable { onSelected(!isSelected) }
                    .background(
                        if (isSelected) Color(0xFF0088CC) else Color.Black.copy(alpha = 0.5f), 
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "已选择",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun VideoThumbnail(
    uri: Uri,
    contentDescription: String?,
    modifier: Modifier = Modifier
) {
    var thumbnailBitmap by remember(uri) { mutableStateOf<android.graphics.Bitmap?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    
    LaunchedEffect(uri) {
        isLoading = true
        withContext(Dispatchers.IO) {
            thumbnailBitmap = extractVideoFrame(uri)
        }
        isLoading = false
    }
    
    if (isLoading) {
        Box(
            modifier = modifier.background(Color.Gray),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = Color.White
            )
        }
    } else {
        thumbnailBitmap?.let { bitmap ->
            androidx.compose.foundation.Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = modifier
            )
        } ?: Box(
            modifier = modifier.background(Color.Gray),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = contentDescription,
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CameraItem(
    cameraPermissionState: com.google.accompanist.permissions.PermissionState,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .clickable { onClick() }
            .background(Color.LightGray),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.CameraAlt,
            contentDescription = "拍照",
            tint = Color.White,
            modifier = Modifier.size(48.dp)
        )
        
        // 如果没有相机权限，显示锁定图标
        if (!cameraPermissionState.status.isGranted) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "需要相机权限",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

/**
 * 加载媒体项
 */
private suspend fun loadMediaItems(
    context: Context,
    mediaType: String,
    onResult: (List<MediaItem>) -> Unit
) = withContext(Dispatchers.IO) {
    val items = mutableListOf<MediaItem>()
    
    try {
        android.util.Log.d("MediaPickerScreen", "开始加载媒体项，类型: $mediaType")
        
        val projection = arrayOf(
            android.provider.MediaStore.MediaColumns._ID,
            android.provider.MediaStore.MediaColumns.DISPLAY_NAME,
            android.provider.MediaStore.MediaColumns.MIME_TYPE,
            android.provider.MediaStore.MediaColumns.SIZE,
            android.provider.MediaStore.MediaColumns.DATE_TAKEN
        )
        
        // 根据媒体类型构建查询条件
        val (selection, selectionArgs) = when (mediaType) {
            "image" -> {
                "${android.provider.MediaStore.Files.FileColumns.MEDIA_TYPE} = ?" to 
                    arrayOf(android.provider.MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE.toString())
            }
            "video" -> {
                "${android.provider.MediaStore.Files.FileColumns.MEDIA_TYPE} = ?" to 
                    arrayOf(android.provider.MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO.toString())
            }
            else -> {
                // 对于"all"情况，查询图片和视频
                "${android.provider.MediaStore.Files.FileColumns.MEDIA_TYPE} IN (?, ?)" to 
                    arrayOf(
                        android.provider.MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE.toString(),
                        android.provider.MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO.toString()
                    )
            }
        }

        android.util.Log.d("MediaPickerScreen", "查询条件: selection=$selection, selectionArgs=${selectionArgs?.contentToString()}")
        
        val sortOrder = "${android.provider.MediaStore.MediaColumns.DATE_TAKEN} DESC"
        
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            android.provider.MediaStore.Files.getContentUri(android.provider.MediaStore.VOLUME_EXTERNAL)
        } else {
            android.provider.MediaStore.Files.getContentUri("external")
        }
        
        android.util.Log.d("MediaPickerScreen", "查询URI: $collection")
        
        val cursor: android.database.Cursor? = context.contentResolver.query(
            collection,
            projection,
            selection,
            selectionArgs,
            sortOrder
        )
        
        android.util.Log.d("MediaPickerScreen", "查询结果cursor: $cursor")
        
        cursor?.use { c ->
            android.util.Log.d("MediaPickerScreen", "查询到 ${c.count} 条记录")
            
            if (c.count == 0) {
                android.util.Log.d("MediaPickerScreen", "没有找到任何媒体文件")
            }
            
            val idColumn = c.getColumnIndexOrThrow(android.provider.MediaStore.MediaColumns._ID)
            val nameColumn = c.getColumnIndexOrThrow(android.provider.MediaStore.MediaColumns.DISPLAY_NAME)
            val mimeColumn = c.getColumnIndexOrThrow(android.provider.MediaStore.MediaColumns.MIME_TYPE)
            val sizeColumn = c.getColumnIndexOrThrow(android.provider.MediaStore.MediaColumns.SIZE)
            val dateTakenColumn = c.getColumnIndexOrThrow(android.provider.MediaStore.MediaColumns.DATE_TAKEN)
            
            var count = 0
            while (c.moveToNext()) {
                try {
                    val id = c.getLong(idColumn)
                    val name = c.getString(nameColumn) ?: "未命名"
                    val mimeType = c.getString(mimeColumn) ?: "unknown"
                    val size = c.getLong(sizeColumn)
                    val dateTaken = c.getLong(dateTakenColumn)
                    
                    // 根据MIME类型确定内容URI
                    val contentUri = when {
                        mimeType.startsWith("image/") -> {
                            android.content.ContentUris.withAppendedId(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
                        }
                        mimeType.startsWith("video/") -> {
                            android.content.ContentUris.withAppendedId(android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)
                        }
                        else -> {
                            // 默认使用文件URI
                            android.content.ContentUris.withAppendedId(android.provider.MediaStore.Files.getContentUri("external"), id)
                        }
                    }
                    
                    items.add(
                        MediaItem(
                            id = id,
                            name = name,
                            uri = contentUri,
                            mimeType = mimeType,
                            size = size,
                            dateTaken = dateTaken
                        )
                    )
                    
                    count++
                    // 限制加载数量，避免过多内存占用
                    if (count >= 100) {
                        android.util.Log.d("MediaPickerScreen", "达到100个文件限制，停止加载")
                        break
                    }
                } catch (e: Exception) {
                    android.util.Log.e("MediaPickerScreen", "处理单个媒体项时出错", e)
                    continue
                }
            }
            
            android.util.Log.d("MediaPickerScreen", "成功加载 $count 个媒体项")
        } ?: run {
            android.util.Log.w("MediaPickerScreen", "Cursor 为 null，可能没有权限或查询失败")
        }
    } catch (e: SecurityException) {
        // 权限异常处理
        android.util.Log.e("MediaPickerScreen", "权限不足，无法访问媒体文件", e)
    } catch (e: IllegalArgumentException) {
        // 参数异常处理
        android.util.Log.e("MediaPickerScreen", "查询参数错误", e)
    } catch (e: Exception) {
        // 其他异常处理
        android.util.Log.e("MediaPickerScreen", "加载媒体文件时发生未知错误", e)
    }
    
    android.util.Log.d("MediaPickerScreen", "最终返回 ${items.size} 个媒体项")
    
    withContext(Dispatchers.Main) {
        onResult(items)
    }
}

/**
 * 提取视频第一帧
 */
private fun extractVideoFrame(uri: Uri): Bitmap? {
    return try {
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(uri.toString())
        val bitmap = retriever.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
        retriever.release()
        bitmap
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}