package com.github.im.group.ui.chat

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.github.im.group.sdk.PickedFile
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class MediaItem(
    val id: Long,
    val name: String,
    val uri: Uri,
    val mimeType: String,
    val size: Long,
    val dateTaken: Long
)

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
actual fun MediaPickerScreen(
    onDismiss: () -> Unit,
    onMediaSelected: (List<PickedFile>) -> Unit,
    mediaType: String // "image" or "video"
) {
    val context = LocalContext.current
    val permission = if (mediaType == "image") {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
    } else {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_VIDEO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
    }
    
    val permissionState = rememberPermissionState(permission)
    var mediaItems by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    var selectedItems by remember { mutableStateOf<Set<MediaItem>>(emptySet()) }
    var isLoading by remember { mutableStateOf(true) }
    
    LaunchedEffect(Unit) {
        if (permissionState.status.isGranted) {
            loadMediaItems(context, mediaType) { items ->
                mediaItems = items
                isLoading = false
            }
        } else {
            permissionState.launchPermissionRequest()
            isLoading = false
        }
    }
    
    LaunchedEffect(permissionState.status.isGranted) {
        if (permissionState.status.isGranted) {
            loadMediaItems(context, mediaType) { items ->
                mediaItems = items
                isLoading = false
            }
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = if (mediaType == "image") "选择图片" else "选择视频",
                        color = Color.White
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    if (selectedItems.isNotEmpty()) {
                        IconButton(onClick = {
                            val pickedFiles = selectedItems.map { mediaItem ->
                                PickedFile(
                                    name = mediaItem.name,
                                    path = mediaItem.uri.toString(),
                                    mimeType = mediaItem.mimeType,
                                    size = mediaItem.size
                                )
                            }
                            onMediaSelected(pickedFiles)
                            onDismiss()
                        }) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(end = 8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "发送",
                                    tint = Color.White
                                )
                                if (selectedItems.size > 1) {
                                    Text(
                                        text = "${selectedItems.size}",
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        modifier = Modifier.padding(start = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0088CC)
                )
            )
        }
    ) { paddingValues ->
        if (!permissionState.status.isGranted) {
            PermissionRequestScreen(
                permission = permission,
                onPermissionResult = { granted ->
                    if (!granted) {
                        // 用户拒绝权限，显示提示信息
                        onDismiss()
                    }
                }
            )
        } else if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            MediaGrid(
                mediaItems = mediaItems,
                selectedItems = selectedItems,
                onSelectionChanged = { selectedItems = it },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            )
        }
    }
}

@Composable
fun PermissionRequestScreen(
    permission: String,
    onPermissionResult: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Image,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(64.dp)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "需要访问${if (permission.contains("image")) "图片" else "视频"}权限",
            style = MaterialTheme.typography.headlineSmall
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "请允许访问${if (permission.contains("image")) "图片" else "视频"}以选择文件",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Button(
            onClick = {
                // 这里应该引导用户到设置页面
                onPermissionResult(false)
            }
        ) {
            Text("去设置")
        }
    }
}

@Composable
fun MediaGrid(
    mediaItems: List<MediaItem>,
    selectedItems: Set<MediaItem>,
    onSelectionChanged: (Set<MediaItem>) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = modifier,
        contentPadding = PaddingValues(4.dp)
    ) {
        items(mediaItems) { mediaItem ->
            MediaItemView(
                mediaItem = mediaItem,
                isSelected = selectedItems.contains(mediaItem),
                onSelected = { selected ->
                    val newSelectedItems = if (selected) {
                        selectedItems + mediaItem
                    } else {
                        selectedItems - mediaItem
                    }
                    onSelectionChanged(newSelectedItems)
                }
            )
        }
    }
}

@Composable
fun MediaItemView(
    mediaItem: MediaItem,
    isSelected: Boolean,
    onSelected: (Boolean) -> Unit
) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .clickable { onSelected(!isSelected) }
    ) {
        AsyncImage(
            model = mediaItem.uri,
            contentDescription = mediaItem.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        
        if (isSelected) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(4.dp),
                contentAlignment = Alignment.TopEnd
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(Color(0xFF0088CC), shape = androidx.compose.foundation.shape.CircleShape),
                    contentAlignment = Alignment.Center
                ) {
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

private suspend fun loadMediaItems(
    context: Context,
    mediaType: String,
    onResult: (List<MediaItem>) -> Unit
) = withContext(Dispatchers.IO) {
    val items = mutableListOf<MediaItem>()
    
    val projection = arrayOf(
        MediaStore.MediaColumns._ID,
        MediaStore.MediaColumns.DISPLAY_NAME,
        MediaStore.MediaColumns.MIME_TYPE,
        MediaStore.MediaColumns.SIZE,
        MediaStore.MediaColumns.DATE_TAKEN
    )
    
    val selection = if (mediaType == "image") {
        "${MediaStore.Files.FileColumns.MEDIA_TYPE} = ?"
    } else {
        "${MediaStore.Files.FileColumns.MEDIA_TYPE} = ?"
    }
    
    val selectionArgs = if (mediaType == "image") {
        arrayOf(MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE.toString())
    } else {
        arrayOf(MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO.toString())
    }
    
    val sortOrder = "${MediaStore.MediaColumns.DATE_TAKEN} DESC"
    
    val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL)
    } else {
        MediaStore.Files.getContentUri("external")
    }
    
    try {
        val cursor: Cursor? = context.contentResolver.query(
            collection,
            projection,
            selection,
            selectionArgs,
            sortOrder
        )
        
        cursor?.use { c ->
            val idColumn = c.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
            val nameColumn = c.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
            val mimeColumn = c.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE)
            val sizeColumn = c.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
            val dateTakenColumn = c.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_TAKEN)
            
            while (c.moveToNext()) {
                val id = c.getLong(idColumn)
                val name = c.getString(nameColumn)
                val mimeType = c.getString(mimeColumn)
                val size = c.getLong(sizeColumn)
                val dateTaken = c.getLong(dateTakenColumn)
                
                val contentUri = if (mediaType == "image") {
                    ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
                } else {
                    ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)
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
            }
        }
    } catch (e: SecurityException) {
        e.printStackTrace()
    } catch (e: Exception) {
        e.printStackTrace()
    }
    
    withContext(Dispatchers.Main) {
        onResult(items)
    }
}