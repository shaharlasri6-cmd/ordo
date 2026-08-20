package com.shahar.quickcontacts

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.os.Bundle
import android.provider.ContactsContract
import android.view.View
import android.widget.RemoteViews
import kotlin.math.ceil
import kotlin.math.min

class QuickContactsWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        ids.forEach { updateWidget(context, manager, it) }
    }

    override fun onAppWidgetOptionsChanged(context: Context, manager: AppWidgetManager, appWidgetId: Int, newOptions: Bundle) {
        super.onAppWidgetOptionsChanged(context, manager, appWidgetId, newOptions)
        updateWidget(context, manager, appWidgetId)
    }

    companion object {
        private val slotIds = intArrayOf(
            R.id.contact_1, R.id.contact_2, R.id.contact_3, R.id.contact_4, R.id.contact_5,
            R.id.contact_6, R.id.contact_7, R.id.contact_8, R.id.contact_9, R.id.contact_10
        )
        private val nameIds = intArrayOf(
            R.id.contact_1_name, R.id.contact_2_name, R.id.contact_3_name, R.id.contact_4_name, R.id.contact_5_name,
            R.id.contact_6_name, R.id.contact_7_name, R.id.contact_8_name, R.id.contact_9_name, R.id.contact_10_name
        )
        private val photoIds = intArrayOf(
            R.id.contact_1_photo, R.id.contact_2_photo, R.id.contact_3_photo, R.id.contact_4_photo, R.id.contact_5_photo,
            R.id.contact_6_photo, R.id.contact_7_photo, R.id.contact_8_photo, R.id.contact_9_photo, R.id.contact_10_photo
        )
        private val rowIds = intArrayOf(R.id.row_1, R.id.row_2, R.id.row_3, R.id.row_4)

        fun refreshAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val component = ComponentName(context, QuickContactsWidget::class.java)
            manager.getAppWidgetIds(component).forEach { updateWidget(context, manager, it) }
        }

        fun updateWidget(context: Context, manager: AppWidgetManager, widgetId: Int) {
            val contacts = ContactStore.load(context)
            val options = manager.getAppWidgetOptions(widgetId)
            val minWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 280)
            val minHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 100)

            val columns = when {
                minWidth >= 390 -> 5
                minWidth >= 275 -> 4
                else -> 3
            }
            val layout = when (columns) {
                5 -> R.layout.widget_quick_contacts_5
                4 -> R.layout.widget_quick_contacts_4
                else -> R.layout.widget_quick_contacts_3
            }
            val views = RemoteViews(context.packageName, layout)

            val heightRows = ((minHeight + 18) / 108).coerceIn(1, 4)
            val neededRows = ceil(contacts.size / columns.toDouble()).toInt().coerceAtLeast(1)
            val visibleRows = min(heightRows, neededRows)
            val capacity = min(10, columns * visibleRows)

            rowIds.forEachIndexed { index, rowId ->
                views.setViewVisibility(rowId, if (index < visibleRows) View.VISIBLE else View.GONE)
            }

            for (i in 0 until 10) {
                if (i < contacts.size && i < capacity) {
                    val contact = contacts[i]
                    views.setViewVisibility(slotIds[i], View.VISIBLE)
                    views.setTextViewText(nameIds[i], contact.name)
                    val raw = loadPhoto(context, contact.id)
                    views.setImageViewBitmap(photoIds[i], styledAvatar(raw, contact.name, contact.id, 176))

                    val transition = Intent(context, CallTransitionActivity::class.java).apply {
                        putExtra("name", contact.name)
                        putExtra("number", contact.number)
                        putExtra("contactId", contact.id)
                    }
                    val pending = PendingIntent.getActivity(
                        context,
                        widgetId * 100 + i,
                        transition,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    views.setOnClickPendingIntent(slotIds[i], pending)
                } else {
                    views.setViewVisibility(slotIds[i], View.GONE)
                }
            }
            manager.updateAppWidget(widgetId, views)
        }

        private fun loadPhoto(context: Context, contactId: Long): Bitmap? = try {
            val uri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, contactId)
            ContactsContract.Contacts.openContactPhotoInputStream(context.contentResolver, uri, true)?.use {
                BitmapFactory.decodeStream(it)
            }
        } catch (_: Exception) { null }

        private fun styledAvatar(source: Bitmap?, name: String, seed: Long, size: Int): Bitmap {
            val out = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(out)
            val hue = ((seed * 47L) % 360L).toFloat()
            val accent = Color.HSVToColor(floatArrayOf(hue, .42f, .98f))
            val cyan = Color.rgb(85, 230, 211)
            val center = size / 2f
            val radius = size * .41f

            val glow = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(55, Color.red(accent), Color.green(accent), Color.blue(accent)) }
            canvas.drawCircle(center, center, size * .48f, glow)
            val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = size * .035f; color = accent }
            canvas.drawCircle(center, center, size * .435f, ring)
            ring.strokeWidth = size * .014f; ring.color = Color.argb(220, 160, 255, 245)
            canvas.drawCircle(center, center, size * .397f, ring)

            val avatarRect = RectF(center - radius, center - radius, center + radius, center + radius)
            if (source != null) {
                val save = canvas.save()
                val clip = Path().apply { addCircle(center, center, radius, Path.Direction.CW) }
                canvas.clipPath(clip)
                val srcSize = min(source.width, source.height)
                val left = (source.width - srcSize) / 2
                val top = (source.height - srcSize) / 2
                canvas.drawBitmap(source, Rect(left, top, left + srcSize, top + srcSize), avatarRect, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
                canvas.restoreToCount(save)
            } else {
                val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.HSVToColor(floatArrayOf(hue, .46f, .72f)) }
                canvas.drawCircle(center, center, radius, fill)
                val letter = name.trim().take(1).ifBlank { "•" }
                fill.color = Color.WHITE; fill.textAlign = Paint.Align.CENTER; fill.typeface = android.graphics.Typeface.DEFAULT_BOLD; fill.textSize = size * .34f
                val y = center - (fill.descent() + fill.ascent()) / 2f
                canvas.drawText(letter, center, y, fill)
            }

            // Small call badge makes each contact look like a floating quick-action bubble.
            val bx = size * .79f; val by = size * .78f; val br = size * .13f
            val badge = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(13, 32, 46) }
            canvas.drawCircle(bx, by, br * 1.15f, badge)
            badge.color = cyan
            canvas.drawCircle(bx, by, br, badge)
            badge.color = Color.rgb(8, 46, 48); badge.style = Paint.Style.STROKE; badge.strokeWidth = size * .023f
            val path = Path().apply {
                moveTo(bx - br*.42f, by - br*.18f)
                cubicTo(bx - br*.20f, by + br*.30f, bx + br*.10f, by + br*.43f, bx + br*.43f, by + br*.25f)
            }
            canvas.drawPath(path, badge)
            return out
        }
    }
}
