package com.shahar.quickcontacts

import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

class TasksActivity : Activity() {
    private lateinit var list: LinearLayout
    private var items = mutableListOf<TaskItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        items = PersonalStore.loadTasks(this)
        build()
    }

    private fun build() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = android.view.View.LAYOUT_DIRECTION_RTL
            setBackgroundColor(UiKit.bg)
            setPadding(UiKit.dp(this@TasksActivity,20), UiKit.dp(this@TasksActivity,38), UiKit.dp(this@TasksActivity,20), UiKit.dp(this@TasksActivity,20))
        }
        root.addView(UiKit.title(this,"משימות"))
        root.addView(UiKit.subtitle(this,"לחיצה על משימה מסמנת אותה כהושלמה; לחיצה ארוכה מוחקת").apply {
            setPadding(0,4,0,16)
        })
        val add = UiKit.moduleCard(this,"＋","משימה חדשה","הוסף משהו שצריך לעשות")
        add.setOnClickListener { addTask() }
        root.addView(add)
        val scroll = ScrollView(this)
        list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        scroll.addView(list)
        root.addView(scroll, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,0,1f))
        setContentView(root)
        render()
    }

    private fun addTask() {
        val input = EditText(this).apply { hint = "מה צריך לעשות?" }
        AlertDialog.Builder(this)
            .setTitle("משימה חדשה")
            .setView(input)
            .setNegativeButton("ביטול",null)
            .setPositiveButton("הוסף") { _,_ ->
                val t = input.text.toString().trim()
                if (t.isNotEmpty()) {
                    items.add(0, TaskItem(System.currentTimeMillis(), t))
                    save()
                }
            }.show()
    }

    private fun render() {
        list.removeAllViews()
        items.sortedWith(compareBy<TaskItem>{it.done}.thenByDescending{it.createdAt}).forEach { item ->
            val row = ListRows.row(this, if(item.done) "✓ ${item.title}" else item.title,
                if(item.done) "הושלם" else "פתוחה", if(item.done) "✓" else "○")
            row.alpha = if(item.done) .55f else 1f
            row.setOnClickListener {
                val i = items.indexOfFirst { it.id == item.id }
                if(i>=0) items[i] = items[i].copy(done = !items[i].done)
                save()
            }
            row.setOnLongClickListener {
                items.removeAll { it.id == item.id }
                save()
                Toast.makeText(this,"המשימה נמחקה",Toast.LENGTH_SHORT).show()
                true
            }
            list.addView(row)
        }
        if(items.isEmpty()) list.addView(UiKit.subtitle(this,"אין משימות כרגע"))
    }

    private fun save() {
        PersonalStore.saveTasks(this,items)
        render()
    }
}
