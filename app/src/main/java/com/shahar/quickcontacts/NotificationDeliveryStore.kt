package com.shahar.quickcontacts

import android.content.Context

object NotificationDeliveryStore {
    private const val PREFS = "ordo_notification_delivery"

    fun wasDelivered(context: Context, notificationId: Int): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(notificationId.toString(), false)

    fun markDelivered(context: Context, notificationId: Int) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(notificationId.toString(), true)
            .apply()
    }
}
