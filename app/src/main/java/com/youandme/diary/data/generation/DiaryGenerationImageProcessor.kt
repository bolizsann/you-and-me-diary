package com.youandme.diary.data.generation

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Rect
import android.util.Base64
import android.util.Log
import com.youandme.diary.data.remote.DiaryRemoteImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.math.min
import kotlin.math.roundToInt

object DiaryGenerationImageProcessor {
    suspend fun estimateDominantColor(
        path: String,
        roiScale: Float,
        roiOffsetX: Float,
        roiOffsetY: Float,
    ): Long? =
        withContext(Dispatchers.IO) {
            runCatching {
                estimateDominantColorBlocking(
                    path = path,
                    roiScale = roiScale,
                    roiOffsetX = roiOffsetX,
                    roiOffsetY = roiOffsetY,
                )
            }.onFailure { error ->
                Log.w(TAG, "Dominant color estimate failed: ${error.javaClass.simpleName}: ${error.message}")
            }.getOrNull()
        }

    suspend fun buildRemoteImage(
        path: String,
        dominantColor: Long?,
        roiScale: Float,
        roiOffsetX: Float,
        roiOffsetY: Float,
    ): DiaryRemoteImage? =
        withContext(Dispatchers.IO) {
            runCatching {
                val compressed = buildCompressedJpeg(
                    path = path,
                    roiScale = roiScale,
                    roiOffsetX = roiOffsetX,
                    roiOffsetY = roiOffsetY,
                ) ?: return@runCatching null
                DiaryRemoteImage(
                    mimeType = "image/jpeg",
                    dataBase64 = Base64.encodeToString(compressed, Base64.NO_WRAP),
                    dominantColor = dominantColor?.toDiaryHexColor(),
                )
            }.onFailure { error ->
                Log.w(TAG, "Remote image preprocessing failed: ${error.javaClass.simpleName}: ${error.message}")
            }.getOrNull()
        }

    suspend fun buildLocalModelImage(
        cacheDir: File,
        path: String,
        roiScale: Float,
        roiOffsetX: Float,
        roiOffsetY: Float,
    ): String? =
        withContext(Dispatchers.IO) {
            runCatching {
                val compressed = buildCompressedJpeg(
                    path = path,
                    roiScale = roiScale,
                    roiOffsetX = roiOffsetX,
                    roiOffsetY = roiOffsetY,
                ) ?: return@runCatching null
                writeLocalModelImage(cacheDir = cacheDir, compressed = compressed)
            }.onFailure { error ->
                Log.w(TAG, "Local image preprocessing failed: ${error.javaClass.simpleName}: ${error.message}")
            }.getOrNull()
        }

    private fun estimateDominantColorBlocking(
        path: String,
        roiScale: Float,
        roiOffsetX: Float,
        roiOffsetY: Float,
    ): Long? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        val roi = calculateSquareRoi(
            imageWidth = bounds.outWidth,
            imageHeight = bounds.outHeight,
            roiScale = roiScale,
            roiOffsetX = roiOffsetX,
            roiOffsetY = roiOffsetY,
        )
        val options = BitmapFactory.Options().apply { inSampleSize = 16 }
        val bitmap = BitmapFactory.decodeFile(path, options) ?: return null
        try {
            val scaleX = bitmap.width.toFloat() / bounds.outWidth.toFloat()
            val scaleY = bitmap.height.toFloat() / bounds.outHeight.toFloat()
            val left = (roi.left * scaleX).roundToInt().coerceIn(0, bitmap.width - 1)
            val top = (roi.top * scaleY).roundToInt().coerceIn(0, bitmap.height - 1)
            val right = ((roi.left + roi.size) * scaleX).roundToInt().coerceIn(left + 1, bitmap.width)
            val bottom = ((roi.top + roi.size) * scaleY).roundToInt().coerceIn(top + 1, bitmap.height)
            var red = 0L
            var green = 0L
            var blue = 0L
            var count = 0L
            val stepX = ((right - left) / 24).coerceAtLeast(1)
            val stepY = ((bottom - top) / 24).coerceAtLeast(1)
            var y = top
            while (y < bottom) {
                var x = left
                while (x < right) {
                    val color = bitmap.getPixel(x, y)
                    red += Color.red(color)
                    green += Color.green(color)
                    blue += Color.blue(color)
                    count += 1
                    x += stepX
                }
                y += stepY
            }
            if (count == 0L) {
                return null
            }
            return 0xFF000000L or
                ((red / count).coerceIn(0, 255) shl 16) or
                ((green / count).coerceIn(0, 255) shl 8) or
                (blue / count).coerceIn(0, 255)
        } finally {
            bitmap.recycle()
        }
    }

    private fun buildCompressedJpeg(
        path: String,
        roiScale: Float,
        roiOffsetX: Float,
        roiOffsetY: Float,
    ): ByteArray? {
        val bitmap = BitmapFactory.decodeFile(path) ?: return null
        return try {
            val roi = calculateSquareRoi(
                imageWidth = bitmap.width,
                imageHeight = bitmap.height,
                roiScale = roiScale,
                roiOffsetX = roiOffsetX,
                roiOffsetY = roiOffsetY,
            )
            val src = Rect(roi.left, roi.top, roi.left + roi.size, roi.top + roi.size)
            val targetSize = minOf(roi.size, IMAGE_TARGET_SIZE).coerceAtLeast(1)
            val cropped = Bitmap.createBitmap(targetSize, targetSize, Bitmap.Config.ARGB_8888)
            try {
                Canvas(cropped).drawBitmap(bitmap, src, Rect(0, 0, targetSize, targetSize), null)
                ByteArrayOutputStream().use { output ->
                    cropped.compress(Bitmap.CompressFormat.JPEG, IMAGE_JPEG_QUALITY, output)
                    output.toByteArray()
                }
            } finally {
                cropped.recycle()
            }
        } finally {
            bitmap.recycle()
        }
    }

    private fun writeLocalModelImage(cacheDir: File, compressed: ByteArray): String {
        val directory = File(cacheDir, "local_gemma_images").apply { mkdirs() }
        val target = File(directory, "local-gemma-${System.currentTimeMillis()}.jpg")
        return try {
            target.writeBytes(compressed)
            target.absolutePath
        } catch (error: Throwable) {
            target.delete()
            throw error
        }
    }
}

internal fun Long.toDiaryHexColor(): String =
    "#%06X".format(this and 0x00FFFFFF)

private fun calculateSquareRoi(
    imageWidth: Int,
    imageHeight: Int,
    roiScale: Float,
    roiOffsetX: Float,
    roiOffsetY: Float,
): SquareRoi {
    val safeScale = roiScale.coerceIn(1f, 4f)
    val baseScale = maxOf(1f / imageWidth.toFloat(), 1f / imageHeight.toFloat())
    val renderedWidth = imageWidth * baseScale * safeScale
    val renderedHeight = imageHeight * baseScale * safeScale
    val cropSize = (1f / (baseScale * safeScale)).roundToInt().coerceAtLeast(1)
    val safeCropSize = cropSize.coerceAtMost(min(imageWidth, imageHeight))
    val left = ((renderedWidth / 2f - 0.5f - roiOffsetX) / (baseScale * safeScale))
        .roundToInt()
        .coerceIn(0, (imageWidth - safeCropSize).coerceAtLeast(0))
    val top = ((renderedHeight / 2f - 0.5f - roiOffsetY) / (baseScale * safeScale))
        .roundToInt()
        .coerceIn(0, (imageHeight - safeCropSize).coerceAtLeast(0))
    return SquareRoi(left = left, top = top, size = safeCropSize)
}

private data class SquareRoi(
    val left: Int,
    val top: Int,
    val size: Int,
)

private const val IMAGE_TARGET_SIZE = 384
private const val IMAGE_JPEG_QUALITY = 75
private const val TAG = "DiaryGeneration"
