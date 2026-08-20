package com.shahar.quickcontacts

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != Intent.ACTION_MY_PACKAGE_REPLACED) return

        val now = System.currentTimeMillis()
        PersonalStore.loadReminders(context)
            .filter { it.atMillis > now }
            .forEach { ReminderScheduler.schedule(context, it) }
    }
}
