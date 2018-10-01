package com.kdrag0n.flexgestures.activities

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle

class ScreenshotPermissionActivity : AppCompatActivity() {
    companion object {
        private const val REQUEST_CODE_PROJECTION = 1

        var screenshotPermission: Intent? = null
        get() = field?.clone() as Intent?
    }

    private lateinit var projectionManager: MediaProjectionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        startActivityForResult(projectionManager.createScreenCaptureIntent(), REQUEST_CODE_PROJECTION)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        setResult(resultCode, data?.clone() as Intent?)
        /*if (requestCode == REQUEST_CODE_PROJECTION) {
            screenshotPermission = when (resultCode) {
                Activity.RESULT_OK -> data?.clone() as Intent?
                Activity.RESULT_CANCELED -> null
                else -> null
            }
        }*/

        finish()
    }
}
