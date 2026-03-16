package com.kgtts.app.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.util.LruCache
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import java.io.File
import kotlin.math.max

object QuickCardRenderCache {
    private const val defaultImageDecodeSizePx = 1600
    private const val defaultQrSizePx = 640

    private val imageCache = object : LruCache<String, Bitmap>(max(8 * 1024, cacheSizeKb() / 12)) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount / 1024
    }

    private val qrCache = object : LruCache<String, Bitmap>(max(4 * 1024, cacheSizeKb() / 24)) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount / 1024
    }

    fun loadImage(path: String, maxDimensionPx: Int = defaultImageDecodeSizePx): Bitmap? {
        val normalized = path.trim()
        if (normalized.isEmpty()) return null
        synchronized(imageCache) {
            imageCache.get(normalized)?.let { return it }
        }
        val file = File(normalized)
        if (!file.exists()) return null
        val bitmap = decodeSampledBitmap(file, maxDimensionPx) ?: return null
        synchronized(imageCache) {
            imageCache.put(normalized, bitmap)
        }
        return bitmap
    }

    fun loadQr(content: String, sizePx: Int = defaultQrSizePx): Bitmap? {
        val normalized = content.trim()
        if (normalized.isEmpty()) return null
        synchronized(qrCache) {
            qrCache.get(normalized)?.let { return it }
        }
        val bitmap = runCatching {
            val matrix = QRCodeWriter().encode(normalized, BarcodeFormat.QR_CODE, sizePx, sizePx)
            Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888).apply {
                for (x in 0 until sizePx) {
                    for (y in 0 until sizePx) {
                        setPixel(x, y, if (matrix.get(x, y)) Color.BLACK else Color.WHITE)
                    }
                }
            }
        }.getOrNull() ?: return null
        synchronized(qrCache) {
            qrCache.put(normalized, bitmap)
        }
        return bitmap
    }

    private fun decodeSampledBitmap(file: File, maxDimensionPx: Int): Bitmap? {
        val safeMax = maxDimensionPx.coerceAtLeast(256)
        val bounds = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        val sample = computeInSampleSize(bounds.outWidth, bounds.outHeight, safeMax)
        val opts = BitmapFactory.Options().apply {
            inSampleSize = sample
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        return BitmapFactory.decodeFile(file.absolutePath, opts)
    }

    private fun computeInSampleSize(width: Int, height: Int, maxDimensionPx: Int): Int {
        var inSampleSize = 1
        var currentWidth = width
        var currentHeight = height
        while (currentWidth > maxDimensionPx || currentHeight > maxDimensionPx) {
            inSampleSize *= 2
            currentWidth /= 2
            currentHeight /= 2
        }
        return inSampleSize.coerceAtLeast(1)
    }

    private fun cacheSizeKb(): Int {
        val maxMemoryKb = (Runtime.getRuntime().maxMemory() / 1024L).toInt()
        return maxMemoryKb.coerceAtLeast(12 * 1024)
    }
}
