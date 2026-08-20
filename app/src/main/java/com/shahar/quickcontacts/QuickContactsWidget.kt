package com.shahar.quickcontacts

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.RemoteViews

class QuickContactsWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        ids.forEach { updateWidget(context, manager, it) }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        manager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle
    ) {
        super.onAppWidgetOptionsChanged(context, manager, appWidgetId, newOptions)
        updateWidget(context, manager, appWidgetId)
    }

    companion object {
        fun refreshAll(context: Context) {
            val appContext = context.applicationContext
            Thread {
                val manager = AppWidgetManager.getInstance(appContext)
                val component = ComponentName(appContext, QuickContactsWidget::class.java)
                manager.getAppWidgetIds(component).forEach {
                    updateWidget(appContext, manager, it)
                    manager.notifyAppWidgetViewDataChanged(it, R.id.contacts_grid)
                }
            }.start()
        }

        fun updateWidget(context: Context, manager: AppWidgetManager, widgetId: Int) {
            val options = manager.getAppWidgetOptions(widgetId)
            val minWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 280)
            val layout = when {
                minWidth >= 390 -> R.layout.widget_contacts_grid_5
                minWidth >= 275 -> R.layout.widget_contacts_grid_4
                else -> R.layout.widget_contacts_grid_3
            }
            val views = RemoteViews(context.packageName, layout)

            val serviceIntent = Intent(context, ContactsWidgetService::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                data = Uri.parse("qcw://contacts/$widgetId/${System.currentTimeMillis()}")
            }
            views.setRemoteAdapter(R.id.contacts_grid, serviceIntent)
            views.setEmptyView(R.id.contacts_grid, R.id.empty_text)

            val templateIntent = Intent(context, CallActionReceiver::class.java).apply {
                data = Uri.parse("qcw://call/$widgetId")
            }
            val template = PendingIntent.getBroadcast(
                context,
                widgetId,
                templateIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            )
            views.setPendingIntentTemplate(R.id.contacts_grid, template)

            val taskIntent = Intent(context, QuickAddReceiver::class.java).apply {
                putExtra("mode", "task")
                data = Uri.parse("ordo://quick-add/task/$widgetId")
            }
            val taskPending = PendingIntent.getBroadcast(context, widgetId + 10000, taskIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            views.setOnClickPendingIntent(R.id.task_quick_card, taskPending)
            val openTasks = PersonalStore.loadTasks(context).count { !it.done }
            views.setTextViewText(R.id.task_quick_sub, if (openTasks == 0) "אין פתוחות" else "$openTasks פתוחות")

            val reminderIntent = Intent(context, QuickAddReceiver::class.java).apply {
                putExtra("mode", "reminder")
                data = Uri.parse("ordo://quick-add/reminder/$widgetId")
            }
            val reminderPending = PendingIntent.getBroadcast(context, widgetId + 20000, reminderIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            views.setOnClickPendingIntent(R.id.reminder_quick_card, reminderPending)
            val next = PersonalStore.loadReminders(context).filter { it.atMillis >= System.currentTimeMillis() }.minByOrNull { it.atMillis }
            val nextText = next?.let { java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date(it.atMillis)) } ?: "אין קרובה"
            views.setTextViewText(R.id.reminder_quick_sub, nextText)
            manager.updateAppWidget(widgetId, views)
        }
    }
}
