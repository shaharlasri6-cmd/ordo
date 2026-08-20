package com.shahar.quickcontacts

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.PixelFormat
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowManager

class CallOverlayService : Service() {
    private val handler = Handler(Looper.getMainLooper())
    private var windowManager: WindowManager? = null
    private var overlay: CallOverlayView? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, notification())
        handler.removeCallbacksAndMessages(null)
        removeOverlay()

        val demo = intent?.getBooleanExtra("demo", false) == true
        val name = intent?.getStringExtra("name").orEmpty().ifBlank { if (demo) "הדגמה" else "איש קשר" }
        val number = intent?.getStringExtra("number").orEmpty()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            stopSelf()
            return START_NOT_STICKY
        }

        showOverlay(name, demo)
        if (demo) {
            handler.postDelayed({ finishOverlay() }, 2800)
        } else {
            handler.postDelayed({
                removeOverlay()
                startCall(number)
                stopSelf()
            }, 1150)
        }
        return START_NOT_STICKY
    }

    private fun showOverlay(name: String, demo: Boolean) {
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        overlay = CallOverlayView(this, name, demo)
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.TOP or Gravity.START }
        windowManager?.addView(overlay, params)
    }

    private fun startCall(number: String) {
        if (number.isBlank()) return
        val action = if (checkSelfPermission(Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
            Intent.ACTION_CALL
        } else Intent.ACTION_DIAL
        val callIntent = Intent(action, Uri.parse("tel:${Uri.encode(number)}")).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            val pending = PendingIntent.getActivity(
                this,
                number.hashCode(),
                callIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            pending.send()
        } catch (_: Exception) {
            try { startActivity(callIntent) } catch (_: Exception) { }
        }
    }

    private fun finishOverlay() {
        removeOverlay()
        stopSelf()
    }

    private fun removeOverlay() {
        overlay?.stop()
        overlay?.let { view ->
            try { windowManager?.removeViewImmediate(view) } catch (_: Exception) { }
        }
        overlay = null
    }

    private fun notification(): Notification {
        val channelId = "call_overlay"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(
                NotificationChannel(channelId, "Call animation", NotificationManager.IMPORTANCE_MIN).apply {
                    setShowBadge(false)
                    description = "Short home-screen call animation"
                }
            )
        }
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) Notification.Builder(this, channelId)
        else @Suppress("DEPRECATION") Notification.Builder(this)
        return builder
            .setSmallIcon(android.R.drawable.sym_action_call)
            .setContentTitle("Quick Contacts")
            .setContentText("מציג אנימציית שיחה")
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        removeOverlay()
        super.onDestroy()
    }

    companion object { private const val NOTIFICATION_ID = 1601 }
}
