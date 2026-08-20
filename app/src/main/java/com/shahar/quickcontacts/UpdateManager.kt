package com.shahar.quickcontacts

import android.app.Activity
import android.app.AlertDialog
import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

class UpdateManager(private val activity: Activity) {
    companion object {
        private const val LATEST_API =
            "https://api.github.com/repos/shaharlasri6-cmd/ordo/releases/latest"
        private const val APK_MIME = "application/vnd.android.package-archive"
    }

    private val io = Executors.newSingleThreadExecutor()
    private val main = Handler(Looper.getMainLooper())

    fun check(showUpToDate: Boolean = false) {
        io.execute {
            try {
                val release = fetchLatest()
                main.post {
                    if (isNewer(release.version, BuildConfig.VERSION_NAME)) {
                        showUpdate(release)
                    } else if (showUpToDate) {
                        Toast.makeText(
                            activity,
                            "האפליקציה מעודכנת לגרסה ${BuildConfig.VERSION_NAME}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } catch (_: Exception) {
                if (showUpToDate) main.post {
                    Toast.makeText(
                        activity,
                        "לא הצלחתי לבדוק עדכונים כרגע",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    fun resumePendingInstaller() {
        UpdateDownloadReceiver.resumePendingInstaller(activity)
    }

    private fun fetchLatest(): ReleaseInfo {
        val connection = (URL(LATEST_API).openConnection() as HttpURLConnection).apply {
            connectTimeout = 5000
            readTimeout = 5000
            requestMethod = "GET"
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("User-Agent", "Ordo/${BuildConfig.VERSION_NAME}")
        }

        try {
            if (connection.responseCode !in 200..299) error("HTTP ${connection.responseCode}")
            val json = JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
            val tag = json.getString("tag_name")
            val version = tag.removePrefix("v")
            val assets = json.getJSONArray("assets")
            var apkUrl: String? = null

            for (i in 0 until assets.length()) {
                val asset = assets.getJSONObject(i)
                if (asset.optString("name").endsWith(".apk", ignoreCase = true)) {
                    apkUrl = asset.getString("browser_download_url")
                    break
                }
            }

            return ReleaseInfo(version, tag, apkUrl ?: error("No APK asset"))
        } finally {
            connection.disconnect()
        }
    }

    private fun showUpdate(release: ReleaseInfo) {
        if (activity.isFinishing || activity.isDestroyed) return

        AlertDialog.Builder(activity)
            .setTitle("עדכון חדש זמין")
            .setMessage(
                "גרסה ${release.version} זמינה.\n\n" +
                    "לחיצה על „עדכן עכשיו” תוריד את ה-APK ותפתח אוטומטית את התקנת Android."
            )
            .setNegativeButton("אחר כך", null)
            .setPositiveButton("עדכן עכשיו") { _, _ ->
                download(release)
            }
            .show()
    }

    private fun download(release: ReleaseInfo) {
        try {
            val manager =
                activity.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val fileName = "Ordo-${release.tag}.apk"

            val request = DownloadManager.Request(Uri.parse(release.apkUrl))
                .setTitle("Ordo ${release.version}")
                .setDescription("מוריד ומכין עדכון…")
                .setMimeType(APK_MIME)
                .setNotificationVisibility(
                    DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED
                )
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(true)
                .setDestinationInExternalPublicDir(
                    Environment.DIRECTORY_DOWNLOADS,
                    fileName
                )

            val id = manager.enqueue(request)
            activity.getSharedPreferences(
                UpdateDownloadReceiver.PREFS,
                Context.MODE_PRIVATE
            ).edit()
                .putLong(UpdateDownloadReceiver.KEY_DOWNLOAD_ID, id)
                .apply()

            Toast.makeText(
                activity,
                "העדכון יורד. בסיום מסך ההתקנה ייפתח אוטומטית.",
                Toast.LENGTH_LONG
            ).show()
        } catch (_: Exception) {
            Toast.makeText(
                activity,
                "לא הצלחתי להתחיל את ההורדה",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun isNewer(remote: String, local: String): Boolean {
        val r = remote.split('.').map { it.toIntOrNull() ?: 0 }
        val l = local.split('.').map { it.toIntOrNull() ?: 0 }
        val n = maxOf(r.size, l.size)

        for (i in 0 until n) {
            val rv = r.getOrElse(i) { 0 }
            val lv = l.getOrElse(i) { 0 }
            if (rv != lv) return rv > lv
        }
        return false
    }

    private data class ReleaseInfo(
        val version: String,
        val tag: String,
        val apkUrl: String
    )
}
