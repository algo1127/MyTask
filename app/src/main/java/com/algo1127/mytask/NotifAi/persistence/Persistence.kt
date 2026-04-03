package com.algo1127.mytask.NotifAi.persistence

import android.content.Context
import android.content.SharedPreferences
import com.algo1127.mytask.NotifAi.model.AiProfile
import com.google.gson.*
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class Persistence(private val context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("ai_prefs", Context.MODE_PRIVATE)

    // ✅ CRITICAL: Gson with Java 8 time adapters + null safety
    private val gson: Gson = GsonBuilder()
        .registerTypeAdapter(LocalTime::class.java, LocalTimeAdapter())
        .registerTypeAdapter(LocalDateTime::class.java, LocalDateTimeAdapter())
        .setPrettyPrinting()
        .serializeNulls() // Prevents field omission that breaks deserialization
        .create()

    fun saveProfile(profile: AiProfile) {
        try {
            val json = gson.toJson(profile)
            prefs.edit().putString("ai_profile", json).apply()
            android.util.Log.d("Persistence", "Profile saved successfully")
        } catch (e: Exception) {
            android.util.Log.e("Persistence", "Failed to save profile", e)
            // Optional: Fallback to clearing corrupted data
            prefs.edit().remove("ai_profile").apply()
        }
    }

    fun loadProfile(): AiProfile {
        return try {
            val json = prefs.getString("ai_profile", null)
            if (json.isNullOrEmpty()) return AiProfile.default()

            // ✅ Validate JSON structure before parsing
            if (!json.contains("taskPatterns") || !json.contains("toggles")) {
                android.util.Log.w("Persistence", "Corrupted profile structure - using defaults")
                prefs.edit().remove("ai_profile").apply()
                return AiProfile.default()
            }

            val profile = gson.fromJson(json, AiProfile::class.java)

            // ✅ Validate critical fields to prevent runtime crashes
            validateProfile(profile)
            profile
        } catch (e: JsonSyntaxException) {
            android.util.Log.e("Persistence", "JSON syntax error - deleting corrupted profile", e)
            prefs.edit().remove("ai_profile").apply()
            AiProfile.default()
        } catch (e: Exception) {
            android.util.Log.w("Persistence", "Profile load failed - using defaults", e)
            prefs.edit().remove("ai_profile").apply()
            AiProfile.default()
        }
    }

    private fun validateProfile(profile: AiProfile) {
        profile.taskPatterns.forEach { (_, pattern) ->
            require(pattern.fuzzyWindowStart != null) { "fuzzyWindowStart is null" }
            require(pattern.fuzzyWindowEnd != null) { "fuzzyWindowEnd is null" }
            require(pattern.completionTimes != null) { "completionTimes is null" }
        }
    }

    // ✅ LocalTime adapter (ISO-8601 format)
    class LocalTimeAdapter : JsonSerializer<LocalTime>, JsonDeserializer<LocalTime> {
        private val formatter = DateTimeFormatter.ISO_LOCAL_TIME

        override fun serialize(src: LocalTime, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
            return JsonPrimitive(src.format(formatter))
        }

        override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): LocalTime {
            return try {
                LocalTime.parse(json.asString, formatter)
            } catch (e: Exception) {
                android.util.Log.w("LocalTimeAdapter", "Failed to parse time '${json.asString}', using default", e)
                LocalTime.NOON // Safe fallback
            }
        }
    }

    // ✅ LocalDateTime adapter (ISO-8601 format)
    class LocalDateTimeAdapter : JsonSerializer<LocalDateTime>, JsonDeserializer<LocalDateTime> {
        private val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

        override fun serialize(src: LocalDateTime, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
            return JsonPrimitive(src.format(formatter))
        }

        override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): LocalDateTime {
            return try {
                LocalDateTime.parse(json.asString, formatter)
            } catch (e: Exception) {
                android.util.Log.w("LocalDateTimeAdapter", "Failed to parse datetime '${json.asString}', using now", e)
                LocalDateTime.now()
            }
        }
    }
}

// ✅ Safe default profile (prevents null crashes)
fun AiProfile.Companion.default(): AiProfile {
    return AiProfile(
        taskPatterns = emptyMap(),
        toggles = mutableMapOf(
            "noRoast" to false,
            "soulless" to false,
            "moodcast" to true
        )
    )
}