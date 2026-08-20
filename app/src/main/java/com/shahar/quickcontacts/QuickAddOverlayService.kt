package com.shahar.quickcontacts

import android.app.AlarmManager
import android.app.DatePickerDialog
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.TimePicker
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class QuickAddOverlayService : Service() {
    private var wm: WindowManager? = null
    private var root: LinearLayout? = null
    private var selectedDate: Calendar = Calendar.getInstance()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(2201, notification())
        removeOverlay()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
            !Settings.canDrawOverlays(this)
        ) {
            stopSelf()
            return START_NOT_STICKY
        }

        when (intent?.getStringExtra("mode")) {
            "task" -> showTask()
            "reminder" -> showReminder()
            else -> stopSelf()
        }
        return START_NOT_STICKY
    }

    private fun showTask() {
        val card = baseCard("משימה חדשה", "הוסף בלי לעזוב את מסך הבית")
        val input = input("מה צריך לעשות?")
        card.addView(input, fieldParams())

        card.addView(buttonRow(
            cancel = { finishOverlay() },
            saveLabel = "הוסף",
            save = {
                val title = input.text.toString().trim()
                if (title.isNotEmpty()) {
                    val tasks = PersonalStore.loadTasks(this)
                    tasks.add(0, TaskItem(System.currentTimeMillis(), title))
                    PersonalStore.saveTasks(this, tasks)
                    QuickContactsWidget.refreshAll(this)
                    Toast.makeText(this, "המשימה נוספה", Toast.LENGTH_SHORT).show()
                    finishOverlay()
                }
            }
        ))
        attach(card, 310)
        focusKeyboard(input)
    }

    private fun showReminder() {
        selectedDate = Calendar.getInstance()

        val card = baseCard("תזכורת חדשה", "היום כברירת מחדל · אפשר לבחור יום אחר")
        val input = input("מה להזכיר?")
        card.addView(input, fieldParams())

        val dateLabel = TextView(this).apply {
            text = "היום"
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(UiKit.mint)
            gravity = Gravity.CENTER
            background = UiKit.rounded(Color.rgb(29, 36, 56), dp(14), Color.rgb(55, 65, 92))
            setPadding(dp(14), dp(9), dp(14), dp(9))
            setOnClickListener {
                val now = Calendar.getInstance()
                val d = DatePickerDialog(
                    this@QuickAddOverlayService,
                    { _, y, m, day ->
                        selectedDate.set(Calendar.YEAR, y)
                        selectedDate.set(Calendar.MONTH, m)
                        selectedDate.set(Calendar.DAY_OF_MONTH, day)
                        text = if (sameDay(selectedDate, now)) {
                            "היום"
                        } else {
                            SimpleDateFormat("dd/MM", Locale.getDefault()).format(Date(selectedDate.timeInMillis))
                        }
                    },
                    selectedDate.get(Calendar.YEAR),
                    selectedDate.get(Calendar.MONTH),
                    selectedDate.get(Calendar.DAY_OF_MONTH)
                )
                d.window?.setType(
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                    else
                        @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE
                )
                d.show()
            }
        }
        card.addView(dateLabel, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { bottomMargin = dp(8) })

        val time = TimePicker(this).apply {
            setIs24HourView(true)
            hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            minute = Calendar.getInstance().get(Calendar.MINUTE)
        }
        card.addView(time, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            dp(155)
        ).apply { bottomMargin = dp(8) })

        val modes = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        var alertMode = "sound"
        val chips = mutableListOf<TextView>()

        fun chip(label: String, mode: String): TextView {
            return TextView(this).apply {
                text = label
                textSize = 13f
                typeface = Typeface.DEFAULT_BOLD
                gravity = Gravity.CENTER
                setTextColor(UiKit.ink)
                setPadding(dp(11), dp(8), dp(11), dp(8))
                background = UiKit.rounded(Color.rgb(29,36,56), dp(14), Color.rgb(55,65,92))
                setOnClickListener {
                    alertMode = mode
                    chips.forEach { it.background = UiKit.rounded(Color.rgb(29,36,56), dp(14), Color.rgb(55,65,92)) }
                    background = UiKit.rounded(UiKit.accent, dp(14))
                }
            }
        }
        val sound = chip("צליל", "sound")
        val vibrate = chip("רטט", "vibrate")
        val silent = chip("שקט", "silent")
        chips.addAll(listOf(sound, vibrate, silent))
        sound.background = UiKit.rounded(UiKit.accent, dp(14))
        modes.addView(sound)
        modes.addView(vibrate, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { marginStart = dp(8); marginEnd = dp(8) })
        modes.addView(silent)
        card.addView(modes, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { bottomMargin = dp(12) })

        card.addView(buttonRow(
            cancel = { finishOverlay() },
            saveLabel = "שמור",
            save = {
                val title = input.text.toString().trim()
                if (title.isEmpty()) return@buttonRow

                val whenAt = Calendar.getInstance().apply {
                    set(
                        selectedDate.get(Calendar.YEAR),
                        selectedDate.get(Calendar.MONTH),
                        selectedDate.get(Calendar.DAY_OF_MONTH),
                        time.hour,
                        time.minute,
                        0
                    )
                    set(Calendar.MILLISECOND, 0)
                }

                if (whenAt.timeInMillis <= System.currentTimeMillis()) {
                    Toast.makeText(this, "הזמן שבחרת כבר עבר", Toast.LENGTH_SHORT).show()
                    return@buttonRow
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val alarm = getSystemService(ALARM_SERVICE) as AlarmManager
                    if (!alarm.canScheduleExactAlarms()) {
                        try {
                            startActivity(
                                Intent(
                                    Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                                    android.net.Uri.parse("package:$packageName")
                                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                        } catch (_: Exception) { }
                    }
                }

                val item = ReminderItem(
                    System.currentTimeMillis(),
                    title,
                    whenAt.timeInMillis,
                    alertMode
                )
                val reminders = PersonalStore.loadReminders(this)
                reminders.add(item)
                PersonalStore.saveReminders(this, reminders)
                ReminderScheduler.schedule(this, item)
                QuickContactsWidget.refreshAll(this)
                Toast.makeText(this, "התזכורת נשמרה", Toast.LENGTH_SHORT).show()
                finishOverlay()
            }
        ))

        attach(card, 500)
        focusKeyboard(input)
    }

    private fun baseCard(title: String, subtitle: String): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setPadding(dp(20), dp(20), dp(20), dp(16))
            background = UiKit.rounded(
                Color.rgb(20, 25, 40),
                dp(26),
                Color.rgb(62, 72, 101)
            )
            addView(TextView(this@QuickAddOverlayService).apply {
                text = title
                textSize = 22f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(UiKit.ink)
                gravity = Gravity.START
            })
            addView(TextView(this@QuickAddOverlayService).apply {
                text = subtitle
                textSize = 13f
                setTextColor(UiKit.muted)
                gravity = Gravity.START
                setPadding(0, dp(3), 0, dp(14))
            })
        }
    }

    private fun input(hint: String) = EditText(this).apply {
        this.hint = hint
        textSize = 16f
        setTextColor(UiKit.ink)
        setHintTextColor(Color.rgb(120, 130, 154))
        setSingleLine(true)
        setPadding(dp(14), dp(12), dp(14), dp(12))
        background = UiKit.rounded(
            Color.rgb(13, 18, 31),
            dp(16),
            Color.rgb(55, 65, 92)
        )
    }

    private fun fieldParams() = LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
    ).apply { bottomMargin = dp(15) }

    private fun buttonRow(
        cancel: () -> Unit,
        saveLabel: String,
        save: () -> Unit
    ): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END
            addView(TextView(this@QuickAddOverlayService).apply {
                text = "ביטול"
                textSize = 14f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(UiKit.muted)
                gravity = Gravity.CENTER
                setPadding(dp(14), dp(10), dp(14), dp(10))
                background = UiKit.rounded(Color.rgb(29,36,56), dp(14))
                setOnClickListener { cancel() }
            })
            addView(TextView(this@QuickAddOverlayService).apply {
                text = saveLabel
                textSize = 14f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER
                setPadding(dp(16), dp(10), dp(16), dp(10))
                background = UiKit.rounded(UiKit.accent, dp(14))
                setOnClickListener { save() }
            }, LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { marginStart = dp(9) })
        }
    }

    private fun attach(view: LinearLayout, heightDp: Int) {
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        root = view
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            (resources.displayMetrics.widthPixels * .92f).toInt(),
            dp(heightDp),
            type,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
            softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        }
        wm?.addView(view, params)
    }

    private fun focusKeyboard(input: EditText) {
        input.requestFocus()
        input.postDelayed({
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(input, InputMethodManager.SHOW_IMPLICIT)
        }, 220)
    }

    private fun finishOverlay() {
        removeOverlay()
        stopSelf()
    }

    private fun removeOverlay() {
        root?.let {
            try { wm?.removeViewImmediate(it) } catch (_: Exception) { }
        }
        root = null
    }

    override fun onDestroy() {
        removeOverlay()
        super.onDestroy()
    }

    private fun sameDay(a: Calendar, b: Calendar): Boolean =
        a.get(Calendar.YEAR) == b.get(Calendar.YEAR) &&
            a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR)

    private fun notification(): Notification {
        val channelId = "quick_add_overlay"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(
                NotificationChannel(
                    channelId,
                    "Ordo quick add",
                    NotificationManager.IMPORTANCE_MIN
                ).apply {
                    setShowBadge(false)
                    description = "Quick task and reminder entry from the home screen"
                }
            )
        }
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, channelId)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
        }
        return builder
            .setSmallIcon(android.R.drawable.ic_input_add)
            .setContentTitle("Ordo")
            .setContentText("הוספה מהירה")
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
}
