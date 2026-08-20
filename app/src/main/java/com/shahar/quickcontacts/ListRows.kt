package com.shahar.quickcontacts

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView

object ListRows {
    fun row(activity: Activity, title: String, subtitle: String, accent: String = "•"): LinearLayout {
        return LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutDirection = android.view.View.LAYOUT_DIRECTION_RTL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(UiKit.dp(activity,14), UiKit.dp(activity,12), UiKit.dp(activity,14), UiKit.dp(activity,12))
            background = UiKit.rounded(Color.WHITE, UiKit.dp(activity,18), Color.rgb(230,233,241))

            addView(TextView(activity).apply {
                text = accent
                textSize = 19f
                setTextColor(UiKit.accent)
                gravity = Gravity.CENTER
            }, LinearLayout.LayoutParams(UiKit.dp(activity,34), UiKit.dp(activity,44)))

            val texts = LinearLayout(activity).apply { orientation = LinearLayout.VERTICAL }
            texts.addView(TextView(activity).apply {
                text = title
                textSize = 16f
                setTextColor(UiKit.ink)
                typeface = Typeface.DEFAULT_BOLD
            })
            texts.addView(TextView(activity).apply {
                text = subtitle
                textSize = 13f
                setTextColor(UiKit.muted)
            })
            addView(texts, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        }.also {
            it.layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = UiKit.dp(activity,8) }
        }
    }
}
