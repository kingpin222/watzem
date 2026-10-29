package com.watzem.nativeapp.data.cache

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import java.io.File

@OptIn(UnstableApi::class)
object VideoCacheManager {
    private const val MAX_CACHE_SIZE_BYTES: Long = 512 * 1024 * 1024 // 512 MB LRU disk cache
    private const val CACHE_DIR_NAME = "media_stream_cache"

    @Volatile
    private var simpleCache: SimpleCache? = null

    @Volatile
    private var databaseProvider: StandaloneDatabaseProvider? = null

    @Synchronized
    fun getCache(context: Context): SimpleCache {
        val appContext = context.applicationContext
        if (simpleCache == null) {
            val cacheFolder = File(appContext.cacheDir, CACHE_DIR_NAME)
            if (!cacheFolder.exists()) {
                cacheFolder.mkdirs()
            }
            val evictor = LeastRecentlyUsedCacheEvictor(MAX_CACHE_SIZE_BYTES)
            val dbProvider = databaseProvider ?: StandaloneDatabaseProvider(appContext).also {
                databaseProvider = it
            }
            simpleCache = SimpleCache(cacheFolder, evictor, dbProvider)
        }
        return simpleCache!!
    }

    fun getCacheDataSourceFactory(
        context: Context,
        headers: Map<String, String> = emptyMap()
    ): CacheDataSource.Factory {
        val upstreamFactory = DefaultHttpDataSource.Factory()
            .setUserAgent(
                headers["User-Agent"]
                    ?: "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Mobile Safari/537.36"
            )
            .setConnectTimeoutMs(15000)
            .setReadTimeoutMs(15000)
            .setAllowCrossProtocolRedirects(true)

        if (headers.isNotEmpty()) {
            upstreamFactory.setDefaultRequestProperties(headers)
        }

        return CacheDataSource.Factory()
            .setCache(getCache(context))
            .setUpstreamDataSourceFactory(upstreamFactory)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
    }

    @Synchronized
    fun clearCache(context: Context) {
        simpleCache?.let { cache ->
            try {
                for (key in cache.keys) {
                    cache.removeResource(key)
                }
            } catch (_: Exception) {
                // Ignore cleanup errors
            }
        }
    }
}

