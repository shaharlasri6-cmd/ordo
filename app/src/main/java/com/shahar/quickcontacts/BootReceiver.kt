package com.shahar.quickcontacts

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != Intent.ACTION_MY_PACKAGE_REPLACED
        ) return

        val now = System.currentTimeMillis()
        val catchUpWindow = 24L * 60L * 60L * 1000L

        PersonalStore.loadReminders(context).forEach { item ->
            when {
                item.atMillis > now -> ReminderScheduler.schedule(context, item)
                now - item.atMillis <= catchUpWindow -> {
                    val notificationId = item.id.hashCode()
                    if (!NotificationDeliveryStore.wasDelivered(context, notificationId)) {
                        context.sendBroadcast(
                            Intent(context, ReminderReceiver::class.java).apply {
                                putExtra("id", item.id)
                                putExtra("title", "הוחמצה בזמן שהטלפון היה כבוי: ${item.title}")
                                putExtra("mode", item.alertMode)
                            }
                        )
                    }
                }
            }
        }

        PersonalStore.loadEvents(context).forEach { item ->
            if (item.reminderMinutes < 0) return@forEach

            val reminderAt = item.atMillis - item.reminderMinutes * 60_000L
            when {
                reminderAt > now -> EventReminderScheduler.schedule(context, item)
                now - reminderAt <= catchUpWindow -> {
                    val rawId = item.id xor 0x45E7L
                    val notificationId = rawId.hashCode()
                    if (!NotificationDeliveryStore.wasDelivered(context, notificationId)) {
                        context.sendBroadcast(
                            Intent(context, ReminderReceiver::class.java).apply {
                                putExtra("id", rawId)
                                putExtra("title", "אירוע שההתראה שלו הוחמצה: ${item.title}")
                                putExtra("mode", "sound")
                            }
                        )
                    }
                }
            }
        }
    }
}
