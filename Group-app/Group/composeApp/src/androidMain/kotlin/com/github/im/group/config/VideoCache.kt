package com.github.im.group.config

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.annotation.OptIn
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import java.io.File
import java.util.WeakHashMap
import android.util.LruCache
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.core.graphics.drawable.toBitmap

@UnstableApi
object VideoCache {
    private var instance: SimpleCache? = null

    fun getInstance(context: Context): SimpleCache {
        if (instance == null) {
            val cacheDir = File(context.cacheDir, "video_cache")
            val evictor = LeastRecentlyUsedCacheEvictor(500L * 1024L * 1024L) // 增加到500MB以支持大视频文件
            instance = SimpleCache(cacheDir, evictor, StandaloneDatabaseProvider(context))
        }
        return instance!!
    }
    
    // 全局视频缩略图缓存
    private val thumbnailCache = LruCache<String, Bitmap>(20 * 1024 * 1024) // 20MB
    
    // 用于存储ImageBitmap的弱引用缓存，防止内存泄漏
    private val imageBitmapCache = WeakHashMap<String, ImageBitmap>()
    
    /**
     * 获取缩略图，优先从缓存获取
     */
    fun getThumbnail(context: Context, path: String): ImageBitmap? {
        val key = getThumbnailKey(path)
        
        // 先检查ImageBitmap缓存
        imageBitmapCache[key]?.let { return it }
        
        // 检查Bitmap缓存
        var bitmap = thumbnailCache.get(key)
        if (bitmap == null) {
            // 从文件提取并缓存
            bitmap = extractVideoThumbnail(context, path)
            bitmap?.let { thumbnailCache.put(key, it) }
        }
        
        return bitmap?.asImageBitmap()?.also { 
            imageBitmapCache[key] = it 
        }
    }
    
    /**
     * 生成缩略图缓存键
     */
    private fun getThumbnailKey(path: String): String {
        return path.hashCode().toString()
    }
    
    /**
     * 安全提取视频缩略图（只取1/4尺寸以节省内存）
     */
    private fun extractVideoThumbnail(context: Context, path: String): Bitmap? {
        val retriever = MediaMetadataRetriever()
        return try {
            when {
                path.startsWith("content://") -> {
                    retriever.setDataSource(context, Uri.parse(path))
                }
                path.startsWith("http://") || path.startsWith("https://") -> {
                    // 对于网络视频，设置认证信息
                    val token = com.github.im.group.GlobalCredentialProvider.currentToken
                    val headers = mapOf(
                        "Authorization" to "Bearer $token",
                        "User-Agent" to "MyAppPlayer/1.0"
                    )
                    retriever.setDataSource(path, headers)
                }
                else -> {
                    retriever.setDataSource(path)
                }
            }
            
            val frame = retriever.getFrameAtTime(
                0,
                MediaMetadataRetriever.OPTION_CLOSEST_SYNC
            )
            
            frame?.let {
                // 缩放为原图的1/4大小以节省内存
                val scaledWidth = (it.width / 4).coerceAtLeast(1)
                val scaledHeight = (it.height / 4).coerceAtLeast(1)
                Bitmap.createScaledBitmap(it, scaledWidth, scaledHeight, true)
            }
        } catch (e: Exception) {
            null
        } finally {
            try {
                retriever.release()
            } catch (e: Exception) {
                // 忽略释放异常
            }
        }
    }
    
    /**
     * 清除缩略图缓存
     */
    fun clearThumbnailCache() {
        thumbnailCache.evictAll()
        imageBitmapCache.clear()
    }
    
    /**
     * 保存缩略图到缓存
     */
    fun saveThumbnail(context: Context, path: String, imageBitmap: ImageBitmap) {
        val key = getThumbnailKey(path)
        
        // 将ImageBitmap转换为Bitmap并缓存
        val bitmap = imageBitmap.asAndroidBitmap()
        thumbnailCache.put(key, bitmap)
        
        // 同时缓存ImageBitmap
        imageBitmapCache[key] = imageBitmap
    }
}