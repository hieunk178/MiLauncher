package com.milauncher.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.widget.ImageView
import androidx.lifecycle.LifecycleCoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream

/**
 * Manages wallpaper auto-fetch from Unsplash every 10 minutes.
 * Categories: nature, architecture, city, space, abstract
 */
class WallpaperManager(
    private val context: Context,
    private val settings: SettingsManager
) {
    private val httpClient = OkHttpClient()
    private var wallpaperJob: Job? = null
    private val cacheFile = File(context.cacheDir, "wallpaper_cache.jpg")

    // Use LoremFlickr since Unsplash Source API is discontinued
    private fun buildUrl(): String {
        val cat = settings.wallpaperCategory
        // 1920x1080 resolution, random landscape photo
        return "https://loremflickr.com/1920/1080/$cat,landscape/all"
    }

    fun start(lifecycleScope: LifecycleCoroutineScope, wallpaperView: ImageView) {
        wallpaperJob?.cancel()
        wallpaperJob = lifecycleScope.launch {
            // Load cached immediately
            loadCached(wallpaperView)
            while (isActive) {
                fetchAndApply(wallpaperView)
                delay(10 * 60 * 1000L) // 10 minutes
            }
        }
    }

    fun stop() {
        wallpaperJob?.cancel()
    }

    private suspend fun loadCached(wallpaperView: ImageView) {
        if (cacheFile.exists()) {
            withContext(Dispatchers.Main) {
                try {
                    val bmp = BitmapFactory.decodeFile(cacheFile.absolutePath)
                    if (bmp != null) {
                        wallpaperView.setImageBitmap(bmp)
                    }
                } catch (e: Exception) { /* ignore */ }
            }
        }
    }

    private suspend fun fetchAndApply(wallpaperView: ImageView) {
        withContext(Dispatchers.IO) {
            try {
                // Add a random timestamp to avoid caching issues with loremflickr
                val url = buildUrl() + "?random=${System.currentTimeMillis()}"
                val request = Request.Builder().url(url).build()
                val response = httpClient.newCall(request).execute()
                if (response.isSuccessful) {
                    val bytes = response.body?.bytes() ?: return@withContext
                    val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    // Only apply if the image is high resolution (prevent blurry error placeholders)
                    if (bmp != null && bmp.width >= 1280 && bmp.height >= 720) {
                        // Cache to disk
                        FileOutputStream(cacheFile).use { it.write(bytes) }
                        // Apply with crossfade
                        withContext(Dispatchers.Main) {
                            wallpaperView.animate().alpha(0f).setDuration(500).withEndAction {
                                wallpaperView.setImageBitmap(bmp)
                                wallpaperView.animate().alpha(1f).setDuration(800).start()
                            }.start()
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Force refresh now (called from settings)
     */
    fun refreshNow(lifecycleScope: LifecycleCoroutineScope, wallpaperView: ImageView) {
        lifecycleScope.launch {
            fetchAndApply(wallpaperView)
        }
    }
}
