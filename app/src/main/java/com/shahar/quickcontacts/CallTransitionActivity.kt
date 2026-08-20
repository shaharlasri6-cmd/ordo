package com.shahar.quickcontacts

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView

class CallTransitionActivity : Activity() {
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT

        val demo = intent.getBooleanExtra("demo", false)
        val name = intent.getStringExtra("name").orEmpty()
        val number = intent.getStringExtra("number").orEmpty()

        val root = FrameLayout(this).apply {
            setBackgroundColor(Color.argb(232, 9, 18, 30))
        }
        val center = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(28), dp(28), dp(28), dp(28))
        }

        val pulseHolder = FrameLayout(this)
        val ringOuter = View(this).apply { background = ring(Color.argb(120, 63, 224, 208), 3) }
        val ringInner = View(this).apply { background = ring(Color.argb(210, 91, 241, 220), 3) }
        val avatar = TextView(this).apply {
            text = name.trim().take(1).ifBlank { "•" }
            textSize = 34f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            background = circle(Color.rgb(27, 184, 180))
            elevation = dp(8).toFloat()
        }
        pulseHolder.addView(ringOuter, FrameLayout.LayoutParams(dp(138), dp(138), Gravity.CENTER))
        pulseHolder.addView(ringInner, FrameLayout.LayoutParams(dp(116), dp(116), Gravity.CENTER))
        pulseHolder.addView(avatar, FrameLayout.LayoutParams(dp(88), dp(88), Gravity.CENTER))
        center.addView(pulseHolder, LinearLayout.LayoutParams(dp(160), dp(160)))

        center.addView(TextView(this).apply {
            text = if (demo) "הדגמת אנימציית שיחה" else "מתקשר אל…"
            textSize = 16f
            setTextColor(Color.rgb(157, 170, 188))
            gravity = Gravity.CENTER
        })
        center.addView(TextView(this).apply {
            text = name
            textSize = 30f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(0, dp(5), 0, 0)
        })
        center.addView(TextView(this).apply {
            text = number
            textSize = 18f
            setTextColor(Color.rgb(194, 204, 217))
            gravity = Gravity.CENTER
            setPadding(0, dp(4), 0, 0)
        })
        center.addView(TextView(this).apply {
            text = "☎"
            textSize = 30f
            gravity = Gravity.CENTER
            setTextColor(Color.rgb(83, 230, 199))
            setPadding(0, dp(14), 0, 0)
        })

        root.addView(center, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        setContentView(root)

        pulse(ringOuter, 0)
        pulse(ringInner, 180)
        if (demo) {
            handler.postDelayed({ finish() }, 2200)
        } else {
            handler.postDelayed({ startCall(number) }, 850)
        }
    }

    private fun pulse(view: View, delay: Long) {
        view.scaleX = .72f; view.scaleY = .72f; view.alpha = .95f
        view.animate().setStartDelay(delay).scaleX(1.18f).scaleY(1.18f).alpha(.08f)
            .setDuration(820).setInterpolator(AccelerateDecelerateInterpolator()).withEndAction {
                if (!isFinishing) pulse(view, 0)
            }.start()
    }

    private fun startCall(number: String) {
        if (number.isBlank()) { finish(); return }
        val action = if (checkSelfPermission(Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) Intent.ACTION_CALL else Intent.ACTION_DIAL
        try {
            startActivity(Intent(action, Uri.parse("tel:${Uri.encode(number)}")))
        } finally {
            finish()
        }
    }

    override fun onDestroy() { handler.removeCallbacksAndMessages(null); super.onDestroy() }

    private fun circle(color: Int) = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(color) }
    private fun ring(color: Int, widthDp: Int) = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(Color.TRANSPARENT); setStroke(dp(widthDp), color) }
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
}
