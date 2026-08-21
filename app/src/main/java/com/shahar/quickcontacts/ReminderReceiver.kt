package com.shahar.quickcontacts

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.core.app.NotificationCompat

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra("title").orEmpty().ifBlank { "תזכורת" }
        val mode = intent.getStringExtra("mode") ?: "sound"
        val rawId = intent.getLongExtra("id", System.currentTimeMillis())
        val id = rawId.hashCode()

        val channelId = "ordo_fullscreen_reminder_v1_$mode"
        val manager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= 26) {
            val channel = NotificationChannel(
                channelId,
                when (mode) {
                    "vibrate" -> "Ordo full-screen reminders - vibration"
                    "silent" -> "Ordo full-screen reminders - silent"
                    else -> "Ordo full-screen reminders - sound"
                },
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "תזכורות במסך מלא של Ordo"
                enableLights(true)
                lightColor = Color.CYAN
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                when (mode) {
                    "sound" -> {
                        enableVibration(false)
                        setSound(
                            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM),
                            AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_ALARM)
                                .build()
                        )
                    }
                    "vibrate" -> {
                        setSound(null, null)
                        enableVibration(true)
                        vibrationPattern = longArrayOf(0, 280, 120, 280, 120, 520)
                    }
                    else -> {
                        setSound(null, null)
                        enableVibration(false)
                    }
                }
            }
            manager.createNotificationChannel(channel)
        }

        val fullScreenIntent = Intent(context, FullScreenReminderActivity::class.java).apply {
            putExtra("id", rawId)
            putExtra("title", title)
            putExtra("mode", mode)
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
            )
        }

        val fullScreenPending = PendingIntent.getActivity(
            context,
            id,
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Ordo")
            .setContentText(title)
            .setStyle(NotificationCompat.BigTextStyle().bigText(title))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setOngoing(true)
            .setContentIntent(fullScreenPending)
            .setFullScreenIntent(fullScreenPending, true)
            .setOnlyAlertOnce(false)
            .build()

        manager.notify(id, notification)
        NotificationDeliveryStore.markDelivered(context, id)

        if (mode == "vibrate" && Build.VERSION.SDK_INT < 26) {
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            @Suppress("DEPRECATION")
            vibrator.vibrate(longArrayOf(0, 280, 120, 280, 120, 520), -1)
        }
    }
}
