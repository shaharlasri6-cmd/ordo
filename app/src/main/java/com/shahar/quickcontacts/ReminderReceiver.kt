package com.shahar.quickcontacts

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
        val id = intent.getLongExtra("id", System.currentTimeMillis()).hashCode()

        val channelId = "reminder_$mode"
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= 26) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(channelId, "Ordo reminders ($mode)", importance).apply {
                description = "תזכורות של Ordo"
                enableLights(true)
                lightColor = Color.CYAN
                when(mode) {
                    "sound" -> {
                        enableVibration(false)
                        setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),
                            AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_NOTIFICATION).build())
                    }
                    "vibrate" -> {
                        setSound(null, null)
                        enableVibration(true)
                        vibrationPattern = longArrayOf(0,220,130,220,130,350)
                    }
                    else -> {
                        setSound(null, null)
                        enableVibration(false)
                    }
                }
            }
            manager.createNotificationChannel(channel)
        }

        val open = Intent(context, RemindersActivity::class.java)
        val pending = PendingIntent.getActivity(context, id, open,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Ordo")
            .setContentText(title)
            .setStyle(NotificationCompat.BigTextStyle().bigText(title))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setAutoCancel(true)
            .setContentIntent(pending)
            .build()

        manager.notify(id, notification)

        if (mode == "vibrate" && Build.VERSION.SDK_INT < 26) {
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            if (Build.VERSION.SDK_INT >= 26) vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0,220,130,220), -1))
            else @Suppress("DEPRECATION") vibrator.vibrate(longArrayOf(0,220,130,220), -1)
        }
    }
}
