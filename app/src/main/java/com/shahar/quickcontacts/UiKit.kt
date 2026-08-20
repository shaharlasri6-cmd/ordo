package com.shahar.quickcontacts

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView

object UiKit {
    val bg = Color.rgb(246, 248, 252)
    val card = Color.WHITE
    val ink = Color.rgb(25, 29, 40)
    val muted = Color.rgb(105, 113, 130)
    val accent = Color.rgb(83, 103, 232)

    fun dp(activity: Activity, v: Int) = (v * activity.resources.displayMetrics.density).toInt()

    fun rounded(color: Int, radius: Int, stroke: Int? = null): GradientDrawable {
        return GradientDrawable().apply {
            setColor(color)
            cornerRadius = radius.toFloat()
            if (stroke != null) setStroke(1, stroke)
        }
    }

    fun title(activity: Activity, text: String, size: Float = 28f) = TextView(activity).apply {
        this.text = text
        textSize = size
        setTextColor(ink)
        typeface = Typeface.DEFAULT_BOLD
        gravity = Gravity.START
    }

    fun subtitle(activity: Activity, text: String) = TextView(activity).apply {
        this.text = text
        textSize = 14f
        setTextColor(muted)
        gravity = Gravity.START
    }

    fun moduleCard(activity: Activity, icon: String, title: String, subtitle: String): View {
        val row = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setPadding(dp(activity, 16), dp(activity, 16), dp(activity, 16), dp(activity, 16))
            background = rounded(card, dp(activity, 22), Color.rgb(228,232,241))
            isClickable = true
            isFocusable = true
        }
        val badge = TextView(activity).apply {
            text = icon
            textSize = 24f
            gravity = Gravity.CENTER
            setTextColor(accent)
            typeface = Typeface.DEFAULT_BOLD
            background = rounded(Color.rgb(235,238,255), dp(activity, 16))
        }
        row.addView(badge, LinearLayout.LayoutParams(dp(activity, 52), dp(activity, 52)))
        val texts = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(activity, 14), 0, dp(activity, 10), 0)
        }
        texts.addView(title(activity, title, 18f))
        texts.addView(subtitle(activity, subtitle))
        row.addView(texts, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        row.addView(TextView(activity).apply {
            text = "‹"
            textSize = 30f
            setTextColor(Color.rgb(160,166,180))
            gravity = Gravity.CENTER
        }, LinearLayout.LayoutParams(dp(activity, 28), dp(activity, 52)))
        row.layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { bottomMargin = dp(activity, 11) }
        return row
    }
}
