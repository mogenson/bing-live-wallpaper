package com.binglivewallpaper

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.service.wallpaper.WallpaperService
import android.util.Log
import android.view.SurfaceHolder
import kotlin.math.max

class BingWallpaperService : WallpaperService() {

    override fun onCreateEngine(): Engine = BingEngine()

    inner class BingEngine : Engine() {

        @Volatile
        private var bitmap: Bitmap? = null

        private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

        override fun onCreate(surfaceHolder: SurfaceHolder?) {
            super.onCreate(surfaceHolder)
            Log.d(TAG, "Engine onCreate")
            // Ensure the daily refresh work is scheduled whenever the
            // wallpaper becomes active.
            BingRefreshWorker.schedule(this@BingWallpaperService)
        }

        override fun onSurfaceCreated(holder: SurfaceHolder) {
            super.onSurfaceCreated(holder)
            Log.d(TAG, "onSurfaceCreated")
            val dm = resources.displayMetrics
            bitmap = ImageStore.load(this@BingWallpaperService, dm.widthPixels, dm.heightPixels)
            Log.d(TAG, "Loaded cached bitmap: ${bitmap?.let { "${it.width}x${it.height}" } ?: "none"}")
            if (bitmap == null) {
                Log.d(TAG, "No cached image — kicking off immediate fetch")
                BingRefreshWorker.runOnceNow(this@BingWallpaperService)
            }
            draw()
        }

        override fun onSurfaceChanged(
            holder: SurfaceHolder,
            format: Int,
            width: Int,
            height: Int,
        ) {
            super.onSurfaceChanged(holder, format, width, height)
            Log.d(TAG, "onSurfaceChanged ${width}x${height}")
            draw()
        }

        override fun onVisibilityChanged(visible: Boolean) {
            super.onVisibilityChanged(visible)
            Log.d(TAG, "onVisibilityChanged visible=$visible")
            if (visible) {
                // Refresh from disk in case a new image was downloaded
                // while we were hidden.
                val dm = resources.displayMetrics
                bitmap = ImageStore.load(this@BingWallpaperService, dm.widthPixels, dm.heightPixels)
                draw()
            }
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            super.onSurfaceDestroyed(holder)
            Log.d(TAG, "onSurfaceDestroyed")
            bitmap = null
        }

        private fun draw() {
            val holder = surfaceHolder ?: run {
                Log.w(TAG, "draw: surfaceHolder is null")
                return
            }
            val canvas: Canvas = holder.lockCanvas() ?: run {
                Log.w(TAG, "draw: lockCanvas returned null")
                return
            }
            try {
                canvas.drawColor(Color.BLACK)
                val bmp = bitmap
                if (bmp != null) {
                    val dest = scaledDestRect(bmp, canvas.width, canvas.height)
                    Log.d(TAG, "draw: canvas=${canvas.width}x${canvas.height} dest=$dest")
                    canvas.drawBitmap(bmp, null, dest, paint)
                } else {
                    Log.d(TAG, "draw: no bitmap, black screen")
                }
            } finally {
                holder.unlockCanvasAndPost(canvas)
            }
        }

        /**
         * Scales the bitmap so that the larger screen dimension is fully
         * covered while preserving aspect ratio, and centers the result.
         */
        private fun scaledDestRect(bmp: Bitmap, screenW: Int, screenH: Int): Rect {
            val dominant = max(screenW, screenH).toFloat()
            val scaleW = dominant / bmp.width
            val scaleH = dominant / bmp.height
            // Use the larger scale so the dominant dimension is filled.
            val scale = max(scaleW, scaleH)
            val scaledW = (bmp.width * scale).toInt()
            val scaledH = (bmp.height * scale).toInt()
            val left = (screenW - scaledW) / 2
            val top = (screenH - scaledH) / 2
            return Rect(left, top, left + scaledW, top + scaledH)
        }
    }

    companion object {
        private const val TAG = "BingWallpaperService"
    }
}
