package com.shahar.quickcontacts

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView

object UiKit {
    val bg = Color.rgb(12, 16, 29)
    val surface = Color.rgb(22, 28, 45)
    val surface2 = Color.rgb(29, 36, 56)
    val ink = Color.rgb(247, 249, 255)
    val muted = Color.rgb(157, 166, 190)
    val accent = Color.rgb(114, 102, 255)
    val mint = Color.rgb(119, 234, 214)

    fun dp(activity: Activity, v: Int) = (v * activity.resources.displayMetrics.density).toInt()

    fun rounded(color: Int, radius: Int, stroke: Int? = null): GradientDrawable = GradientDrawable().apply {
        setColor(color); cornerRadius = radius.toFloat(); if (stroke != null) setStroke(1, stroke)
    }

    fun title(activity: Activity, text: String, size: Float = 28f) = TextView(activity).apply {
        this.text = text; textSize = size; setTextColor(ink); typeface = Typeface.DEFAULT_BOLD; gravity = Gravity.START
    }

    fun subtitle(activity: Activity, text: String) = TextView(activity).apply {
        this.text = text; textSize = 14f; setTextColor(muted); gravity = Gravity.START
    }

    fun header(activity: Activity, title: String, subtitle: String, showHome: Boolean = true): LinearLayout {
        val wrap = LinearLayout(activity).apply { orientation = LinearLayout.VERTICAL; layoutDirection = View.LAYOUT_DIRECTION_RTL }
        if (showHome) {
            val home = TextView(activity).apply {
                text = "⌂  בית"; textSize = 14f; typeface = Typeface.DEFAULT_BOLD; setTextColor(mint); gravity = Gravity.CENTER
                background = rounded(surface2, dp(activity, 14)); setPadding(dp(activity,12),dp(activity,8),dp(activity,12),dp(activity,8))
                setOnClickListener {
                    activity.startActivity(Intent(activity, MainActivity::class.java).apply { addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP) })
                    activity.finish()
                }
            }
            wrap.addView(home, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { bottomMargin=dp(activity,18) })
        }
        wrap.addView(title(activity,title,30f))
        wrap.addView(subtitle(activity,subtitle).apply { setPadding(0,dp(activity,5),0,dp(activity,20)) })
        return wrap
    }

    fun moduleCard(activity: Activity, icon: String, title: String, subtitle: String, accentColor: Int = accent): View {
        val row = LinearLayout(activity).apply {
            orientation=LinearLayout.HORIZONTAL; gravity=Gravity.CENTER_VERTICAL; layoutDirection=View.LAYOUT_DIRECTION_RTL
            setPadding(dp(activity,17),dp(activity,17),dp(activity,17),dp(activity,17)); background=rounded(surface,dp(activity,22),Color.rgb(44,52,76)); isClickable=true
        }
        val badge=TextView(activity).apply { text=icon; textSize=22f; gravity=Gravity.CENTER; setTextColor(accentColor); typeface=Typeface.DEFAULT_BOLD; background=rounded(surface2,dp(activity,16)) }
        row.addView(badge,LinearLayout.LayoutParams(dp(activity,52),dp(activity,52)))
        val texts=LinearLayout(activity).apply { orientation=LinearLayout.VERTICAL; setPadding(dp(activity,14),0,dp(activity,8),0) }
        texts.addView(title(activity,title,17f)); texts.addView(subtitle(activity,subtitle))
        row.addView(texts,LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1f))
        row.addView(TextView(activity).apply { text="‹"; textSize=28f; setTextColor(muted); gravity=Gravity.CENTER },LinearLayout.LayoutParams(dp(activity,26),dp(activity,52)))
        row.layoutParams=LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT).apply{bottomMargin=dp(activity,11)}
        return row
    }

    fun compactButton(activity: Activity, text: String): TextView = TextView(activity).apply {
        this.text=text; textSize=13f; typeface=Typeface.DEFAULT_BOLD; setTextColor(mint); gravity=Gravity.CENTER
        background=rounded(surface2,dp(activity,14),Color.rgb(52,63,92)); setPadding(dp(activity,13),dp(activity,9),dp(activity,13),dp(activity,9)); isClickable=true
    }
}
