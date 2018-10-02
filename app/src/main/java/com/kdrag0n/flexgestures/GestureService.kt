package com.kdrag0n.flexgestures

import android.annotation.TargetApi
import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.graphics.drawable.ColorDrawable
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Build.VERSION.SDK_INT as sdk
import android.os.IBinder
import android.view.*
import android.view.WindowManager.LayoutParams
import android.widget.ImageView
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import com.kdrag0n.flexgestures.activities.ScreenshotPermissionActivity
import com.kdrag0n.flexgestures.utils.takeScreenshot
import kotlinx.coroutines.experimental.*
import kotlinx.coroutines.experimental.android.*

class GestureService : Service(), CoroutineScope {
    companion object {
        private const val SERVICE_NOTIFICATION_ID = 1
        private const val SERVICE_NOTIFICATION_CHANNEL = "service"

        const val EXTRA_RESULT_INTENT = "resultIntent"

        const val ACTION_PROJECTION_TOKEN = "projection"
        const val ACTION_STOP = "stop"
    }

    private val job = Job()
    override val coroutineContext = job + Dispatchers.Main

    private lateinit var windowManager: WindowManager
    private lateinit var projection: MediaProjection
    private lateinit var projectionManager: MediaProjectionManager
    private lateinit var touchView: ImageView

    private lateinit var permissionCallback: () -> Unit
    private var projectionToken: Intent? = null
        get() = field?.clone() as Intent?

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onCreate() {
        windowManager = getSystemService()!!
        projectionManager = getSystemService()!!

        val inflater = LayoutInflater.from(this)
        touchView = inflater.inflate(R.layout.overlay_gestures, null, false) as ImageView

        val bgColor = if (BuildConfig.DEBUG) R.color.debug_overlay_bg else android.R.color.transparent
        touchView.setImageDrawable(ColorDrawable(ContextCompat.getColor(this, bgColor)))

        touchView.setOnClickListener { _ ->
            launch {
                takeScreenshot {
                    touchView.setImageBitmap(it)
                }
            }
        }
        /*
        touchView.setOnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    launch {
                        val data = async(Dispatchers.IO) {
                            Thread.sleep(1000)
                            return@async R.color.btn_green
                        }.await()

                        touchView.setBackgroundColor(ContextCompat.getColor(ctx, data))
                    }
                }
                MotionEvent.ACTION_MOVE -> {
                    val drawable = touchView.background as ColorDrawable
                    drawable.color += 4

                    touchView.updateLayoutParams {
                        height += 4
                    }
                }
            }

            true
        }*/

        val params = LayoutParams(
                /* width */ LayoutParams.MATCH_PARENT,
                /* height */ LayoutParams.WRAP_CONTENT,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    LayoutParams.TYPE_APPLICATION_OVERLAY
                else
                    LayoutParams.TYPE_PHONE,
                LayoutParams.FLAG_NOT_FOCUSABLE /*or LayoutParams.FLAG_NOT_TOUCHABLE*/,
                PixelFormat.TRANSLUCENT
        )

        params.gravity = Gravity.BOTTOM or Gravity.START or Gravity.END

        windowManager.addView(touchView, params)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) {
            if (sdk >= 26) {
                createNotification()
            }
        } else if (intent.action == ACTION_PROJECTION_TOKEN) {
            projectionToken = intent.getParcelableExtra(EXTRA_RESULT_INTENT)
        }

        return START_STICKY
    }

    override fun onDestroy() {
        if (::touchView.isInitialized) {
            windowManager.removeView(touchView)
        }
    }

    private fun takeScreenshot(callback: (Bitmap) -> Unit) {
        if (::projection.isInitialized) {
            takeScreenshot(windowManager, projection, callback)
        } else {
            if (projectionToken == null) {
                permissionCallback = {
                    takeScreenshot(windowManager, projection, callback)
                }
                requestScreenshotPermission()
            } else {
                projection = projectionManager.getMediaProjection(Activity.RESULT_OK, projectionToken!!)
                takeScreenshot(windowManager, projection, callback)
            }
        }
    }

    private fun requestScreenshotPermission() {
        val intent = Intent(this, ScreenshotPermissionActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    }

    @TargetApi(26)
    private fun createNotification() {
        val channelName = getString(R.string.service_notification_channel)

        val channel = NotificationChannel(SERVICE_NOTIFICATION_CHANNEL, channelName, NotificationManager.IMPORTANCE_MIN)
        channel.description = getString(R.string.service_notification_channel_desc)
        val manager = getSystemService<NotificationManager>()
        manager?.createNotificationChannel(channel)

        val notification = NotificationCompat.Builder(this, SERVICE_NOTIFICATION_CHANNEL)
                .setStyle(NotificationCompat.BigTextStyle()
                        .setBigContentTitle(getString(R.string.service_notification_title, getString(R.string.app_name)))
                        .bigText(getString(R.string.service_notification_content)))
                .setColor(ContextCompat.getColor(this, R.color.accent))
                .setSmallIcon(R.drawable.ic_touch)
                .setOnlyAlertOnce(true)
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .build()

        startForeground(SERVICE_NOTIFICATION_ID, notification)
    }
}
