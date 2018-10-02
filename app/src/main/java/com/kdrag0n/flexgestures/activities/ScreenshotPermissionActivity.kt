package com.kdrag0n.flexgestures.activities

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import com.kdrag0n.flexgestures.GestureService

class ScreenshotPermissionActivity : Activity() {
    companion object {
        private const val REQUEST_CODE_PROJECTION = 1
    }

    private lateinit var projectionManager: MediaProjectionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        startActivityForResult(projectionManager.createScreenCaptureIntent(), REQUEST_CODE_PROJECTION)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_CODE_PROJECTION && resultCode == Activity.RESULT_OK) {
            val intent = Intent(this, GestureService::class.java)
                    .putExtra(GestureService.EXTRA_RESULT_INTENT, data)
            intent.action = GestureService.ACTION_PROJECTION_TOKEN

            startService(intent)
        }

        finish()
    }
}
