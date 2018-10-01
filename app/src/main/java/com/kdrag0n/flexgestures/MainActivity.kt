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
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Build.VERSION.SDK_INT as sdk
import android.os.Bundle
import android.provider.Settings
import android.util.DisplayMetrics
import androidx.annotation.UiThread
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.preference.Preference
import com.kdrag0n.flexgestures.utils.PrivateWindowManager as PWM
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity(), OptionFragment.Callbacks {
    companion object {
        private const val REQUEST_CODE_RECORD = 1
        private const val REQUEST_CODE_OVERLAY = 2
    }

    private lateinit var projectionManager: MediaProjectionManager
    private lateinit var projection: MediaProjection
    private lateinit var optFrag: OptionFragment
    private var beginTime = System.currentTimeMillis()

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme)
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar_main as Toolbar?)

        projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        optFrag = optionFragment as OptionFragment
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                REQUEST_CODE_RECORD -> {
                    projection = projectionManager.getMediaProjection(resultCode, data ?: return)
                    takeScreenshot { bitmap, timeMs ->
                        imageView.setImageBitmap(bitmap)
                        val pref = optFrag.preferenceManager.findPreference("screenshot")
                        pref.summary = getString(R.string.take_screenshot_time_desc, timeMs)
                    }
                }
                REQUEST_CODE_OVERLAY -> {
                    if (sdk >= 23 && Settings.canDrawOverlays(this)) {
                        startGestureService()
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        projection.stop()
    }

    private fun startGestureService() {
        val intent = Intent(this, GestureService::class.java)
        ContextCompat.startForegroundService(this, intent)
    }

    private fun stopGestureService() {
        val intent = Intent(this, GestureService::class.java)
        stopService(intent)
    }

    override fun onScreenshot(preference: Preference) {
        beginTime = System.currentTimeMillis()
        if (!::projection.isInitialized) {
            startActivityForResult(projectionManager.createScreenCaptureIntent(), REQUEST_CODE_RECORD)
        } else {
            takeScreenshot { bitmap, timeMs ->
                imageView.setImageBitmap(bitmap)
                preference.summary = getString(R.string.take_screenshot_time_desc, timeMs)
            }
        }
    }

    override fun onHideNavbarChange(hide: Boolean): Boolean? {
        if (hide) {
            hideNavBar()
        } else {
            showNavBar()
        }

        return null
    }

    override fun onServiceChange(enable: Boolean): Boolean? {
        if (enable) {
            if (sdk >= 23) {
                if (Settings.canDrawOverlays(this)) {
                    startGestureService()
                } else {
                    val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + BuildConfig.APPLICATION_ID))
                    startActivityForResult(intent, REQUEST_CODE_OVERLAY)
                    return false
                }
            } else {
                startGestureService()
            }
        } else {
            stopGestureService()
        }

        return null
    }

    @UiThread
    private fun takeScreenshot(callback: (Bitmap, Long) -> Unit) {
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

            callback(bitmap, System.currentTimeMillis() - beginTime)

            image.close()
            reader.close()
        }, null)
    }

    private fun hideNavBar() {
        val navHeightId = resources.getIdentifier("navigation_bar_height", "dimen", "android")
        val navHeight = resources.getDimensionPixelSize(navHeightId)

        PWM.setOverscan(display = 0, left = 0, top = 0, right = 0, bottom = -navHeight)
    }

    private fun showNavBar() {
        PWM.setOverscan(display = 0, left = 0, top = 0, right = 0, bottom = 0)
    }

    private class DisplayCallbacks : VirtualDisplay.Callback() {
        override fun onPaused() {
        }

        override fun onResumed() {
        }

        override fun onStopped() {
        }
    }
}
