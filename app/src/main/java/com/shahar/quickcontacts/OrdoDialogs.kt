package com.shahar.quickcontacts

import android.app.Activity
import android.app.Dialog
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.content.Context
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView

object OrdoDialogs {
    private fun card(activity: Activity): LinearLayout = LinearLayout(activity).apply {
        orientation = LinearLayout.VERTICAL
        layoutDirection = View.LAYOUT_DIRECTION_RTL
        setPadding(
            UiKit.dp(activity, 22),
            UiKit.dp(activity, 22),
            UiKit.dp(activity, 22),
            UiKit.dp(activity, 18)
        )
        background = UiKit.rounded(
            Color.rgb(20, 25, 40),
            UiKit.dp(activity, 26),
            Color.rgb(55, 64, 90)
        )
    }

    private fun title(activity: Activity, value: String) = TextView(activity).apply {
        text = value
        textSize = 22f
        typeface = Typeface.DEFAULT_BOLD
        setTextColor(UiKit.ink)
        gravity = Gravity.START
    }

    private fun subtitle(activity: Activity, value: String) = TextView(activity).apply {
        text = value
        textSize = 13f
        setTextColor(UiKit.muted)
        gravity = Gravity.START
        setPadding(0, UiKit.dp(activity, 4), 0, UiKit.dp(activity, 16))
    }

    fun input(
        activity: Activity,
        heading: String,
        description: String,
        hint: String,
        positive: String,
        onSubmit: (String) -> Unit
    ) {
        val dialog = Dialog(activity)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)

        val root = card(activity)
        root.addView(title(activity, heading))
        root.addView(subtitle(activity, description))

        val input = EditText(activity).apply {
            this.hint = hint
            textSize = 16f
            setTextColor(UiKit.ink)
            setHintTextColor(Color.rgb(120, 130, 154))
            setSingleLine(true)
            setPadding(
                UiKit.dp(activity, 15),
                UiKit.dp(activity, 13),
                UiKit.dp(activity, 15),
                UiKit.dp(activity, 13)
            )
            background = UiKit.rounded(
                Color.rgb(13, 18, 31),
                UiKit.dp(activity, 16),
                Color.rgb(52, 61, 87)
            )
        }
        root.addView(
            input,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = UiKit.dp(activity, 18) }
        )

        val buttons = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END
        }

        val cancel = UiKit.compactButton(activity, "ביטול").apply {
            setTextColor(UiKit.muted)
            setOnClickListener { dialog.dismiss() }
        }
        buttons.addView(cancel)

        val save = UiKit.compactButton(activity, positive).apply {
            setTextColor(Color.WHITE)
            background = UiKit.rounded(UiKit.accent, UiKit.dp(activity, 14))
            setOnClickListener {
                val value = input.text.toString().trim()
                if (value.isNotEmpty()) {
                    dialog.dismiss()
                    onSubmit(value)
                }
            }
        }
        buttons.addView(
            save,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { marginStart = UiKit.dp(activity, 10) }
        )

        root.addView(buttons)
        dialog.setContentView(root)

        dialog.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setDimAmount(.56f)
            addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            setLayout(
                (activity.resources.displayMetrics.widthPixels * .90f).toInt(),
                WindowManager.LayoutParams.WRAP_CONTENT
            )
        }

        dialog.setOnShowListener {
            dialog.window?.setLayout(
                (activity.resources.displayMetrics.widthPixels * .90f).toInt(),
                WindowManager.LayoutParams.WRAP_CONTENT
            )
            input.requestFocus()
            input.postDelayed({
                val imm = activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.showSoftInput(input, InputMethodManager.SHOW_IMPLICIT)
            }, 180)
        }
        dialog.show()
    }

    fun twoInputs(
        activity: Activity,
        heading: String,
        description: String,
        firstHint: String,
        secondHint: String,
        positive: String,
        onSubmit: (String, String) -> Unit
    ) {
        val dialog = Dialog(activity)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        val root = card(activity)
        root.addView(title(activity, heading))
        root.addView(subtitle(activity, description))

        fun field(hint: String) = EditText(activity).apply {
            this.hint = hint
            textSize = 16f
            setTextColor(UiKit.ink)
            setHintTextColor(Color.rgb(120, 130, 154))
            setPadding(
                UiKit.dp(activity, 15),
                UiKit.dp(activity, 13),
                UiKit.dp(activity, 15),
                UiKit.dp(activity, 13)
            )
            background = UiKit.rounded(
                Color.rgb(13, 18, 31),
                UiKit.dp(activity, 16),
                Color.rgb(52, 61, 87)
            )
        }

        val first = field(firstHint).apply { setSingleLine(true) }
        val second = field(secondHint)

        root.addView(first, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { bottomMargin = UiKit.dp(activity, 10) })
        root.addView(second, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { bottomMargin = UiKit.dp(activity, 18) })

        val buttons = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END
        }
        buttons.addView(UiKit.compactButton(activity, "ביטול").apply {
            setTextColor(UiKit.muted)
            setOnClickListener { dialog.dismiss() }
        })
        buttons.addView(UiKit.compactButton(activity, positive).apply {
            setTextColor(Color.WHITE)
            background = UiKit.rounded(UiKit.accent, UiKit.dp(activity, 14))
            setOnClickListener {
                val a = first.text.toString().trim()
                val b = second.text.toString().trim()
                if (a.isNotEmpty()) {
                    dialog.dismiss()
                    onSubmit(a, b)
                }
            }
        }, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { marginStart = UiKit.dp(activity, 10) })

        root.addView(buttons)
        dialog.setContentView(root)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setDimAmount(.56f)
        dialog.window?.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        dialog.show()
        dialog.window?.setLayout(
            (activity.resources.displayMetrics.widthPixels * .90f).toInt(),
            WindowManager.LayoutParams.WRAP_CONTENT
        )
    }

    fun choices(
        activity: Activity,
        heading: String,
        description: String,
        labels: Array<String>,
        onSelected: (Int) -> Unit
    ) {
        val dialog = Dialog(activity)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        val root = card(activity)
        root.addView(title(activity, heading))
        root.addView(subtitle(activity, description))

        labels.forEachIndexed { index, label ->
            val item = TextView(activity).apply {
                text = label
                textSize = 16f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(UiKit.ink)
                gravity = Gravity.START or Gravity.CENTER_VERTICAL
                setPadding(
                    UiKit.dp(activity, 16),
                    UiKit.dp(activity, 13),
                    UiKit.dp(activity, 16),
                    UiKit.dp(activity, 13)
                )
                background = UiKit.rounded(
                    Color.rgb(29, 36, 56),
                    UiKit.dp(activity, 15),
                    Color.rgb(51, 61, 88)
                )
                setOnClickListener {
                    dialog.dismiss()
                    onSelected(index)
                }
            }
            root.addView(item, LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = UiKit.dp(activity, 8) })
        }

        root.addView(UiKit.compactButton(activity, "ביטול").apply {
            setTextColor(UiKit.muted)
            setOnClickListener { dialog.dismiss() }
        }, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { gravity = Gravity.END; topMargin = UiKit.dp(activity, 4) })

        dialog.setContentView(root)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setDimAmount(.56f)
        dialog.window?.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        dialog.show()
        dialog.window?.setLayout(
            (activity.resources.displayMetrics.widthPixels * .90f).toInt(),
            WindowManager.LayoutParams.WRAP_CONTENT
        )
    }
}
