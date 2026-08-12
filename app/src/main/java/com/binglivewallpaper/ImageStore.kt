package com.binglivewallpaper

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.File

/**
 * Persists the last downloaded Bing image and its metadata so the
 * wallpaper service can render without a network round-trip.
 */
object ImageStore {

    private const val PREFS = "bing_image_store"
    private const val KEY_URL = "url"
    private const val KEY_DATE = "date"
    private const val IMAGE_FILE = "bing_image.jpg"

    fun save(context: Context, bitmap: Bitmap, url: String, date: String) {
        imageFile(context).outputStream().use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(KEY_URL, url)
            .putString(KEY_DATE, date)
            .apply()
    }

    fun load(context: Context): Bitmap? {
        val file = imageFile(context)
        if (!file.exists()) return null
        return BitmapFactory.decodeFile(file.absolutePath)
    }

    /**
     * Loads the cached image, downsampling to approximately [reqWidth]×[reqHeight]
     * to avoid allocating a full-resolution bitmap on low-memory devices.
     */
    fun load(context: Context, reqWidth: Int, reqHeight: Int): Bitmap? {
        val file = imageFile(context)
        if (!file.exists()) return null

        // First pass: read bounds only.
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        // Second pass: decode with subsampling.
        val opts = BitmapFactory.Options().apply {
            inSampleSize = calculateInSampleSize(bounds, reqWidth, reqHeight)
        }
        return BitmapFactory.decodeFile(file.absolutePath, opts)
    }

    private fun calculateInSampleSize(
        options: BitmapFactory.Options,
        reqWidth: Int,
        reqHeight: Int,
    ): Int {
        val (width, height) = options.outWidth to options.outHeight
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            // Largest power-of-2 that keeps both dimensions >= requested size.
            while (halfHeight / inSampleSize >= reqHeight &&
                halfWidth / inSampleSize >= reqWidth
            ) {
                inSampleSize *= 2
            }
        }
        return inSampleSize.coerceAtLeast(1)
    }

    fun savedDate(context: Context): String? =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_DATE, null)

    fun savedUrl(context: Context): String? =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_URL, null)

    private fun imageFile(context: Context): File =
        File(context.filesDir, IMAGE_FILE)
}
