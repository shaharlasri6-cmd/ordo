package com.shahar.quickcontacts

import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class EventsActivity : Activity() {
    private lateinit var list: LinearLayout
    private var items = mutableListOf<EventItem>()
    private val fmt = SimpleDateFormat("EEE dd/MM  HH:mm", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        items = PersonalStore.loadEvents(this)
        build()
    }

    private fun build() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = android.view.View.LAYOUT_DIRECTION_RTL
            setBackgroundColor(UiKit.bg)
            setPadding(UiKit.dp(this@EventsActivity,20),UiKit.dp(this@EventsActivity,38),UiKit.dp(this@EventsActivity,20),UiKit.dp(this@EventsActivity,20))
        }
        root.addView(UiKit.title(this,"אירועים קרובים"))
        root.addView(UiKit.subtitle(this,"כל האירועים שלך, מסודרים לפי מה שמגיע קודם").apply { setPadding(0,4,0,16) })
        val add = UiKit.moduleCard(this,"＋","אירוע חדש","שם, זמן והערה אופציונלית")
        add.setOnClickListener { createEvent() }
        root.addView(add)
        val scroll = ScrollView(this)
        list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        scroll.addView(list)
        root.addView(scroll,LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,0,1f))
        setContentView(root)
        render()
    }

    private fun createEvent() {
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(30,0,30,0) }
        val title = EditText(this).apply { hint = "שם האירוע" }
        val note = EditText(this).apply { hint = "הערה (אופציונלי)" }
        box.addView(title); box.addView(note)
        AlertDialog.Builder(this).setTitle("אירוע חדש").setView(box)
            .setNegativeButton("ביטול",null)
            .setPositiveButton("בחר זמן") { _,_ ->
                val t = title.text.toString().trim()
                if(t.isEmpty()) return@setPositiveButton
                DateTimePicker.pick(this) { at ->
                    items.add(EventItem(System.currentTimeMillis(),t,at,note.text.toString().trim()))
                    PersonalStore.saveEvents(this,items)
                    render()
                }
            }.show()
    }

    private fun render() {
        list.removeAllViews()
        val now = System.currentTimeMillis()
        items.sortedBy { it.atMillis }.forEach { item ->
            val sub = buildString {
                append(fmt.format(Date(item.atMillis)))
                if(item.note.isNotBlank()) append(" · ${item.note}")
            }
            val row = ListRows.row(this,item.title,sub,if(item.atMillis < now) "✓" else "◫")
            row.alpha = if(item.atMillis < now) .5f else 1f
            row.setOnLongClickListener {
                items.removeAll { it.id == item.id }
                PersonalStore.saveEvents(this,items)
                render()
                true
            }
            list.addView(row)
        }
        if(items.isEmpty()) list.addView(UiKit.subtitle(this,"אין אירועים"))
    }
}
