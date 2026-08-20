package com.shahar.quickcontacts

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File

class UpdateDownloadReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != DownloadManager.ACTION_DOWNLOAD_COMPLETE) return

        val completedId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)
        if (completedId <= 0) return

        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val expectedId = prefs.getLong(KEY_DOWNLOAD_ID, -1L)
        if (completedId != expectedId) return

        val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val cursor = manager.query(DownloadManager.Query().setFilterById(completedId))
        cursor.use {
            if (!it.moveToFirst()) return
            val status = it.getInt(it.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
            if (status != DownloadManager.STATUS_SUCCESSFUL) {
                prefs.edit().remove(KEY_DOWNLOAD_ID).apply()
                Toast.makeText(context, "הורדת העדכון נכשלה", Toast.LENGTH_LONG).show()
                return
            }

            val localUri = it.getString(it.getColumnIndexOrThrow(DownloadManager.COLUMN_LOCAL_URI))
            prefs.edit().remove(KEY_DOWNLOAD_ID).apply()

            if (!context.packageManager.canRequestPackageInstalls()) {
                prefs.edit()
                    .putString(KEY_PENDING_APK_URI, localUri)
                    .apply()

                val settingsIntent = Intent(
                    Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:${context.packageName}")
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                try {
                    context.startActivity(settingsIntent)
                } catch (_: Exception) {
                    Toast.makeText(
                        context,
                        "יש לאשר ל-Quick Contacts התקנת אפליקציות ממקור זה",
                        Toast.LENGTH_LONG
                    ).show()
                }
                return
            }

            openInstaller(context, localUri)
        }
    }

    companion object {
        const val PREFS = "update_state"
        const val KEY_DOWNLOAD_ID = "download_id"
        const val KEY_PENDING_APK_URI = "pending_apk_uri"

        fun resumePendingInstaller(context: Context) {
            val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            val uri = prefs.getString(KEY_PENDING_APK_URI, null) ?: return
            if (!context.packageManager.canRequestPackageInstalls()) return
            prefs.edit().remove(KEY_PENDING_APK_URI).apply()
            openInstaller(context, uri)
        }

        private fun openInstaller(context: Context, localUri: String) {
            try {
                val source = Uri.parse(localUri)
                val apkUri = when (source.scheme) {
                    "file" -> {
                        val file = File(requireNotNull(source.path))
                        FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.fileprovider",
                            file
                        )
                    }
                    "content" -> source
                    else -> source
                }

                val install = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(apkUri, "application/vnd.android.package-archive")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(install)
            } catch (_: Exception) {
                Toast.makeText(
                    context,
                    "העדכון ירד, אבל לא הצלחתי לפתוח את מסך ההתקנה",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}
