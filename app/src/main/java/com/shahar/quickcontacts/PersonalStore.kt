package com.shahar.quickcontacts

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class TaskItem(
    val id: Long,
    val title: String,
    val done: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

data class ReminderItem(
    val id: Long,
    val title: String,
    val atMillis: Long,
    val alertMode: String
)

data class EventItem(
    val id: Long,
    val title: String,
    val atMillis: Long,
    val note: String = ""
)

object PersonalStore {
    private const val PREFS = "ordo_personal"
    private const val TASKS = "tasks"
    private const val REMINDERS = "reminders"
    private const val EVENTS = "events"

    fun loadTasks(context: Context): MutableList<TaskItem> {
        val a = array(context, TASKS)
        return MutableList(a.length()) { i ->
            val o = a.getJSONObject(i)
            TaskItem(
                o.getLong("id"),
                o.getString("title"),
                o.optBoolean("done", false),
                o.optLong("createdAt", 0L)
            )
        }
    }

    fun saveTasks(context: Context, items: List<TaskItem>) {
        val a = JSONArray()
        items.forEach {
            a.put(JSONObject()
                .put("id", it.id)
                .put("title", it.title)
                .put("done", it.done)
                .put("createdAt", it.createdAt))
        }
        save(context, TASKS, a)
    }

    fun loadReminders(context: Context): MutableList<ReminderItem> {
        val a = array(context, REMINDERS)
        return MutableList(a.length()) { i ->
            val o = a.getJSONObject(i)
            ReminderItem(
                o.getLong("id"),
                o.getString("title"),
                o.getLong("atMillis"),
                o.optString("alertMode", "sound")
            )
        }
    }

    fun saveReminders(context: Context, items: List<ReminderItem>) {
        val a = JSONArray()
        items.forEach {
            a.put(JSONObject()
                .put("id", it.id)
                .put("title", it.title)
                .put("atMillis", it.atMillis)
                .put("alertMode", it.alertMode))
        }
        save(context, REMINDERS, a)
    }

    fun loadEvents(context: Context): MutableList<EventItem> {
        val a = array(context, EVENTS)
        return MutableList(a.length()) { i ->
            val o = a.getJSONObject(i)
            EventItem(
                o.getLong("id"),
                o.getString("title"),
                o.getLong("atMillis"),
                o.optString("note", "")
            )
        }
    }

    fun saveEvents(context: Context, items: List<EventItem>) {
        val a = JSONArray()
        items.forEach {
            a.put(JSONObject()
                .put("id", it.id)
                .put("title", it.title)
                .put("atMillis", it.atMillis)
                .put("note", it.note))
        }
        save(context, EVENTS, a)
    }

    private fun array(context: Context, key: String): JSONArray {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(key, "[]") ?: "[]"
        return try { JSONArray(raw) } catch (_: Exception) { JSONArray() }
    }

    private fun save(context: Context, key: String, array: JSONArray) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(key, array.toString()).apply()
    }
}
