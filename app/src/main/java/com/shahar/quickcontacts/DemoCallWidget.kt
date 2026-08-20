package com.shahar.quickcontacts

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews

class DemoCallWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        ids.forEach { id ->
            val views = RemoteViews(context.packageName, R.layout.widget_demo_call)
            val intent = Intent(context, CallTransitionActivity::class.java).apply {
                putExtra("demo", true)
                putExtra("name", "מצב הדגמה")
                putExtra("number", "לא מתבצעת שיחה")
            }
            val pending = PendingIntent.getActivity(
                context,
                900000 + id,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.demo_widget_root, pending)
            manager.updateAppWidget(id, views)
        }
    }
}
