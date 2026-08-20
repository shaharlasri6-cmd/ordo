package com.shahar.quickcontacts

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.ContactsContract
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import java.util.Locale
import java.util.concurrent.Executors

class MainActivity : Activity() {
    companion object { private const val MAX_CONTACTS = 30 }
    private lateinit var listBox: LinearLayout
    private lateinit var countText: TextView
    private var selected = mutableListOf<QuickContact>()
    private val io = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())
    private val updateManager by lazy { UpdateManager(this) }

    private val bg = Color.rgb(247, 248, 252)
    private val ink = Color.rgb(26, 29, 38)
    private val muted = Color.rgb(103, 110, 126)
    private val accent = Color.rgb(74, 99, 220)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        selected = ContactStore.load(this).toMutableList()
        buildUi()
        requestNeededPermissions()
        mainHandler.postDelayed({ updateManager.check(false) }, 900)
        mainHandler.postDelayed({ ensureOverlayPermission(false) }, 1400)
    }

    private fun buildUi() {
        window.statusBarColor = bg
        window.navigationBarColor = bg
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setBackgroundColor(bg)
            setPadding(dp(20), dp(18), dp(20), dp(18))
        }

        // Android 15 / HyperOS can draw edge-to-edge. Keep content clear of system bars.
        root.setOnApplyWindowInsetsListener { view, insets ->
            val top: Int
            val bottom: Int
            if (android.os.Build.VERSION.SDK_INT >= 30) {
                val bars = insets.getInsets(WindowInsets.Type.systemBars())
                top = bars.top
                bottom = bars.bottom
            } else {
                @Suppress("DEPRECATION")
                top = insets.systemWindowInsetTop
                @Suppress("DEPRECATION")
                bottom = insets.systemWindowInsetBottom
            }
            view.setPadding(dp(20), top + dp(18), dp(20), bottom + dp(18))
            insets
        }

        val scroll = ScrollView(this).apply {
            isFillViewport = true
            clipToPadding = false
        }
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
        }

        content.addView(TextView(this).apply {
            text = "אנשי קשר מהירים"
            textSize = 29f
            setTextColor(ink)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.START
        })

        content.addView(TextView(this).apply {
            text = "האנשים החשובים לך, ישירות ממסך הבית"
            textSize = 15f
            setTextColor(muted)
            gravity = Gravity.START
            setPadding(0, dp(5), 0, dp(22))
        })

        val add = actionCard("＋", "הוסף איש קשר", "חיפוש מהיר מתוך אנשי הקשר שלך", true)
        add.setOnClickListener {
            if (checkSelfPermission(Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
                openContactSearch()
            } else {
                requestPermissions(arrayOf(Manifest.permission.READ_CONTACTS), 501)
            }
        }
        content.addView(add)

        val widget = actionCard("⌂", "הוסף למסך הבית", "ווידג'ט אנשי הקשר השקוף", false)
        widget.setOnClickListener { pinWidget() }
        content.addView(widget)

        val demoWidget = actionCard("◉", "הוסף ווידג'ט הדגמה", "לחיצה עליו מציגה את אנימציית השיחה בלי להתקשר", false)
        demoWidget.setOnClickListener { pinDemoWidget() }
        content.addView(demoWidget)

        val overlayPermission = actionCard("◎", "אנימציה מעל מסך הבית", "הרשאה חד-פעמית כדי שהאנימציה לא תפתח את האפליקציה", false)
        overlayPermission.setOnClickListener { ensureOverlayPermission(true) }
        content.addView(overlayPermission)

        val updates = actionCard("↻", "בדוק עדכונים", "בדיקה והורדה ישירות מתוך האפליקציה", false)
        updates.setOnClickListener { updateManager.check(true) }
        content.addView(updates)

        val section = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(24), 0, dp(10))
        }
        countText = TextView(this).apply {
            textSize = 13f
            setTextColor(accent)
            gravity = Gravity.CENTER
            background = pill(Color.rgb(233, 237, 255), dp(999))
            setPadding(dp(11), dp(5), dp(11), dp(5))
        }
        section.addView(countText)
        section.addView(TextView(this).apply {
            text = "אנשי הקשר בווידג'ט"
            textSize = 18f
            setTextColor(ink)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.START
            setPadding(dp(10), 0, 0, 0)
        }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        content.addView(section)

        listBox = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        content.addView(listBox)

        content.addView(TextView(this).apply {
            text = "אפשר לבחור עד 30 אנשי קשר. הווידג'ט ניתן לגלילה ומתעדכן מיד."
            textSize = 13f
            setTextColor(muted)
            gravity = Gravity.START
            setPadding(0, dp(14), 0, dp(4))
        })

        scroll.addView(content, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        root.addView(scroll, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        setContentView(root)
        renderSelected()
    }


    private fun ensureOverlayPermission(force: Boolean) {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.M || Settings.canDrawOverlays(this)) {
            if (force) Toast.makeText(this, "האנימציה מעל מסך הבית כבר פעילה", Toast.LENGTH_SHORT).show()
            return
        }

        AlertDialog.Builder(this)
            .setTitle("הפעל אנימציה מעל מסך הבית")
            .setMessage("כדי שהאנימציה תופיע מעל הווידג'ט בלי לפתוח את האפליקציה, Android צריך הרשאת ‘הצגה מעל אפליקציות אחרות’. זו הרשאה חד-פעמית.")
            .setNegativeButton("אחר כך", null)
            .setPositiveButton("אפשר עכשיו") { _, _ ->
                try {
                    startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
                } catch (_: Exception) {
                    startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION))
                }
            }
            .show()
    }

    private fun actionCard(symbol: String, title: String, subtitle: String, primary: Boolean): View {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setPadding(dp(16), dp(14), dp(16), dp(14))
            background = rounded(if (primary) Color.WHITE else Color.rgb(252, 252, 254), dp(20), Color.rgb(228, 231, 239))
            isClickable = true
            isFocusable = true
        }
        val symbolView = TextView(this).apply {
            text = symbol
            textSize = 22f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setTextColor(if (primary) Color.WHITE else accent)
            background = pill(if (primary) accent else Color.rgb(232, 236, 255), dp(16))
        }
        row.addView(symbolView, LinearLayout.LayoutParams(dp(48), dp(48)))

        val texts = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), 0, dp(10), 0)
        }
        texts.addView(TextView(this).apply {
            text = title
            textSize = 17f
            setTextColor(ink)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.START
        })
        texts.addView(TextView(this).apply {
            text = subtitle
            textSize = 13f
            setTextColor(muted)
            gravity = Gravity.START
            setPadding(0, dp(2), 0, 0)
        })
        row.addView(texts, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        row.addView(TextView(this).apply {
            text = "‹"
            textSize = 28f
            setTextColor(Color.rgb(160, 165, 178))
            gravity = Gravity.CENTER
        }, LinearLayout.LayoutParams(dp(28), dp(48)))

        val lp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        lp.bottomMargin = dp(10)
        row.layoutParams = lp
        return row
    }

    private fun renderSelected() {
        if (!::listBox.isInitialized) return
        countText.text = "${selected.size}/$MAX_CONTACTS"
        listBox.removeAllViews()

        if (selected.isEmpty()) {
            listBox.addView(TextView(this).apply {
                text = "עוד לא בחרת אף אחד\nלחץ על ‘הוסף איש קשר’ כדי להתחיל"
                textSize = 14f
                gravity = Gravity.CENTER
                setTextColor(muted)
                setPadding(dp(16), dp(28), dp(16), dp(28))
                background = rounded(Color.WHITE, dp(20), Color.rgb(229, 232, 240))
            })
            return
        }

        selected.forEachIndexed { index, contact ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                layoutDirection = View.LAYOUT_DIRECTION_RTL
                setPadding(dp(14), dp(11), dp(12), dp(11))
                background = rounded(Color.WHITE, dp(18), Color.rgb(230, 233, 241))
            }

            val avatar = TextView(this).apply {
                text = contact.name.trim().take(1).ifBlank { "•" }
                textSize = 20f
                typeface = Typeface.DEFAULT_BOLD
                gravity = Gravity.CENTER
                setTextColor(Color.WHITE)
                background = pill(accent, dp(999))
            }
            row.addView(avatar, LinearLayout.LayoutParams(dp(46), dp(46)))

            val textCol = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(12), 0, dp(12), 0)
            }
            textCol.addView(TextView(this).apply {
                text = contact.name
                textSize = 16f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(ink)
                gravity = Gravity.START
                maxLines = 1
            })
            textCol.addView(TextView(this).apply {
                text = contact.number
                textSize = 12f
                setTextColor(muted)
                gravity = Gravity.START
                maxLines = 1
            })
            row.addView(textCol, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))

            row.addView(smallButton("↑", index > 0) {
                val item = selected.removeAt(index)
                selected.add(index - 1, item)
                saveAndRefresh()
            })
            row.addView(smallButton("↓", index < selected.lastIndex) {
                val item = selected.removeAt(index)
                selected.add(index + 1, item)
                saveAndRefresh()
            })
            row.addView(smallButton("×", true, danger = true) {
                selected.removeAt(index)
                saveAndRefresh()
            })

            val lp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            lp.bottomMargin = dp(8)
            listBox.addView(row, lp)
        }
    }

    private fun smallButton(label: String, enabled: Boolean, danger: Boolean = false, action: () -> Unit): TextView {
        return TextView(this).apply {
            text = label
            textSize = 18f
            gravity = Gravity.CENTER
            isEnabled = enabled
            alpha = if (enabled) 1f else .28f
            setTextColor(if (danger) Color.rgb(178, 65, 70) else Color.rgb(87, 94, 111))
            background = pill(if (danger) Color.rgb(255, 240, 241) else Color.rgb(244, 246, 250), dp(12))
            setOnClickListener { if (enabled) action() }
            val lp = LinearLayout.LayoutParams(dp(38), dp(38))
            lp.marginStart = dp(5)
            layoutParams = lp
        }
    }

    private fun openContactSearch() {
        if (selected.size >= MAX_CONTACTS) {
            Toast.makeText(this, "כבר נבחרו $MAX_CONTACTS אנשי קשר", Toast.LENGTH_SHORT).show()
            return
        }
        val dialogRoot = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setPadding(dp(18), dp(8), dp(18), dp(10))
        }
        val search = EditText(this).apply {
            hint = "חפש לפי שם או מספר…"
            textSize = 16f
            setSingleLine(true)
            gravity = Gravity.START
            setPadding(dp(14), dp(10), dp(14), dp(10))
            background = rounded(Color.rgb(246, 247, 251), dp(16), Color.rgb(224, 227, 236))
            isEnabled = false
        }
        dialogRoot.addView(search, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(50)))

        val results = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val resultsScroll = ScrollView(this).apply { addView(results) }
        val scrollLp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(430))
        scrollLp.topMargin = dp(10)
        dialogRoot.addView(resultsScroll, scrollLp)

        val dialog = AlertDialog.Builder(this)
            .setTitle("הוסף אנשי קשר")
            .setView(dialogRoot)
            .setNegativeButton("סגור", null)
            .create()

        var all: List<QuickContact> = emptyList()
        var renderToken = 0

        fun showRows(query: String) {
            val token = ++renderToken
            val q = query.trim().lowercase(Locale.getDefault())
            val selectedKeys = selected.asSequence().map { "${it.id}:${it.number}" }.toHashSet()
            val filtered = all.asSequence()
                .filterNot { "${it.id}:${it.number}" in selectedKeys }
                .filter { q.isBlank() || it.name.lowercase(Locale.getDefault()).contains(q) || it.number.contains(q) }
                .take(30)
                .toList()

            if (token != renderToken) return
            results.removeAllViews()
            filtered.forEach { contact ->
                val row = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    layoutDirection = View.LAYOUT_DIRECTION_RTL
                    setPadding(dp(10), dp(10), dp(10), dp(10))
                    isClickable = true
                }
                row.addView(TextView(this).apply {
                    text = contact.name.trim().take(1).ifBlank { "•" }
                    textSize = 18f
                    typeface = Typeface.DEFAULT_BOLD
                    gravity = Gravity.CENTER
                    setTextColor(Color.WHITE)
                    background = pill(accent, dp(999))
                }, LinearLayout.LayoutParams(dp(42), dp(42)))

                val labels = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(dp(12), 0, dp(12), 0)
                }
                labels.addView(TextView(this).apply {
                    text = contact.name
                    textSize = 16f
                    setTextColor(ink)
                    typeface = Typeface.DEFAULT_BOLD
                    gravity = Gravity.START
                })
                labels.addView(TextView(this).apply {
                    text = contact.number
                    textSize = 12f
                    setTextColor(muted)
                    gravity = Gravity.START
                })
                row.addView(labels, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
                row.addView(TextView(this).apply {
                    text = "＋"
                    textSize = 22f
                    setTextColor(accent)
                    gravity = Gravity.CENTER
                }, LinearLayout.LayoutParams(dp(40), dp(40)))

                row.setOnClickListener {
                    if (selected.size >= MAX_CONTACTS) {
                        Toast.makeText(this, "כבר נבחרו $MAX_CONTACTS אנשי קשר", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }
                    selected.add(contact)
                    ContactStore.save(this, selected)
                    QuickContactsWidget.refreshAll(this)
                    renderSelected()
                    showRows(search.text?.toString().orEmpty())
                }
                results.addView(row, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
                results.addView(View(this).apply { setBackgroundColor(Color.rgb(236, 238, 243)) }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(1)))
            }

            if (filtered.isEmpty()) {
                results.addView(TextView(this).apply {
                    text = if (all.isEmpty()) "טוען אנשי קשר…" else "לא נמצאו תוצאות"
                    textSize = 14f
                    setTextColor(muted)
                    gravity = Gravity.CENTER
                    setPadding(0, dp(30), 0, dp(30))
                })
            }
        }

        search.addTextChangedListener(object : TextWatcher {
            private var pending: Runnable? = null
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                pending?.let(mainHandler::removeCallbacks)
                val value = s?.toString().orEmpty()
                pending = Runnable { showRows(value) }.also { mainHandler.postDelayed(it, 120) }
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        dialog.setOnShowListener {
            showRows("")
            io.execute {
                val loaded = loadPhoneContacts()
                mainHandler.post {
                    if (!dialog.isShowing) return@post
                    all = loaded
                    search.isEnabled = true
                    showRows(search.text?.toString().orEmpty())
                    search.requestFocus()
                }
            }
        }
        dialog.show()
    }

    private fun loadPhoneContacts(): List<QuickContact> {
        val result = LinkedHashMap<String, QuickContact>()
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER
        )
        contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            projection, null, null,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " COLLATE LOCALIZED ASC"
        )?.use { cursor ->
            val idIx = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
            val nameIx = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val numIx = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)
            while (cursor.moveToNext()) {
                val c = QuickContact(cursor.getLong(idIx), cursor.getString(nameIx) ?: "איש קשר", cursor.getString(numIx) ?: "")
                if (c.number.isNotBlank()) result["${c.id}:${c.number}"] = c
            }
        }
        return result.values.toList()
    }

    private fun pinWidget() {
        val manager = AppWidgetManager.getInstance(this)
        val provider = ComponentName(this, QuickContactsWidget::class.java)
        if (manager.isRequestPinAppWidgetSupported) {
            manager.requestPinAppWidget(provider, null, null)
        } else {
            Toast.makeText(this, "במסך הבית: לחיצה ארוכה → ווידג'טים → Quick Contacts", Toast.LENGTH_LONG).show()
        }
    }


    private fun pinDemoWidget() {
        val manager = AppWidgetManager.getInstance(this)
        val provider = ComponentName(this, DemoCallWidget::class.java)
        if (manager.isRequestPinAppWidgetSupported) {
            manager.requestPinAppWidget(provider, null, null)
        } else {
            Toast.makeText(this, "במסך הבית: לחיצה ארוכה → ווידג'טים → Quick Contacts Demo", Toast.LENGTH_LONG).show()
        }
    }

    private fun requestNeededPermissions() {
        val needed = mutableListOf<String>()
        if (checkSelfPermission(Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) needed += Manifest.permission.READ_CONTACTS
        if (checkSelfPermission(Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) needed += Manifest.permission.CALL_PHONE
        if (needed.isNotEmpty()) requestPermissions(needed.toTypedArray(), 500)
    }

    private fun saveAndRefresh() {
        ContactStore.save(this, selected)
        renderSelected()
        QuickContactsWidget.refreshAll(this)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 501 && checkSelfPermission(Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
            openContactSearch()
        }
    }

    override fun onDestroy() {
        io.shutdownNow()
        mainHandler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    private fun rounded(fill: Int, radius: Int, stroke: Int? = null) = GradientDrawable().apply {
        cornerRadius = radius.toFloat()
        setColor(fill)
        if (stroke != null) setStroke(dp(1), stroke)
    }

    private fun pill(fill: Int, radius: Int) = GradientDrawable().apply {
        cornerRadius = radius.toFloat()
        setColor(fill)
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()
}
