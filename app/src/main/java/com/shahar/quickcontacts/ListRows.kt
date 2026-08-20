package com.shahar.quickcontacts

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView

object ListRows {
    fun row(
        activity: Activity,
        title: String,
        subtitle: String,
        accent: String = "•"
    ): LinearLayout {
        val container = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(
                UiKit.dp(activity, 16),
                UiKit.dp(activity, 14),
                UiKit.dp(activity, 16),
                UiKit.dp(activity, 14)
            )
            background = UiKit.rounded(
                Color.rgb(28, 33, 45),
                UiKit.dp(activity, 18),
                Color.rgb(48, 55, 70)
            )
        }

        val icon = TextView(activity).apply {
            text = accent
            textSize = 19f
            setTextColor(UiKit.accent)
            gravity = Gravity.CENTER
            typeface = Typeface.DEFAULT_BOLD
        }
        container.addView(
            icon,
            LinearLayout.LayoutParams(
                UiKit.dp(activity, 42),
                UiKit.dp(activity, 48)
            )
        )

        val texts = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
        }
        texts.addView(TextView(activity).apply {
            text = title
            textSize = 16f
            setTextColor(UiKit.ink)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.START
        })
        texts.addView(TextView(activity).apply {
            text = subtitle
            textSize = 13f
            setTextColor(UiKit.muted)
            gravity = Gravity.START
            setPadding(0, UiKit.dp(activity, 3), 0, 0)
        })
        container.addView(
            texts,
            LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f
            )
        )

        container.layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            bottomMargin = UiKit.dp(activity, 9)
        }
        return container
    }
}
