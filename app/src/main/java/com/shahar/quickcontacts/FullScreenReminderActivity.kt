package com.shahar.quickcontacts

import android.animation.ValueAnimator
import android.app.Activity
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import java.util.Calendar

class FullScreenReminderActivity : Activity() {
    private var rawId: Long = 0L
    private var titleText: String = ""
    private var mode: String = "sound"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        rawId = intent.getLongExtra("id", System.currentTimeMillis())
        titleText = intent.getStringExtra("title").orEmpty().ifBlank { "תזכורת" }
        mode = intent.getStringExtra("mode") ?: "sound"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }

        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.rgb(8, 12, 23)

        buildUi()
    }

    private fun buildUi() {
        val root = ReminderPulseView(this).apply {
            setBackgroundColor(Color.rgb(8, 12, 23))
        }

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setPadding(dp(28), dp(48), dp(28), dp(38))
        }

        val badge = TextView(this).apply {
            text = "⏰"
            textSize = 54f
            gravity = Gravity.CENTER
            background = UiKit.rounded(Color.argb(190, 29, 36, 56), dp(30))
        }
        content.addView(
            badge,
            LinearLayout.LayoutParams(dp(108), dp(108)).apply {
                bottomMargin = dp(28)
            }
        )

        content.addView(TextView(this).apply {
            text = "ORDO REMINDER"
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.rgb(126, 231, 213))
            letterSpacing = .18f
            gravity = Gravity.CENTER
        })

        content.addView(TextView(this).apply {
            text = titleText
            textSize = 30f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(0, dp(14), 0, dp(10))
        })

        content.addView(TextView(this).apply {
            text = when (mode) {
                "vibrate" -> "התראה ברטט"
                "silent" -> "תזכורת שקטה"
                else -> "הגיע הזמן"
            }
            textSize = 15f
            setTextColor(Color.rgb(166, 177, 202))
            gravity = Gravity.CENTER
        })

        val spacer = View(this)
        content.addView(
            spacer,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        val snooze = actionButton("נודניק · 10 דקות", false).apply {
            setOnClickListener {
                scheduleSnooze()
                finishAndRemoveTask()
            }
        }
        content.addView(
            snooze,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(58)
            ).apply { bottomMargin = dp(12) }
        )

        val dismiss = actionButton("סגור תזכורת", true).apply {
            setOnClickListener {
                cancelNotification()
                finishAndRemoveTask()
            }
        }
        content.addView(
            dismiss,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(62)
            )
        )

        root.addView(
            content,
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )
        setContentView(root)

        root.setOnApplyWindowInsetsListener { _, insets ->
            if (Build.VERSION.SDK_INT >= 30) {
                val bars = insets.getInsets(WindowInsets.Type.systemBars())
                content.setPadding(dp(28), bars.top + dp(28), dp(28), bars.bottom + dp(26))
            }
            insets
        }
    }

    private fun actionButton(label: String, primary: Boolean): TextView {
        return TextView(this).apply {
            text = label
            textSize = 17f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setTextColor(if (primary) Color.WHITE else Color.rgb(204, 212, 231))
            background = UiKit.rounded(
                if (primary) UiKit.accent else Color.rgb(25, 31, 49),
                dp(19),
                if (primary) null else Color.rgb(58, 68, 96)
            )
        }
    }

    private fun scheduleSnooze() {
        val snoozeAt = System.currentTimeMillis() + 10 * 60 * 1000L
        val intent = Intent(this, ReminderReceiver::class.java).apply {
            putExtra("id", rawId)
            putExtra("title", titleText)
            putExtra("mode", mode)
        }
        val pending = PendingIntent.getBroadcast(
            this,
            rawId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val alarm = getSystemService(ALARM_SERVICE) as AlarmManager
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                !alarm.canScheduleExactAlarms()
            ) {
                alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, snoozeAt, pending)
            } else {
                alarm.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, snoozeAt, pending)
            }
        } catch (_: Exception) {
            alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, snoozeAt, pending)
        }
        cancelNotification()
    }

    private fun cancelNotification() {
        val manager = getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager
        manager.cancel(rawId.hashCode())
    }

    override fun onBackPressed() {
        cancelNotification()
        super.onBackPressed()
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private class ReminderPulseView(activity: Activity) : ViewGroup(activity) {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val glow = Paint(Paint.ANTI_ALIAS_FLAG)
        private var phase = 0f

        private val animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 2100L
            repeatCount = ValueAnimator.INFINITE
            addUpdateListener {
                phase = it.animatedValue as Float
                invalidate()
            }
            start()
        }

        init {
            setWillNotDraw(false)
        }

        override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
            val w = MeasureSpec.getSize(widthMeasureSpec)
            val h = MeasureSpec.getSize(heightMeasureSpec)
            setMeasuredDimension(w, h)
            for (i in 0 until childCount) {
                getChildAt(i).measure(
                    MeasureSpec.makeMeasureSpec(w, MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(h, MeasureSpec.EXACTLY)
                )
            }
        }

        override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
            for (i in 0 until childCount) {
                getChildAt(i).layout(0, 0, r - l, b - t)
            }
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val cx = width / 2f
            val cy = height * .34f

            glow.shader = RadialGradient(
                cx,
                cy,
                width * .72f,
                intArrayOf(
                    Color.argb(80, 93, 107, 255),
                    Color.argb(26, 126, 231, 213),
                    Color.TRANSPARENT
                ),
                floatArrayOf(0f, .45f, 1f),
                Shader.TileMode.CLAMP
            )
            canvas.drawCircle(cx, cy, width * .72f, glow)
            glow.shader = null

            repeat(3) { i ->
                val local = (phase + i / 3f) % 1f
                val radius = width * (.15f + local * .48f)
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = dpF(2.2f)
                paint.color = Color.argb(
                    ((1f - local) * 100).toInt(),
                    if (i % 2 == 0) 126 else 93,
                    if (i % 2 == 0) 231 else 107,
                    if (i % 2 == 0) 213 else 255
                )
                canvas.drawCircle(cx, cy, radius, paint)
            }
        }

        override fun onDetachedFromWindow() {
            animator.cancel()
            super.onDetachedFromWindow()
        }

        private fun dpF(v: Float) = v * resources.displayMetrics.density
    }
}
