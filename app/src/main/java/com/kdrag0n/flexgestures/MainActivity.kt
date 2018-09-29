package com.kdrag0n.flexgestures

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.DisplayMetrics
import android.widget.TextView
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
    private lateinit var projectionManager: MediaProjectionManager
    private lateinit var projection: MediaProjection
    private var beginTime = System.currentTimeMillis()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager

        bt.setOnClickListener {
            beginTime = System.currentTimeMillis()
            if (!::projection.isInitialized) {
                startActivityForResult(projectionManager.createScreenCaptureIntent(), REQUEST_CODE_RECORD)
            } else {
                takeScreenshot()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                REQUEST_CODE_RECORD -> {
                    projection = projectionManager.getMediaProjection(resultCode, data ?: return)
                    takeScreenshot()
                }
            }
        }
    }

    private fun takeScreenshot() {
        val metrics = DisplayMetrics().also {
            windowManager.defaultDisplay.getMetrics(it)
        }
        val width = metrics.widthPixels
        val height = metrics.heightPixels

        val imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
        val display = projection.createVirtualDisplay("screenshot", width, height, metrics.densityDpi, DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR or DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC, imageReader.surface, DisplayCallbacks(), null)

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

            imageView.setImageBitmap(bitmap)

            timeView.setText("${System.currentTimeMillis() - beginTime} ms taken", TextView.BufferType.NORMAL)

            image.close()
            reader.close()
        }, null)
    }

    private class DisplayCallbacks : VirtualDisplay.Callback() {
        override fun onPaused() {
        }

        override fun onResumed() {
        }

        override fun onStopped() {
        }
    }

    companion object {
        private const val REQUEST_CODE_RECORD = 1
    }
}
