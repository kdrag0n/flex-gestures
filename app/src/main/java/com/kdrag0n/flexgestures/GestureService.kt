package com.kdrag0n.flexgestures

import android.annotation.SuppressLint
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
import android.util.Log
import android.view.*
import android.view.WindowManager.LayoutParams
import android.widget.ImageView
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import androidx.core.view.updatePadding
import com.kdrag0n.flexgestures.activities.ScreenshotPermissionActivity
import com.kdrag0n.flexgestures.touch.TouchState
import com.kdrag0n.flexgestures.utils.takeScreenshot
import kotlinx.coroutines.experimental.*
import kotlinx.coroutines.experimental.android.*
import kotlin.coroutines.experimental.suspendCoroutine

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

    private var state = TouchState.NONE
    private var startX = 0.0f
    private var startY = 0.0f
    private var screenshotDeferred: Deferred<Bitmap>? = null

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate() {
        windowManager = getSystemService()!!
        projectionManager = getSystemService()!!

        val inflater = LayoutInflater.from(this)
        touchView = inflater.inflate(R.layout.overlay_gestures, null, false) as ImageView

        val bgColor = if (BuildConfig.DEBUG) R.color.debug_overlay_bg else android.R.color.transparent
        touchView.setImageDrawable(ColorDrawable(ContextCompat.getColor(this, bgColor)))

        touchView.setOnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    state = TouchState.DOWN
                    startX = event.rawX
                    startY = event.rawY

                    // preemptively take a screenshot so it's more likely to be available by the time user swipes to threshold
                    launch {
                        if (screenshotDeferred == null) {
                            screenshotDeferred = takeScreenshot()
                        }
                    }
                }
                MotionEvent.ACTION_MOVE -> {
                    // calculate delta from first touch point
                    val deltaY = startY - event.rawY
                    val deltaX = startX - event.rawX

                    // scale it with some acceleration
                    // move the screenshot/bar view up
                    touchView.updatePadding(bottom = deltaY.toInt())

                    // user's swipe past the threshold
                    if (state != TouchState.SWIPE) {
                        state = TouchState.SWIPE

                        // show the screenshot now
                        launch {
                            // in case somehow, this triggers before ACTION_DOWN or ACTION_UP does not trigger
                            if (screenshotDeferred == null) {
                                screenshotDeferred = takeScreenshot()
                            }

                            val screenshot = screenshotDeferred!!.await()
                            if (state == TouchState.SWIPE) {
                                touchView.setImageBitmap(screenshot)
                            }
                        }
                    }
                }
                MotionEvent.ACTION_UP -> {
                    state = TouchState.NONE

                    // reset to touch bar appearance
                    touchView.setImageDrawable(ColorDrawable(ContextCompat.getColor(this, bgColor)))

                    // bring it back down
                    touchView.updatePadding(bottom = 0)

                    // clear the consumed screenshooter coroutine
                    screenshotDeferred = null
                }
            }

            true
        }

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

    private suspend fun takeScreenshot(): Deferred<Bitmap> {
        val lock = java.lang.Object()
        lateinit var result: Bitmap

        val callback = { bitmap: Bitmap ->
            result = bitmap
            synchronized(lock) {
                lock.notify()
            }
        }

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

        return async(Dispatchers.Default) {
            suspendCoroutine<Bitmap> {
                synchronized(lock) {
                    lock.wait()
                }
                it.resume(result)
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
