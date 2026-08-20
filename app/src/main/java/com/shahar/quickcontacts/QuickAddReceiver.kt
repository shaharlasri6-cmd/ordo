package com.shahar.quickcontacts

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.widget.Toast

class QuickAddReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val mode = intent.getStringExtra("mode") ?: return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
            !Settings.canDrawOverlays(context)
        ) {
            Toast.makeText(
                context,
                "כדי להוסיף ישירות ממסך הבית, אשר ל-Ordo הצגה מעל אפליקציות אחרות",
                Toast.LENGTH_LONG
            ).show()
            try {
                context.startActivity(
                    Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        android.net.Uri.parse("package:${context.packageName}")
                    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            } catch (_: Exception) { }
            return
        }

        val service = Intent(context, QuickAddOverlayService::class.java).apply {
            putExtra("mode", mode)
        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(service)
            } else {
                context.startService(service)
            }
        } catch (_: Exception) {
            Toast.makeText(context, "לא הצלחתי לפתוח את ההוספה המהירה", Toast.LENGTH_SHORT).show()
        }
    }
}
