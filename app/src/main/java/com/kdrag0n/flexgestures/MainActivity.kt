package com.kdrag0n.flexgestures

import android.annotation.SuppressLint
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
import androidx.annotation.UiThread
import androidx.appcompat.widget.Toolbar
import kotlinx.android.synthetic.main.activity_main.*
import java.lang.reflect.Method

class MainActivity : AppCompatActivity() {
    private lateinit var projectionManager: MediaProjectionManager
    private lateinit var projection: MediaProjection
    private var beginTime = System.currentTimeMillis()

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme)
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar_main as Toolbar?)

        projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager

        screenshotBtn.setOnClickListener {
            beginTime = System.currentTimeMillis()
            if (!::projection.isInitialized) {
                startActivityForResult(projectionManager.createScreenCaptureIntent(), REQUEST_CODE_RECORD)
            } else {
                takeScreenshot()
            }
        }

        hideNavBtn.setOnClickListener {
            hideNavBar()
        }
        showNavBtn.setOnClickListener {
            showNavBar()
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

    @UiThread
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

            timeView.setText(getString(R.string.time_ms, System.currentTimeMillis() - beginTime), TextView.BufferType.NORMAL)

            image.close()
            reader.close()
        }, null)
    }

    @SuppressLint("PrivateApi")
    private fun <T: Any> getWmMethod(name: String, vararg types: Class<T>): Method {
        val wmClass = Class.forName("android.view.IWindowManager")
        return wmClass.getMethod(name, *types)
    }

    @SuppressLint("PrivateApi")
    private fun getWmService(): Any {
        return Class.forName("android.view.WindowManagerGlobal")
                .getMethod("getWindowManagerService")
                .invoke(null)
    }

    @UiThread
    private fun hideNavBar() {
        val navHeightId = resources.getIdentifier("navigation_bar_height", "dimen", "android")
        val navHeight = resources.getDimensionPixelSize(navHeightId)

        getWmMethod("setOverscan", Int::class.java, Int::class.java, Int::class.java, Int::class.java, Int::class.java)
                .invoke(getWmService(), 0, 0, 0, 0, -navHeight)
    }

    @UiThread
    private fun showNavBar() {
        val navHeightId = resources.getIdentifier("navigation_bar_height", "dimen", "android")
        val navHeight = resources.getDimensionPixelSize(navHeightId)

        getWmMethod("setOverscan", Int::class.java, Int::class.java, Int::class.java, Int::class.java, Int::class.java)
                .invoke(getWmService(), 0, 0, 0, 0, -navHeight)
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
