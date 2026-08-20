package com.shahar.quickcontacts

import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RemindersActivity : Activity() {
    private lateinit var list: LinearLayout
    private var items = mutableListOf<ReminderItem>()
    private val fmt = SimpleDateFormat("dd/MM/yyyy  HH:mm", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        items = PersonalStore.loadReminders(this)
        build()
    }

    private fun build() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = android.view.View.LAYOUT_DIRECTION_RTL
            setBackgroundColor(UiKit.bg)
            setPadding(UiKit.dp(this@RemindersActivity,20),UiKit.dp(this@RemindersActivity,38),UiKit.dp(this@RemindersActivity,20),UiKit.dp(this@RemindersActivity,20))
        }
        root.addView(UiKit.title(this,"תזכורות"))
        root.addView(UiKit.subtitle(this,"בחר זמן ואיך Ordo תתריע לך").apply { setPadding(0,4,0,16) })
        val add = UiKit.moduleCard(this,"＋","תזכורת חדשה","תאריך, שעה, צליל או רטט")
        add.setOnClickListener { ensureExactAlarmAccessThenCreate() }
        root.addView(add)
        val scroll = ScrollView(this)
        list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        scroll.addView(list)
        root.addView(scroll,LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,0,1f))
        setContentView(root)
        render()
    }


    private fun ensureExactAlarmAccessThenCreate() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            val alarm = getSystemService(ALARM_SERVICE) as android.app.AlarmManager
            if (!alarm.canScheduleExactAlarms()) {
                AlertDialog.Builder(this)
                    .setTitle("תזכורות בזמן מדויק")
                    .setMessage("כדי ש-Ordo תתריע בדיוק בזמן שבחרת, Android צריך הרשאה לתזכורות מדויקות. אחרי האישור חזור לכאן והוסף את התזכורת.")
                    .setNegativeButton("לא עכשיו", null)
                    .setPositiveButton("אפשר") { _, _ ->
                        try {
                            startActivity(android.content.Intent(
                                android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                                android.net.Uri.parse("package:$packageName")
                            ))
                        } catch (_: Exception) {
                            startActivity(android.content.Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
                        }
                    }.show()
                return
            }
        }
        createReminder()
    }

    private fun createReminder() {
        val input = EditText(this).apply { hint = "מה להזכיר?" }
        AlertDialog.Builder(this).setTitle("תזכורת חדשה").setView(input)
            .setNegativeButton("ביטול",null)
            .setPositiveButton("המשך") { _,_ ->
                val title = input.text.toString().trim()
                if(title.isEmpty()) return@setPositiveButton
                DateTimePicker.pick(this) { at -> chooseMode(title, at) }
            }.show()
    }

    private fun chooseMode(title: String, at: Long) {
        val modes = arrayOf("צליל","רטט","שקט")
        AlertDialog.Builder(this).setTitle("איך להתריע?")
            .setItems(modes) { _,which ->
                val mode = when(which){0->"sound";1->"vibrate";else->"silent"}
                val item = ReminderItem(System.currentTimeMillis(),title,at,mode)
                items.add(item)
                PersonalStore.saveReminders(this,items)
                ReminderScheduler.schedule(this,item)
                render()
                Toast.makeText(this,"התזכורת נשמרה",Toast.LENGTH_SHORT).show()
            }.show()
    }

    private fun render() {
        list.removeAllViews()
        val now = System.currentTimeMillis()
        items.sortedBy { it.atMillis }.forEach { item ->
            val mode = when(item.alertMode){"sound"->"צליל";"vibrate"->"רטט";else->"שקט"}
            val row = ListRows.row(this,item.title,"${fmt.format(Date(item.atMillis))} · $mode", if(item.atMillis < now) "✓" else "⏰")
            row.alpha = if(item.atMillis < now) .55f else 1f
            row.setOnLongClickListener {
                ReminderScheduler.cancel(this,item)
                items.removeAll { it.id == item.id }
                PersonalStore.saveReminders(this,items)
                render()
                true
            }
            list.addView(row)
        }
        if(items.isEmpty()) list.addView(UiKit.subtitle(this,"אין תזכורות"))
    }
}
