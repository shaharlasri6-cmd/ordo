package com.shahar.quickcontacts

import android.content.Context
import android.net.Uri

data class QuickContact(val id: Long, val name: String, val number: String)

object ContactStore {
    private const val PREFS = "quick_contacts_prefs"
    private const val KEY = "selected_contacts"

    fun save(context: Context, contacts: List<QuickContact>) {
        val value = contacts.joinToString("\n") {
            "${it.id}|${Uri.encode(it.name)}|${Uri.encode(it.number)}"
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY, value).apply()
    }

    fun load(context: Context): List<QuickContact> {
        val value = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY, "") ?: ""
        if (value.isBlank()) return emptyList()
        return value.lines().mapNotNull { line ->
            val parts = line.split('|', limit = 3)
            if (parts.size != 3) null else {
                val id = parts[0].toLongOrNull() ?: return@mapNotNull null
                QuickContact(id, Uri.decode(parts[1]), Uri.decode(parts[2]))
            }
        }
    }
}
