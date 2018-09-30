package com.kdrag0n.flexgestures

import android.annotation.TargetApi
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.util.DisplayMetrics
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.WindowManager
import android.view.WindowManager.LayoutParams
import android.widget.LinearLayout
import androidx.core.app.NotificationCompat
import androidx.core.content.getSystemService

class GestureService : Service() {
    companion object {
        private const val SERVICE_NOTIFICATION_ID = 1
        private const val SERVICE_NOTIFICATION_CHANNEL = "service"
    }

    private lateinit var windowManager: WindowManager
    private lateinit var overlayView: LinearLayout

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onCreate() {
        windowManager = getSystemService()!!

        val inflater = LayoutInflater.from(ContextThemeWrapper(this, R.style.AppTheme))
        overlayView = inflater.inflate(R.layout.overlay_gestures, null, false) as LinearLayout

        val params = LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    LayoutParams.TYPE_APPLICATION_OVERLAY
                else
                    LayoutParams.TYPE_PHONE,
                LayoutParams.FLAG_NOT_FOCUSABLE or LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT
        )

        params.gravity = Gravity.BOTTOM or Gravity.START or Gravity.END
        //params.x = 64
        //params.y = 64

        windowManager.addView(overlayView, params)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotification()
        }

        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        if (::overlayView.isInitialized) {
            windowManager.removeView(overlayView)
        }
    }

    @TargetApi(Build.VERSION_CODES.O)
    private fun createNotification() {
        val channelName = getString(R.string.notification_channel_service)

        val channel = NotificationChannel(SERVICE_NOTIFICATION_CHANNEL, channelName, NotificationManager.IMPORTANCE_MIN)
        val manager = getSystemService<NotificationManager>()
        manager?.createNotificationChannel(channel)

        val notification = NotificationCompat.Builder(this, SERVICE_NOTIFICATION_CHANNEL)
                .setContentTitle(getString(R.string.service_notification_title, getString(R.string.app_name)))
                .setContentText(getString(R.string.service_notification_content))
                .setSmallIcon(R.drawable.ic_touch)
                .setOnlyAlertOnce(true)
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .build()

        startForeground(SERVICE_NOTIFICATION_ID, notification)
    }
}