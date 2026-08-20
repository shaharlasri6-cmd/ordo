package com.shahar.quickcontacts

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : Activity() {
    private val updateManager by lazy { UpdateManager(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestNotificationPermission()
        buildUi()
        window.decorView.postDelayed({ updateManager.check(false) }, 900)
    }

    override fun onResume() {
        super.onResume()
        updateManager.resumePendingInstaller()
        if (::summary.isInitialized) refreshSummary()
    }

    private lateinit var summary: TextView

    private fun buildUi() {
        window.statusBarColor = UiKit.bg
        window.navigationBarColor = UiKit.bg

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setBackgroundColor(UiKit.bg)
            setPadding(UiKit.dp(this@MainActivity, 20), UiKit.dp(this@MainActivity, 18),
                UiKit.dp(this@MainActivity, 20), UiKit.dp(this@MainActivity, 18))
        }
        root.setOnApplyWindowInsetsListener { v, insets ->
            val bars = if (Build.VERSION.SDK_INT >= 30) insets.getInsets(WindowInsets.Type.systemBars()) else null
            val top = bars?.top ?: 0
            val bottom = bars?.bottom ?: 0
            v.setPadding(UiKit.dp(this,20), top+UiKit.dp(this,18), UiKit.dp(this,20), bottom+UiKit.dp(this,18))
            insets
        }

        val scroll = ScrollView(this).apply { isFillViewport = true }
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
        }

        content.addView(UiKit.title(this, "Ordo"))
        content.addView(UiKit.subtitle(this, "העוזר האישי שלך למסך הבית").apply {
            setPadding(0, UiKit.dp(this@MainActivity,4), 0, UiKit.dp(this@MainActivity,16))
        })

        summary = TextView(this).apply {
            textSize = 15f
            setTextColor(UiKit.ink)
            background = UiKit.rounded(android.graphics.Color.WHITE, UiKit.dp(this@MainActivity,20),
                android.graphics.Color.rgb(228,232,241))
            setPadding(UiKit.dp(this@MainActivity,16), UiKit.dp(this@MainActivity,14),
                UiKit.dp(this@MainActivity,16), UiKit.dp(this@MainActivity,14))
        }
        content.addView(summary, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { bottomMargin = UiKit.dp(this@MainActivity,16) })

        val contacts = UiKit.moduleCard(this, "☎", "אנשי קשר", "הווידג'ט המהיר והחיוג שכבר בנינו")
        contacts.setOnClickListener { startActivity(Intent(this, ContactsActivity::class.java)) }
        content.addView(contacts)

        val tasks = UiKit.moduleCard(this, "✓", "משימות", "הוסף משימות, סמן כהושלם ונהל את היום")
        tasks.setOnClickListener { startActivity(Intent(this, TasksActivity::class.java)) }
        content.addView(tasks)

        val reminders = UiKit.moduleCard(this, "⏰", "תזכורות", "בחר שעה, צליל, רטט או התראה שקטה")
        reminders.setOnClickListener { startActivity(Intent(this, RemindersActivity::class.java)) }
        content.addView(reminders)

        val events = UiKit.moduleCard(this, "◫", "אירועים קרובים", "אירועים מסודרים אוטומטית לפי הזמן הקרוב")
        events.setOnClickListener { startActivity(Intent(this, EventsActivity::class.java)) }
        content.addView(events)

        val updates = UiKit.moduleCard(this, "↻", "עדכונים", "בדיקה והתקנה של גרסאות חדשות")
        updates.setOnClickListener { updateManager.check(true) }
        content.addView(updates)

        scroll.addView(content)
        root.addView(scroll, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,0,1f))
        setContentView(root)
        refreshSummary()
    }

    private fun refreshSummary() {
        val tasks = PersonalStore.loadTasks(this)
        val openTasks = tasks.count { !it.done }
        val now = System.currentTimeMillis()
        val nextReminder = PersonalStore.loadReminders(this)
            .filter { it.atMillis >= now }.minByOrNull { it.atMillis }
        val nextEvent = PersonalStore.loadEvents(this)
            .filter { it.atMillis >= now }.minByOrNull { it.atMillis }

        val fmt = SimpleDateFormat("dd/MM  HH:mm", Locale.getDefault())
        summary.text = buildString {
            append("היום שלך\n")
            append("$openTasks משימות פתוחות")
            nextReminder?.let { append("\nתזכורת קרובה: ${it.title} · ${fmt.format(Date(it.atMillis))}") }
            nextEvent?.let { append("\nאירוע קרוב: ${it.title} · ${fmt.format(Date(it.atMillis))}") }
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 701)
        }
    }
}
