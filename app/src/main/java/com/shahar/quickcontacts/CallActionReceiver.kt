package com.shahar.quickcontacts

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.widget.Toast

class CallActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val demo = intent.getBooleanExtra("demo", false)
        val name = intent.getStringExtra("name").orEmpty().ifBlank { if (demo) "הדגמה" else "איש קשר" }
        val number = intent.getStringExtra("number").orEmpty()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) {
            Toast.makeText(context, "פתח את Ordo ואשר ‘אנימציה מעל מסך הבית’", Toast.LENGTH_LONG).show()
            return
        }

        val service = Intent(context, CallOverlayService::class.java).apply {
            putExtra("demo", demo)
            putExtra("name", name)
            putExtra("number", number)
        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(service)
            else context.startService(service)
        } catch (_: Exception) {
            Toast.makeText(context, "לא הצלחתי להציג את אנימציית השיחה", Toast.LENGTH_SHORT).show()
        }
    }
}
