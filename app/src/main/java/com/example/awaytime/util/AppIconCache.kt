package com.example.awaytime.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap

/**
 * High-performance in-memory cache for app icons to prevent blocking IPC
 * calls and bitmap reallocation during fast list scrolling.
 */
object AppIconCache {
    private val memoryCache = LruCache<String, ImageBitmap>(250)

    fun get(packageName: String): ImageBitmap? {
        return memoryCache.get(packageName)
    }

    fun put(packageName: String, bitmap: ImageBitmap) {
        memoryCache.put(packageName, bitmap)
    }

    fun getOrCreate(packageName: String, drawable: Drawable?): ImageBitmap? {
        if (drawable == null) return null
        val cached = memoryCache.get(packageName)
        if (cached != null) return cached

        return try {
            val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth.coerceAtMost(96) else 48
            val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight.coerceAtMost(96) else 48
            val bm = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bm)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            val imageBitmap = bm.asImageBitmap()
            memoryCache.put(packageName, imageBitmap)
            imageBitmap
        } catch (e: Exception) {
            null
        }
    }

    fun getOrLoadFromPackageManager(context: Context, packageName: String): ImageBitmap? {
        val cached = memoryCache.get(packageName)
        if (cached != null) return cached

        return try {
            val drawable = context.packageManager.getApplicationIcon(packageName)
            getOrCreate(packageName, drawable)
        } catch (e: Exception) {
            null
        }
    }

    fun clear() {
        memoryCache.evictAll()
    }
}
