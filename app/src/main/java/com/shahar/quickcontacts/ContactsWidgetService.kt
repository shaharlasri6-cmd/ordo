package com.shahar.quickcontacts

import android.appwidget.AppWidgetManager
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
import android.provider.ContactsContract
import android.util.LruCache
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import kotlin.math.min

class ContactsWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory =
        ContactsFactory(applicationContext)

    private class ContactsFactory(private val context: Context) : RemoteViewsFactory {
        private var contacts: List<QuickContact> = emptyList()

        override fun onCreate() = Unit
        override fun onDestroy() = Unit
        override fun getLoadingView(): RemoteViews? = null
        override fun getViewTypeCount(): Int = 1
        override fun hasStableIds(): Boolean = true
        override fun getCount(): Int = contacts.size
        override fun getItemId(position: Int): Long = contacts.getOrNull(position)?.let { it.id xor it.number.hashCode().toLong() } ?: position.toLong()

        override fun onDataSetChanged() {
            contacts = ContactStore.load(context)
        }

        override fun getViewAt(position: Int): RemoteViews? {
            val contact = contacts.getOrNull(position) ?: return null
            val views = RemoteViews(context.packageName, R.layout.widget_contact_item)
            views.setTextViewText(R.id.contact_name, contact.name)
            views.setImageViewBitmap(R.id.contact_photo, avatarFor(contact))

            val fillIn = Intent().apply {
                putExtra("name", contact.name)
                putExtra("number", contact.number)
                putExtra("contactId", contact.id)
            }
            views.setOnClickFillInIntent(R.id.contact_item_root, fillIn)
            return views
        }

        private fun avatarFor(contact: QuickContact): Bitmap {
            val key = "${contact.id}:${contact.name}"
            avatarCache.get(key)?.let { return it }
            val source = loadPhoto(contact.id)
            val bitmap = styledAvatar(source, contact.name, contact.id, 144)
            avatarCache.put(key, bitmap)
            return bitmap
        }

        private fun loadPhoto(contactId: Long): Bitmap? = try {
            val uri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, contactId)
            ContactsContract.Contacts.openContactPhotoInputStream(context.contentResolver, uri, true)?.use {
                BitmapFactory.decodeStream(it)
            }
        } catch (_: Exception) { null }
    }

    companion object {
        private val avatarCache = object : LruCache<String, Bitmap>(32) {}

        private fun styledAvatar(source: Bitmap?, name: String, seed: Long, size: Int): Bitmap {
            val out = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(out)
            val hue = ((seed * 47L) % 360L).toFloat()
            val accent = Color.HSVToColor(floatArrayOf(hue, .42f, .98f))
            val cyan = Color.rgb(85, 230, 211)
            val center = size / 2f
            val radius = size * .40f

            val glow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(42, Color.red(accent), Color.green(accent), Color.blue(accent))
            }
            canvas.drawCircle(center, center, size * .47f, glow)
            val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = size * .032f
                color = accent
            }
            canvas.drawCircle(center, center, size * .43f, ring)
            ring.strokeWidth = size * .012f
            ring.color = Color.argb(210, 160, 255, 245)
            canvas.drawCircle(center, center, size * .392f, ring)

            val avatarRect = RectF(center - radius, center - radius, center + radius, center + radius)
            if (source != null) {
                val save = canvas.save()
                val clip = Path().apply { addCircle(center, center, radius, Path.Direction.CW) }
                canvas.clipPath(clip)
                val srcSize = min(source.width, source.height)
                val left = (source.width - srcSize) / 2
                val top = (source.height - srcSize) / 2
                canvas.drawBitmap(
                    source,
                    Rect(left, top, left + srcSize, top + srcSize),
                    avatarRect,
                    Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
                )
                canvas.restoreToCount(save)
            } else {
                val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.HSVToColor(floatArrayOf(hue, .46f, .72f))
                }
                canvas.drawCircle(center, center, radius, fill)
                val letter = name.trim().take(1).ifBlank { "•" }
                fill.color = Color.WHITE
                fill.textAlign = Paint.Align.CENTER
                fill.typeface = android.graphics.Typeface.DEFAULT_BOLD
                fill.textSize = size * .34f
                canvas.drawText(letter, center, center - (fill.descent() + fill.ascent()) / 2f, fill)
            }

            val bx = size * .79f
            val by = size * .78f
            val br = size * .12f
            val badge = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(13, 32, 46) }
            canvas.drawCircle(bx, by, br * 1.16f, badge)
            badge.color = cyan
            canvas.drawCircle(bx, by, br, badge)
            return out
        }
    }
}
