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

    fun savedDate(context: Context): String? =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_DATE, null)

    fun savedUrl(context: Context): String? =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_URL, null)

    private fun imageFile(context: Context): File =
        File(context.filesDir, IMAGE_FILE)
}
