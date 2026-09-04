package com.algo1127.mytask.data

import androidx.room.TypeConverter
import com.algo1127.mytask.ui.TaskCategory
import com.algo1127.mytask.ui.models.*
import com.google.gson.*
import com.google.gson.reflect.TypeToken
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class Converters {
    private val gson: Gson = GsonBuilder()
        .registerTypeAdapter(LocalTime::class.java, object : JsonSerializer<LocalTime>, JsonDeserializer<LocalTime> {
            private val fmt = DateTimeFormatter.ISO_LOCAL_TIME
            override fun serialize(src: LocalTime, typeOfSrc: java.lang.reflect.Type, context: JsonSerializationContext) =
                JsonPrimitive(src.format(fmt))
            override fun deserialize(json: JsonElement, typeOfT: java.lang.reflect.Type, context: JsonDeserializationContext) =
                LocalTime.parse(json.asString, fmt)
        })
        .registerTypeAdapter(LocalDate::class.java, object : JsonSerializer<LocalDate>, JsonDeserializer<LocalDate> {
            private val fmt = DateTimeFormatter.ISO_LOCAL_DATE
            override fun serialize(src: LocalDate, typeOfSrc: java.lang.reflect.Type, context: JsonSerializationContext) =
                JsonPrimitive(src.format(fmt))
            override fun deserialize(json: JsonElement, typeOfT: java.lang.reflect.Type, context: JsonDeserializationContext) =
                LocalDate.parse(json.asString, fmt)
        })
        .registerTypeAdapter(LocalDateTime::class.java, object : JsonSerializer<LocalDateTime>, JsonDeserializer<LocalDateTime> {
            private val fmt = DateTimeFormatter.ISO_LOCAL_DATE_TIME
            override fun serialize(src: LocalDateTime, typeOfSrc: java.lang.reflect.Type, context: JsonSerializationContext) =
                JsonPrimitive(src.format(fmt))
            override fun deserialize(json: JsonElement, typeOfT: java.lang.reflect.Type, context: JsonDeserializationContext) =
                LocalDateTime.parse(json.asString, fmt)
        })
        .registerTypeAdapter(Duration::class.java, object : JsonSerializer<Duration>, JsonDeserializer<Duration> {
            override fun serialize(src: Duration, typeOfSrc: java.lang.reflect.Type, context: JsonSerializationContext) =
                JsonPrimitive(src.toMillis())
            override fun deserialize(json: JsonElement, typeOfT: java.lang.reflect.Type, context: JsonDeserializationContext) =
                Duration.ofMillis(json.asLong)
        })
        .registerTypeAdapter(TimePreference::class.java, object : JsonSerializer<TimePreference>, JsonDeserializer<TimePreference> {
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

    @TypeConverter
    fun fromLocalDate(value: LocalDate?): String? = value?.format(DateTimeFormatter.ISO_LOCAL_DATE)

    @TypeConverter
    fun toLocalDate(value: String?): LocalDate? = value?.let { LocalDate.parse(it, DateTimeFormatter.ISO_LOCAL_DATE) }

    @TypeConverter
    fun fromLocalTime(value: LocalTime?): String? = value?.format(DateTimeFormatter.ISO_LOCAL_TIME)

    @TypeConverter
    fun toLocalTime(value: String?): LocalTime? = value?.let { LocalTime.parse(it, DateTimeFormatter.ISO_LOCAL_TIME) }

    @TypeConverter
    fun fromLocalDateTime(value: LocalDateTime?): String? = value?.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)

    @TypeConverter
    fun toLocalDateTime(value: String?): LocalDateTime? = value?.let { LocalDateTime.parse(it, DateTimeFormatter.ISO_LOCAL_DATE_TIME) }

    @TypeConverter
    fun fromDuration(value: Duration?): Long? = value?.toMillis()

    @TypeConverter
    fun toDuration(value: Long?): Duration? = value?.let { Duration.ofMillis(it) }

    @TypeConverter
    fun fromPriority(value: Priority): String = value.name

    @TypeConverter
    fun toPriority(value: String): Priority = Priority.valueOf(value)

    @TypeConverter
    fun fromFocusState(value: FocusState): String = value.name

    @TypeConverter
    fun toFocusState(value: String): FocusState = FocusState.valueOf(value)

    @TypeConverter
    fun fromVerificationStatus(value: VerificationStatus): String = value.name

    @TypeConverter
    fun toVerificationStatus(value: String): VerificationStatus = VerificationStatus.valueOf(value)

    @TypeConverter
    fun fromTaskCategory(value: TaskCategory): String = gson.toJson(value)

    @TypeConverter
    fun toTaskCategory(value: String): TaskCategory = try {
        gson.fromJson(value, TaskCategory::class.java)
    } catch (e: Exception) {
        // Fallback for legacy enum names
        TaskCategory.valueOf(value)
    }

    @TypeConverter
    fun fromSubtaskList(value: List<Subtask>): String = gson.toJson(value)

    @TypeConverter
    fun toSubtaskList(value: String): List<Subtask> = gson.fromJson(value, object : TypeToken<List<Subtask>>() {}.type)

    @TypeConverter
    fun fromTimePreference(value: TimePreference): String = gson.toJson(value)

    @TypeConverter
    fun toTimePreference(value: String): TimePreference = gson.fromJson(value, TimePreference::class.java)
}
