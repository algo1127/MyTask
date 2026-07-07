package com.algo1127.mytask.NotifAi.persistence

import android.content.Context
import android.content.SharedPreferences
import com.algo1127.mytask.data.MyTaskDatabase
import com.algo1127.mytask.ui.models.*
import com.google.gson.*
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class Persistence(private val context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("mytask_data", Context.MODE_PRIVATE)
    private val database = MyTaskDatabase.getDatabase(context)
    private val taskDao = database.taskDao()
    private val reminderDao = database.reminderDao()
    private val eventDao = database.eventDao()

    private val gson: Gson = GsonBuilder()
        .registerTypeAdapter(LocalTime::class.java, object : JsonSerializer<LocalTime>, JsonDeserializer<LocalTime> {
            private val fmt = DateTimeFormatter.ISO_LOCAL_TIME
            override fun serialize(src: LocalTime, typeOfSrc: java.lang.reflect.Type, context: JsonSerializationContext) =
                JsonPrimitive(src.format(fmt))
            override fun deserialize(json: JsonElement, typeOfT: java.lang.reflect.Type, context: JsonDeserializationContext) =
                try { 
                    if (json.isJsonPrimitive) LocalTime.parse(json.asString, fmt) 
                    else LocalTime.NOON 
                } catch (e: Exception) { LocalTime.NOON }
        })
        .registerTypeAdapter(LocalDate::class.java, object : JsonSerializer<LocalDate>, JsonDeserializer<LocalDate> {
            private val fmt = DateTimeFormatter.ISO_LOCAL_DATE
            override fun serialize(src: LocalDate, typeOfSrc: java.lang.reflect.Type, context: JsonSerializationContext) =
                JsonPrimitive(src.format(fmt))
            override fun deserialize(json: JsonElement, typeOfT: java.lang.reflect.Type, context: JsonDeserializationContext) =
                try { 
                    if (json.isJsonPrimitive) LocalDate.parse(json.asString, fmt) 
                    else LocalDate.now() 
                } catch (e: Exception) { LocalDate.now() }
        })
        .registerTypeAdapter(LocalDateTime::class.java, object : JsonSerializer<LocalDateTime>, JsonDeserializer<LocalDateTime> {
            private val fmt = DateTimeFormatter.ISO_LOCAL_DATE_TIME
            override fun serialize(src: LocalDateTime, typeOfSrc: java.lang.reflect.Type, context: JsonSerializationContext) =
                JsonPrimitive(src.format(fmt))
            override fun deserialize(json: JsonElement, typeOfT: java.lang.reflect.Type, context: JsonDeserializationContext) =
                try { 
                    if (json.isJsonPrimitive) LocalDateTime.parse(json.asString, fmt) 
                    else LocalDateTime.now() 
                } catch (e: Exception) { LocalDateTime.now() }
        })
        .registerTypeAdapter(Duration::class.java, object : JsonSerializer<Duration>, JsonDeserializer<Duration> {
            override fun serialize(src: Duration, typeOfSrc: java.lang.reflect.Type, context: JsonSerializationContext) =
                JsonPrimitive(src.toMillis())
            override fun deserialize(json: JsonElement, typeOfT: java.lang.reflect.Type, context: JsonDeserializationContext) =
                try { json.asLong.let { Duration.ofMillis(it) } } catch (e: Exception) { Duration.ZERO }
        })
        .registerTypeHierarchyAdapter(TimePreference::class.java, object : JsonSerializer<TimePreference>, JsonDeserializer<TimePreference> {
            override fun serialize(src: TimePreference, typeOfSrc: java.lang.reflect.Type, context: JsonSerializationContext): JsonElement {
                val obj = JsonObject()
                when (src) {
                    is TimePreference.Fixed -> {
                        obj.addProperty("type", "Fixed")
                        obj.addProperty("time", src.time.format(DateTimeFormatter.ISO_LOCAL_TIME))
                    }
                    is TimePreference.LaterToday -> obj.addProperty("type", "LaterToday")
                    is TimePreference.Tomorrow -> obj.addProperty("type", "Tomorrow")
                    is TimePreference.Window -> {
                        obj.addProperty("type", "Window")
                        obj.addProperty("startHour", src.startHour)
                        obj.addProperty("endHour", src.endHour)
                    }
                    is TimePreference.AiDecide -> obj.addProperty("type", "AiDecide")
                }
                return obj
            }

            override fun deserialize(json: JsonElement, typeOfT: java.lang.reflect.Type, context: JsonDeserializationContext): TimePreference {
                return try {
                    if (json.isJsonObject) {
                        val obj = json.asJsonObject
                        when (obj.get("type")?.asString) {
                            "Fixed" -> TimePreference.Fixed(LocalTime.parse(obj.get("time").asString, DateTimeFormatter.ISO_LOCAL_TIME))
                            "LaterToday" -> TimePreference.LaterToday
                            "Tomorrow" -> TimePreference.Tomorrow
                            "Window" -> TimePreference.Window(obj.get("startHour").asInt, obj.get("endHour").asInt)
                            "AiDecide" -> TimePreference.AiDecide
                            else -> TimePreference.Fixed(LocalTime.NOON)
                        }
                    } else {
                        // Legacy string format
                        val value = json.asString
                        when {
                            value.startsWith("FIXED:") -> TimePreference.Fixed(LocalTime.parse(value.removePrefix("FIXED:"), DateTimeFormatter.ISO_LOCAL_TIME))
                            value == "LATER_TODAY" -> TimePreference.LaterToday
                            value == "TOMORROW" -> TimePreference.Tomorrow
                            value.startsWith("WINDOW:") -> {
                                val parts = value.removePrefix("WINDOW:").split(":")
                                TimePreference.Window(parts[0].toInt(), parts[1].toInt())
                            }
                            value == "AI_DECIDE" -> TimePreference.AiDecide
                            else -> TimePreference.Fixed(LocalTime.NOON)
                        }
                    }
                } catch (e: Exception) {
                    TimePreference.Fixed(LocalTime.NOON)
                }
            }
        })
        .create()

    suspend fun migrateIfNecessary() = withContext(Dispatchers.IO) {
        if (!prefs.getBoolean("room_migrated", false)) {
            // Migrate Tasks
            val tasksJson = prefs.getString("tasks", null)
            if (!tasksJson.isNullOrEmpty()) {
                try {
                    val tasks: List<Task> = gson.fromJson(tasksJson, object : TypeToken<List<Task>>(){}.type)
                    tasks.forEach { taskDao.insertTask(it) }
                } catch (e: Exception) {
                    android.util.Log.e("Persistence", "Failed to migrate tasks", e)
                }
            }

            // Migrate Reminders
            val remindersJson = prefs.getString("reminders", null)
            if (!remindersJson.isNullOrEmpty()) {
                try {
                    val reminders: List<ReminderItem> = gson.fromJson(remindersJson, object : TypeToken<List<ReminderItem>>(){}.type)
                    reminders.forEach { reminderDao.insertReminder(it) }
                } catch (e: Exception) {
                    android.util.Log.e("Persistence", "Failed to migrate reminders", e)
                }
            }

            // Migrate Events
            val eventsJson = prefs.getString("events", null)
            if (!eventsJson.isNullOrEmpty()) {
                try {
                    val events: List<EventItem> = gson.fromJson(eventsJson, object : TypeToken<List<EventItem>>(){}.type)
                    events.forEach { eventDao.insertEvent(it) }
                } catch (e: Exception) {
                    android.util.Log.e("Persistence", "Failed to migrate events", e)
                }
            }

            prefs.edit().putBoolean("room_migrated", true).apply()
        }
    }

    // ==================== TASKS ====================
    suspend fun saveTask(task: Task) = withContext(Dispatchers.IO) {
        taskDao.insertTask(task)
    }

    suspend fun getTasks(): List<Task> = withContext(Dispatchers.IO) {
        taskDao.getAllTasks()
    }

    suspend fun deleteTask(taskId: Long) = withContext(Dispatchers.IO) {
        taskDao.deleteTaskById(taskId)
    }

    // ==================== REMINDERS ====================
    suspend fun saveReminder(reminder: ReminderItem) = withContext(Dispatchers.IO) {
        reminderDao.insertReminder(reminder)
    }

    suspend fun getReminders(): List<ReminderItem> = withContext(Dispatchers.IO) {
        reminderDao.getAllReminders()
    }

    // ==================== EVENTS ====================
    suspend fun saveEvent(event: EventItem) = withContext(Dispatchers.IO) {
        eventDao.insertEvent(event)
    }

    suspend fun getEvents(): List<EventItem> = withContext(Dispatchers.IO) {
        eventDao.getAllEvents()
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
        // Note: This doesn't clear Room database, but we might want to for a full clear
    }
}
