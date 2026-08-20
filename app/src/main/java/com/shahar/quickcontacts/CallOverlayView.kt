package com.shahar.quickcontacts

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.graphics.Typeface
import android.view.View
import android.view.animation.LinearInterpolator
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

class CallOverlayView(
    context: Context,
    private val contactName: String,
    private val demo: Boolean
) : View(context) {
    private val density = resources.displayMetrics.density
    private var phase = 0f
    private val accent = accentFor(contactName)
    private val accentSoft = Color.argb(220, 103, 236, 224)

    private val phaseAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 1800L
        repeatCount = ValueAnimator.INFINITE
        interpolator = LinearInterpolator()
        addUpdateListener {
            phase = it.animatedValue as Float
            invalidate()
        }
        start()
    }

    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    private val subTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (width == 0 || height == 0) return

        val cx = width / 2f
        val cy = height * 0.47f
        val base = min(width, height) * 0.105f

        repeat(3) { i ->
            val p = (phase + i / 3f) % 1f
            val eased = 1f - (1f - p) * (1f - p)
            val radius = base * (1.05f + 1.10f * eased)
            ringPaint.strokeWidth = dp(2.2f)
            ringPaint.color = withAlpha(if (i == 1) accentSoft else accent, ((1f - p) * 118).toInt())
            canvas.drawCircle(cx, cy, radius, ringPaint)
        }

        val orbitR = base * 1.42f
        val rotation = phase * 2f * PI.toFloat()
        repeat(8) { i ->
            val a = rotation + i * (2f * PI.toFloat() / 8f)
            val x = cx + cos(a) * orbitR
            val y = cy + sin(a) * orbitR
            val pulse = 0.62f + 0.38f * ((sin(a - rotation) + 1f) / 2f)
            dotPaint.color = withAlpha(if (i % 2 == 0) accent else accentSoft, (150 + 90 * pulse).toInt())
            canvas.drawCircle(x, y, dp(3.0f + 1.4f * pulse), dotPaint)
        }

        fillPaint.shader = RadialGradient(
            cx, cy, base * 1.15f,
            intArrayOf(withAlpha(accent, 110), withAlpha(accent, 36), Color.TRANSPARENT),
            floatArrayOf(0f, .58f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawCircle(cx, cy, base * 1.15f, fillPaint)
        fillPaint.shader = null

        fillPaint.color = Color.argb(238, 16, 28, 43)
        canvas.drawCircle(cx, cy, base * .72f, fillPaint)
        ringPaint.strokeWidth = dp(2.6f)
        ringPaint.color = withAlpha(accentSoft, 235)
        canvas.drawCircle(cx, cy, base * .72f, ringPaint)

        val letter = contactName.trim().take(1).ifBlank { "•" }
        textPaint.color = Color.WHITE
        textPaint.textSize = base * .62f
        canvas.drawText(letter, cx, cy - (textPaint.ascent() + textPaint.descent()) / 2f, textPaint)

        val label = if (demo) "תצוגת הדגמה" else "מתקשר…"
        val nameText = if (demo) "בלי לבצע שיחה" else contactName
        val chipY = cy + base * 1.95f
        val chipW = min(width * .78f, dp(300f))
        val chipH = dp(76f)
        fillPaint.color = Color.argb(170, 13, 22, 34)
        canvas.drawRoundRect(cx-chipW/2f, chipY-chipH/2f, cx+chipW/2f, chipY+chipH/2f, dp(22f), dp(22f), fillPaint)
        ringPaint.strokeWidth = dp(1f)
        ringPaint.color = withAlpha(accent, 90)
        canvas.drawRoundRect(cx-chipW/2f, chipY-chipH/2f, cx+chipW/2f, chipY+chipH/2f, dp(22f), dp(22f), ringPaint)

        val breathe = 0.82f + 0.18f * ((sin(phase * 2f * PI.toFloat()) + 1f) / 2f)
        subTextPaint.textSize = dp(13f)
        subTextPaint.color = Color.argb((185*breathe).toInt(), 197, 211, 225)
        canvas.drawText(label, cx, chipY-dp(8f), subTextPaint)
        textPaint.textSize = dp(19f)
        textPaint.color = Color.argb((255*breathe).toInt(), 255, 255, 255)
        canvas.drawText(nameText, cx, chipY+dp(17f), textPaint)
    }

    fun stop() = phaseAnimator.cancel()

    private fun accentFor(value: String): Int {
        val colors = intArrayOf(
            Color.rgb(100,220,255), Color.rgb(91,124,250), Color.rgb(126,104,255),
            Color.rgb(52,211,153), Color.rgb(45,212,191)
        )
        return colors[(value.hashCode() and Int.MAX_VALUE) % colors.size]
    }
    private fun withAlpha(color: Int, alpha: Int): Int = Color.argb(
        alpha.coerceIn(0,255), Color.red(color), Color.green(color), Color.blue(color)
    )
    private fun dp(value: Float): Float = value * density
}
