package com.shahar.quickcontacts

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build

object ReminderScheduler {
    fun schedule(context: Context, item: ReminderItem) {
        val alarm = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra("id", item.id)
            putExtra("title", item.title)
            putExtra("mode", item.alertMode)
        }
        val pending = PendingIntent.getBroadcast(
            context, item.id.hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarm.canScheduleExactAlarms()) {
                alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, item.atMillis, pending)
            } else {
                alarm.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, item.atMillis, pending)
            }
        } catch (_: Exception) {
            alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, item.atMillis, pending)
        }
    }

    fun cancel(context: Context, item: ReminderItem) {
        val alarm = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReminderReceiver::class.java)
        val pending = PendingIntent.getBroadcast(
            context, item.id.hashCode(), intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pending != null) alarm.cancel(pending)
    }
}
