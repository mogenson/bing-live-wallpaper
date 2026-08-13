package com.binglivewallpaper

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object BingImageFetcher {

    private const val TAG = "BingImageFetcher"

    private const val JSON_URL =
        "https://www.bing.com/HPImageArchive.aspx?format=js&uhd=1&idx=0&n=1&mkt=en-US"
    private const val BASE_URL = "https://bing.com"
    private const val IMAGE_SUFFIX = "_UHD.jpg"
    private const val CONNECT_TIMEOUT_MS = 15_000
    private const val READ_TIMEOUT_MS = 30_000

    data class Result(
        val bitmap: Bitmap,
        val url: String,
        val date: String,
    )

    /**
     * Fetches the Bing image-of-the-day metadata, downloads the UHD image,
     * and returns the decoded [Bitmap]. Throws on any failure or if the
     * fetched image startdate is earlier than today's UTC date.
     */
    fun fetch(): Result {
        Log.d(TAG, "Fetching JSON from $JSON_URL")
        val jsonText = httpGet(JSON_URL)
        Log.d(TAG, "Got JSON (${jsonText.length} bytes)")
        val json = JSONObject(jsonText)
        val images = json.getJSONArray("images")
        val image = images.getJSONObject(0)
        val urlbase = image.getString("urlbase")
        val date = image.optString("startdate", "")
        val imageUrl = BASE_URL + urlbase + IMAGE_SUFFIX
        Log.d(TAG, "Image URL: $imageUrl, startdate: $date")

        val todayUtc = getTodayUtcDateString()
        if (date.isNotEmpty() && date < todayUtc) {
            Log.w(TAG, "Bing returned startdate $date which is earlier than today's UTC date $todayUtc")
            throw IllegalStateException("Bing image of the day for $todayUtc is not available yet (got startdate=$date).")
        }

        val bitmap = downloadBitmap(imageUrl)
            ?: throw IllegalStateException("Failed to decode image from $imageUrl")
        Log.d(TAG, "Decoded bitmap: ${bitmap.width}x${bitmap.height}")
        return Result(bitmap = bitmap, url = imageUrl, date = date)
    }

    /**
     * Fetches the image and saves it as a JPEG at [dest].
     */
    fun fetchToFile(dest: File): Result {
        val result = fetch()
        FileOutputStream(dest).use { out ->
            result.bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
        }
        return result
    }

    internal fun getTodayUtcDateString(): String {
        val sdf = SimpleDateFormat("yyyyMMdd", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        return sdf.format(Date())
    }

    private fun httpGet(url: String): String {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
            requestMethod = "GET"
            useCaches = false
            defaultUseCaches = false
            setRequestProperty("Cache-Control", "no-cache, no-store, must-revalidate")
            setRequestProperty("Pragma", "no-cache")
        }
        try {
            val code = conn.responseCode
            if (code != HttpURLConnection.HTTP_OK) {
                Log.e(TAG, "HTTP $code for $url")
                throw IllegalStateException("HTTP $code for $url")
            }
            return conn.inputStream.bufferedReader().use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }

    private fun downloadBitmap(url: String): Bitmap? {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
            requestMethod = "GET"
            useCaches = false
            defaultUseCaches = false
            setRequestProperty("Cache-Control", "no-cache, no-store, must-revalidate")
            setRequestProperty("Pragma", "no-cache")
        }
        try {
            val code = conn.responseCode
            if (code != HttpURLConnection.HTTP_OK) {
                Log.e(TAG, "Image HTTP $code for $url")
                return null
            }
            return conn.inputStream.use { BitmapFactory.decodeStream(it) }
        } finally {
            conn.disconnect()
        }
    }
}

