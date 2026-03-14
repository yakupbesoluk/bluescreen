package com.yakup.bluescreen

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.View
import android.view.WindowManager
import androidx.core.app.NotificationCompat

class FilterService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var prefs: PreferencesManager
    private var overlayView: View? = null

    // ─── Lifecycle ───────────────────────────────────────────────────────────

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        prefs = PreferencesManager(this)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                startForeground(NOTIFICATION_ID, buildNotification())
                showOrUpdateOverlay()
            }
            ACTION_STOP -> {
                removeOverlay()
                stopForegroundCompat()
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_UPDATE -> {
                showOrUpdateOverlay()
                // Bildirim güncelle (bildirimde sıcaklık bilgisi var)
                val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
                nm.notify(NOTIFICATION_ID, buildNotification())
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        removeOverlay()
        super.onDestroy()
    }

    // ─── Overlay Yönetimi ────────────────────────────────────────────────────

    private fun showOrUpdateOverlay() {
        val color = prefs.computeOverlayColor()

        if (overlayView == null) {
            val view = View(this)
            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                        or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                        or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                        or WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                PixelFormat.TRANSLUCENT
            )
            view.setBackgroundColor(color)
            try {
                windowManager.addView(view, params)
                overlayView = view
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else {
            overlayView?.setBackgroundColor(color)
        }
    }

    private fun removeOverlay() {
        overlayView?.let {
            try {
                windowManager.removeViewImmediate(it)
            } catch (e: Exception) {
                // View zaten kaldırılmış olabilir
            }
            overlayView = null
        }
    }

    // ─── Bildirim ────────────────────────────────────────────────────────────

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.notification_channel_desc)
            setShowBadge(false)
            enableLights(false)
            enableVibration(false)
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        val openIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = PendingIntent.getService(
            this, 1,
            Intent(this, FilterService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val tempText = "${prefs.temperature}K • %${prefs.intensity}"

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(tempText)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(openIntent)
            .addAction(R.drawable.ic_notification, getString(R.string.action_stop), stopIntent)
            .setOngoing(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    @Suppress("DEPRECATION")
    private fun stopForegroundCompat() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            stopForeground(true)
        }
    }

    // ─── Companion ───────────────────────────────────────────────────────────

    companion object {
        const val ACTION_START  = "com.yakup.bluescreen.START"
        const val ACTION_STOP   = "com.yakup.bluescreen.STOP"
        const val ACTION_UPDATE = "com.yakup.bluescreen.UPDATE"

        private const val NOTIFICATION_ID = 1001
        const val CHANNEL_ID = "bluescreen_service_channel"

        fun start(context: Context) {
            val intent = Intent(context, FilterService::class.java).apply {
                action = ACTION_START
            }
            context.startForegroundService(intent)
        }

        fun stop(context: Context) {
            context.startService(
                Intent(context, FilterService::class.java).apply { action = ACTION_STOP }
            )
        }

        fun update(context: Context) {
            context.startService(
                Intent(context, FilterService::class.java).apply { action = ACTION_UPDATE }
            )
        }
    }
}
