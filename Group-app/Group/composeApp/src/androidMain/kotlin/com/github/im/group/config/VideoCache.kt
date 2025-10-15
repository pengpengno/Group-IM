package com.github.im.group.config

import android.content.Context
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import java.io.File

@UnstableApi
object VideoCache {
    private var instance: SimpleCache? = null

    fun getInstance(context: Context): SimpleCache {
        if (instance == null) {

            val cacheDir = File(context.cacheDir, "video_cache")
            val evictor = LeastRecentlyUsedCacheEvictor(50L * 1024L * 1024L) // 50MB
            instance = SimpleCache(cacheDir, evictor,StandaloneDatabaseProvider(context))
        }
        return instance!!
    }
}
