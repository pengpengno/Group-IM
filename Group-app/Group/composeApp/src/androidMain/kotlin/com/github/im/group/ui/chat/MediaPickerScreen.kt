package com.github.im.group.ui.chat

import android.Manifest
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.view.ViewGroup
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil3.compose.AsyncImage
import com.github.im.group.sdk.File
import com.github.im.group.sdk.FileData
import com.github.im.group.sdk.MediaFileView
import com.github.im.group.sdk.TryGetMultiplePermissions
import com.github.im.group.ui.theme.ThemeTokens
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import io.github.aakira.napier.Napier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private enum class PickerCategory(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    GALLERY("相册", Icons.Default.PhotoLibrary),
    FILE("文件", Icons.AutoMirrored.Filled.InsertDriveFile),
    LOCATION("位置", Icons.Default.LocationOn),
    MUSIC("音乐", Icons.Default.MusicNote)
}

data class MediaItem(
    val id: Long,
    val name: String,
    val uri: Uri,
    val mimeType: String,
    val size: Long,
    val dateTaken: Long,
    val isVideo: Boolean
) {
    fun toPickedFile(): File {
        return File(
            name = name,
            path = uri.toString(),
            mimeType = mimeType,
            size = size,
            data = FileData.Path(uri.toString())
        )
    }
}

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
actual fun MediaPickerScreen(
    onDismiss: () -> Unit,
    onMediaSelected: (List<File>) -> Unit
) {
    var permissionGranted by remember { mutableStateOf(false) }

    val permissions = remember {
        val list = mutableListOf(Manifest.permission.CAMERA)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            list.add(Manifest.permission.READ_MEDIA_IMAGES)
            list.add(Manifest.permission.READ_MEDIA_VIDEO)
        } else {
            list.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        list
    }

    TryGetMultiplePermissions(
        permissions = permissions,
        onAllGranted = { permissionGranted = true },
        onRequest = { Napier.d("request media permissions") },
        onAnyDenied = { permissionGranted = false }
    )

    if (permissionGranted) {
        MediaPickerBottomSheet(
            onDismiss = onDismiss,
            onMediaSelected = onMediaSelected
        )
    }
}

/***
 * Picker Bottom Sheet
 *
 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun MediaPickerBottomSheet(
    onDismiss: () -> Unit,
    onMediaSelected: (List<File>) -> Unit
) {
    val context = LocalContext.current
    var category by remember { mutableStateOf(PickerCategory.GALLERY) }
    var mediaItems by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    var selectedIds by remember { mutableStateOf(setOf<Long>()) }
    var previewIndex by remember { mutableStateOf<Int?>(null) }
    var showDiscardDialog by remember { mutableStateOf(false) }

    val handleDismiss = {
        if (selectedIds.isNotEmpty()) {
            showDiscardDialog = true
        } else {
            onDismiss()
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            Napier.d("Captured camera photo successfully.")
        }
    }

    LaunchedEffect(Unit) {
        mediaItems = loadMediaItems(context)
    }

    ModalBottomSheet(
        onDismissRequest = handleDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color(0xFFF8FAFC)
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight(0.9f)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "选取附件",
                        style = MaterialTheme.typography.titleLarge,
                        color = ThemeTokens.TextMain,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (selectedIds.isEmpty()) "选择图片、视频或拍摄"
                        else "已选择 ${selectedIds.size} 项",
                        style = MaterialTheme.typography.bodySmall,
                        color = ThemeTokens.TextSecondary,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                Surface(
                    shape = CircleShape,
                    color = Color.White,
                    tonalElevation = 2.dp,
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        IconButton(onClick = handleDismiss) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "关闭",
                                tint = ThemeTokens.TextMain
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Content Area depending on Category
            Box(modifier = Modifier.weight(1f).padding(horizontal = 14.dp)) {
                when (category) {
                    PickerCategory.GALLERY -> {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
                            contentPadding = PaddingValues(bottom = 80.dp), // Space for bottom bar
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // Camera Item
                            item {
                                CameraPreviewItem(
                                    onClick = { cameraLauncher.launch(null) }
                                )
                            }
                            
                            items(mediaItems, key = { it.id }) { item ->
                                MediaGridItem(
                                    item = item,
                                    isSelected = selectedIds.contains(item.id),
                                    onClick = {
                                        if (selectedIds.isNotEmpty()) {
                                            selectedIds = selectedIds.toggle(item.id)
                                        } else {
                                            previewIndex = mediaItems.indexOfFirst { it.id == item.id }.takeIf { it >= 0 }
                                        }
                                    },
                                    onLongPress = {
                                        selectedIds = selectedIds.toggle(item.id)
                                    },
                                    onToggleSelected = {
                                        selectedIds = selectedIds.toggle(item.id)
                                    }
                                )
                            }
                        }
                        
                        if (selectedIds.isNotEmpty()) {
                            FloatingActionButton(
                                onClick = {
                                    val files = mediaItems
                                        .filter { selectedIds.contains(it.id) }
                                        .sortedByDescending { it.dateTaken }
                                        .map { it.toPickedFile() }
                                    onMediaSelected(files)
                                    onDismiss()
                                },
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(bottom = 16.dp, end = 16.dp),
                                containerColor = ThemeTokens.PrimaryBlue,
                                contentColor = Color.White,
                                shape = CircleShape
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = "发送",
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                    else -> {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    category.icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp),
                                    tint = ThemeTokens.TextMuted.copy(alpha = 0.5f)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    "${category.label} 选择器开发中...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = ThemeTokens.TextSecondary
                                )
                            }
                        }
                    }
                }
            }
            
            // Bottom Category Tabs (hidden when items are selected)
            if (selectedIds.isEmpty()) {
                Surface(
                    color = Color.White,
                    tonalElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PickerCategory.values().forEach { cat ->
                            val isSelected = category == cat
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { category = cat }
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                Icon(
                                    imageVector = cat.icon,
                                    contentDescription = cat.label,
                                    tint = if (isSelected) ThemeTokens.PrimaryBlue else ThemeTokens.TextMuted,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = cat.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isSelected) ThemeTokens.PrimaryBlue else ThemeTokens.TextMuted,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Discard Dialog
    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text("放弃选择") },
            text = { Text("您已经选择了一些项目，确定要放弃吗？") },
            confirmButton = {
                TextButton(onClick = {
                    showDiscardDialog = false
                    onDismiss()
                }) {
                    Text("放弃", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = false }) {
                    Text("继续选择")
                }
            }
        )
    }

    // Preview
    previewIndex?.let { index ->
        MediaPreviewDialog(
            items = mediaItems,
            initialIndex = index,
            selectedIds = selectedIds,
            onDismiss = { previewIndex = null },
            onToggleSelected = { item -> selectedIds = selectedIds.toggle(item.id) },
            onSend = { selectedItems ->
                val files = selectedItems.map { it.toPickedFile() }
                onMediaSelected(files)
                onDismiss()
            }
        )
    }
}

@Composable
fun CameraPreviewItem(onClick: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(18.dp))
            .background(Color.Black)
            .clickable(onClick = onClick)
    ) {
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            modifier = Modifier.fillMaxSize(),
            update = { previewView ->
                val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }
                    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                    try {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            preview
                        )
                    } catch (exc: Exception) {
                        Napier.e("Use case binding failed", exc)
                    }
                }, ContextCompat.getMainExecutor(context))
            }
        )
        // Overlay a camera icon
        Icon(
            imageVector = Icons.Default.PhotoCamera,
            contentDescription = "Camera",
            tint = Color.White.copy(alpha = 0.8f),
            modifier = Modifier.align(Alignment.Center).size(36.dp)
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MediaGridItem(
    item: MediaItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
    onToggleSelected: () -> Unit
) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongPress
            )
    ) {
        if (item.isVideo) {
            VideoThumbnail(item = item, modifier = Modifier.fillMaxSize())
        } else {
            AsyncImage(
                model = item.uri,
                contentDescription = item.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        if (item.isVideo) {
            Surface(
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.38f),
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(34.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "预览视频",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp)
                .size(24.dp)
                .clip(CircleShape)
                .background(if (isSelected) ThemeTokens.PrimaryBlue else Color.Black.copy(alpha = 0.32f))
                .border(
                    width = 1.2.dp,
                    color = if (isSelected) ThemeTokens.PrimaryBlue else Color.White.copy(alpha = 0.9f),
                    shape = CircleShape
                )
                .clickable(onClick = onToggleSelected),
            contentAlignment = Alignment.Center
        ) {
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "已选中",
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Composable
private fun MediaPreviewDialog(
    items: List<MediaItem>,
    initialIndex: Int,
    selectedIds: Set<Long>,
    onDismiss: () -> Unit,
    onToggleSelected: (MediaItem) -> Unit,
    onSend: (List<MediaItem>) -> Unit
) {
    if (items.isEmpty()) return
    var currentIndex by remember(items, initialIndex) { mutableStateOf(initialIndex.coerceIn(0, items.lastIndex)) }
    val currentItem = items.getOrNull(currentIndex) ?: return
    val currentSelected = selectedIds.contains(currentItem.id)
    val sendItems = remember(selectedIds, currentItem, items) {
        items.filter { selectedIds.contains(it.id) }.ifEmpty { listOf(currentItem) }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = true)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0B1220))
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回",
                            tint = Color.White
                        )
                    }
                    Text(
                        text = "${currentIndex + 1} / ${items.size}",
                        color = Color.White,
                        style = MaterialTheme.typography.titleSmall
                    )
                    Surface(
                        shape = CircleShape,
                        color = if (currentSelected) ThemeTokens.PrimaryBlue else Color.White.copy(alpha = 0.15f),
                        modifier = Modifier
                            .size(34.dp)
                            .clickable { onToggleSelected(currentItem) }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (currentSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "取消选中",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    if (currentItem.isVideo) {
                        MediaFileView(
                            file = currentItem.toPickedFile(),
                            modifier = Modifier.fillMaxSize(),
                            size = 220.dp
                        )
                    } else {
                        AsyncImage(
                            model = currentItem.uri,
                            contentDescription = currentItem.name,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = currentItem.name,
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(onClick = { onSend(sendItems) }) {
                        Text("发送 ${sendItems.size}")
                    }
                }
            }

            if (currentIndex > 0) {
                PreviewNavButton(
                    modifier = Modifier.align(Alignment.CenterStart),
                    isPrevious = true,
                    onClick = { currentIndex -= 1 }
                )
            }
            if (currentIndex < items.lastIndex) {
                PreviewNavButton(
                    modifier = Modifier.align(Alignment.CenterEnd),
                    isPrevious = false,
                    onClick = { currentIndex += 1 }
                )
            }
        }
    }
}

@Composable
private fun PreviewNavButton(
    modifier: Modifier = Modifier,
    isPrevious: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = CircleShape,
        color = Color.Black.copy(alpha = 0.32f),
        modifier = modifier
            .padding(horizontal = 12.dp)
            .size(42.dp)
            .clickable(onClick = onClick)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = if (isPrevious) Icons.AutoMirrored.Filled.ArrowBack else Icons.Default.PlayArrow,
                contentDescription = "切换预览",
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun VideoThumbnail(
    item: MediaItem,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val thumbnail by produceState<Bitmap?>(initialValue = null, item.uri) {
        value = withContext(Dispatchers.IO) { loadVideoThumbnail(context, item.uri) }
    }

    Box(modifier = modifier.background(Color(0xFF0F172A))) {
        if (thumbnail != null) {
            Image(
                bitmap = thumbnail!!.asImageBitmap(),
                contentDescription = item.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

private suspend fun loadMediaItems(
    context: Context
): List<MediaItem> = withContext(Dispatchers.IO) {
    val projection = arrayOf(
        MediaStore.Files.FileColumns._ID,
        MediaStore.Files.FileColumns.DISPLAY_NAME,
        MediaStore.Files.FileColumns.MIME_TYPE,
        MediaStore.Files.FileColumns.SIZE,
        MediaStore.Files.FileColumns.DATE_ADDED,
        MediaStore.Files.FileColumns.MEDIA_TYPE
    )

    val selection = "${MediaStore.Files.FileColumns.MEDIA_TYPE}=? OR ${MediaStore.Files.FileColumns.MEDIA_TYPE}=?"

    val selectionArgs = arrayOf(
        MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE.toString(),
        MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO.toString()
    )

    val sortOrder = "${MediaStore.Files.FileColumns.DATE_ADDED} DESC"
    val mediaItems = mutableListOf<MediaItem>()

    context.contentResolver.query(
        MediaStore.Files.getContentUri("external"),
        projection,
        selection,
        selectionArgs,
        sortOrder
    )?.use { cursor ->
        val idIndex = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
        val nameIndex = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
        val mimeIndex = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MIME_TYPE)
        val sizeIndex = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE)
        val dateIndex = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_ADDED)
        val typeIndex = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MEDIA_TYPE)

        while (cursor.moveToNext()) {
            val id = cursor.getLong(idIndex)
            val mediaType = cursor.getInt(typeIndex)
            val contentUri = when (mediaType) {
                MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO -> {
                    Uri.withAppendedPath(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id.toString())
                }
                else -> Uri.withAppendedPath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id.toString())
            }

            mediaItems += MediaItem(
                id = id,
                name = cursor.getString(nameIndex) ?: "媒体",
                uri = contentUri,
                mimeType = cursor.getString(mimeIndex) ?: "application/octet-stream",
                size = cursor.getLong(sizeIndex),
                dateTaken = cursor.getLong(dateIndex),
                isVideo = mediaType == MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO
            )
        }
    }

    mediaItems
}

private fun loadVideoThumbnail(context: Context, uri: Uri): Bitmap? {
    return runCatching {
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(context, uri)
        val frame = retriever.frameAtTime
        retriever.release()
        frame
    }.getOrNull()
}

private fun Set<Long>.toggle(id: Long): Set<Long> {
    return if (contains(id)) this - id else this + id
}
