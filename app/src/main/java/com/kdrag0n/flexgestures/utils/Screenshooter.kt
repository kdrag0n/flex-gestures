package com.kdrag0n.flexgestures.utils

import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.util.DisplayMetrics
import android.view.WindowManager
import androidx.annotation.UiThread

@UiThread
fun takeScreenshot(windowManager: WindowManager, projection: MediaProjection, callback: (Bitmap) -> Unit) {
    val metrics = DisplayMetrics().also {
        windowManager.defaultDisplay.getMetrics(it)
    }
    val width = metrics.widthPixels
    val height = metrics.heightPixels

    val imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
    val display = projection.createVirtualDisplay("screenshot", width, height, metrics.densityDpi, DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR or DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC, imageReader.surface, null, null)

    imageReader.setOnImageAvailableListener({ reader ->
        display.release()

        val image = reader.acquireLatestImage()
        val buffer = image.planes[0].buffer
        val rowStride = image.planes[0].rowStride
        val pixelStride = image.planes[0].pixelStride
        val rowPadding = rowStride - pixelStride * width

        val bmWidth = width + rowPadding / pixelStride
        val bitmap = Bitmap.createBitmap(bmWidth, height, Bitmap.Config.ARGB_8888)
        bitmap.copyPixelsFromBuffer(buffer)

        callback(bitmap)

        image.close()
        reader.close()
    }, null)
}