package com.kdrag0n.flexgestures

import android.annotation.TargetApi
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.Build.VERSION.SDK_INT as sdk
import android.os.IBinder
import android.view.*
import android.view.WindowManager.LayoutParams
import android.widget.LinearLayout
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService

class GestureService : Service() {
    companion object {
        private const val SERVICE_NOTIFICATION_ID = 1
        private const val SERVICE_NOTIFICATION_CHANNEL = "service"
    }

    private lateinit var windowManager: WindowManager
    private lateinit var layout: LinearLayout

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onCreate() {
        windowManager = getSystemService()!!

        val ctx = ContextThemeWrapper(this, R.style.AppTheme)
        val inflater = LayoutInflater.from(ctx)
        layout = inflater.inflate(R.layout.overlay_gestures, null, false) as LinearLayout

        val touchView = layout.findViewById<View>(R.id.touch_view)
        val bgColor = if (BuildConfig.DEBUG) R.color.debug_overlay_bg else android.R.color.transparent
        touchView.setBackgroundColor(ContextCompat.getColor(ctx, bgColor))

        touchView.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
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

        windowManager.addView(layout, params)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (sdk >= 26) {
            createNotification()
        }

        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        if (::layout.isInitialized) {
            windowManager.removeView(layout)
        }
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
