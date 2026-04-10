package com.algo1127.mytask.NotifAi.persistence

import android.content.Context
import android.content.SharedPreferences
import com.algo1127.mytask.ui.models.*
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class Persistence(private val context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("mytask_data", Context.MODE_PRIVATE)
    private val gson: Gson = GsonBuilder()
        .registerTypeAdapter(LocalTime::class.java, object : com.google.gson.JsonSerializer<LocalTime>, com.google.gson.JsonDeserializer<LocalTime> {
            private val fmt = DateTimeFormatter.ISO_LOCAL_TIME
            override fun serialize(src: LocalTime, typeOfSrc: java.lang.reflect.Type, context: com.google.gson.JsonSerializationContext) =
                com.google.gson.JsonPrimitive(src.format(fmt))
            override fun deserialize(json: com.google.gson.JsonElement, typeOfT: java.lang.reflect.Type, context: com.google.gson.JsonDeserializationContext) =
                LocalTime.parse(json.asString, fmt)
        })
        .registerTypeAdapter(LocalDateTime::class.java, object : com.google.gson.JsonSerializer<LocalDateTime>, com.google.gson.JsonDeserializer<LocalDateTime> {
            private val fmt = DateTimeFormatter.ISO_LOCAL_DATE_TIME
            override fun serialize(src: LocalDateTime, typeOfSrc: java.lang.reflect.Type, context: com.google.gson.JsonSerializationContext) =
                com.google.gson.JsonPrimitive(src.format(fmt))
            override fun deserialize(json: com.google.gson.JsonElement, typeOfT: java.lang.reflect.Type, context: com.google.gson.JsonDeserializationContext) =
                LocalDateTime.parse(json.asString, fmt)
        })
        .create()

    // ==================== TASKS ====================
    fun saveTask(task: Task) {
        val tasks = getTasks().toMutableList()
        val idx = tasks.indexOfFirst { it.id == task.id }
        if (idx >= 0) tasks[idx] = task else tasks.add(task)
        prefs.edit().putString("tasks", gson.toJson(tasks)).apply()
    }

    fun getTasks(): List<Task> {
        return try {
            val json = prefs.getString("tasks", null)
            if (json.isNullOrEmpty()) emptyList() else
                gson.fromJson(json, object : TypeToken<List<Task>>(){}.type)
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun deleteTask(taskId: Long) {
        val tasks = getTasks().filter { it.id != taskId }
        prefs.edit().putString("tasks", gson.toJson(tasks)).apply()
    }

    // ==================== REMINDERS ====================
    fun saveReminder(reminder: ReminderItem) {
        val reminders = getReminders().toMutableList()
        val idx = reminders.indexOfFirst { it.id == reminder.id }
        if (idx >= 0) reminders[idx] = reminder else reminders.add(reminder)
        prefs.edit().putString("reminders", gson.toJson(reminders)).apply()
    }

    fun getReminders(): List<ReminderItem> {
        return try {
            val json = prefs.getString("reminders", null)
            if (json.isNullOrEmpty()) emptyList() else
                gson.fromJson(json, object : TypeToken<List<ReminderItem>>(){}.type)
        } catch (e: Exception) {
            emptyList()
        }
    }

    // ==================== EVENTS ====================
    fun saveEvent(event: EventItem) {
        val events = getEvents().toMutableList()
        val idx = events.indexOfFirst { it.id == event.id }
        if (idx >= 0) events[idx] = event else events.add(event)
        prefs.edit().putString("events", gson.toJson(events)).apply()
    }

    fun getEvents(): List<EventItem> {
        return try {
            val json = prefs.getString("events", null)
            if (json.isNullOrEmpty()) emptyList() else
                gson.fromJson(json, object : TypeToken<List<EventItem>>(){}.type)
        } catch (e: Exception) {
            emptyList()
        }
    }

    // ==================== AI STATE (Transferable ✅) ====================
    fun saveAiState(trustScore: Float, preferences: Map<String, String>) {
        val state = mapOf("trust" to trustScore, "prefs" to preferences)
        prefs.edit().putString("ai_state", gson.toJson(state)).apply()
    }

    fun getAiState(): Pair<Float, Map<String, String>> {
        return try {
            val json = prefs.getString("ai_state", null)
            if (json.isNullOrEmpty()) {
                Pair(0.5f, emptyMap())
            } else {
                @Suppress("UNCHECKED_CAST")
                val map = gson.fromJson(json, object : TypeToken<Map<String, Any>>(){}.type) as Map<String, Any>
                val trust = (map["trust"] as? Double)?.toFloat() ?: 0.5f
                @Suppress("UNCHECKED_CAST")
                val prefs = (map["prefs"] as? Map<*, *>)?.mapKeys { it.key.toString() }?.mapValues { it.value.toString() } ?: emptyMap()
                Pair(trust, prefs)
            }
        } catch (e: Exception) {
            Pair(0.5f, emptyMap())
        }
    }

    // ==================== SETTINGS ====================
    fun saveSettings(key: String, value: String) {
        prefs.edit().putString("setting_$key", value).apply()
    }

    fun getSettings(key: String, default: String = ""): String {
        return prefs.getString("setting_$key", default) ?: default
    }

    fun getSettingsBoolean(key: String, default: Boolean = false): Boolean {
        return prefs.getBoolean("setting_$key", default)
    }

    // ==================== CLEANUP ====================
    fun clearSacredTrainingData() {
        // v2: clears usage stats only
        prefs.edit().remove("usage_heatmap").remove("effectiveness_logs").apply()
    }

    fun clearAllData() {
        prefs.edit().clear().apply()
    }
}