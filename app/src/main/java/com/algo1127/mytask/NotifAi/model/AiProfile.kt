package com.algo1127.mytask.NotifAi.model

import java.time.LocalDateTime
import java.time.LocalTime

data class AiProfile(
    val taskPatterns: Map<Long, TaskPattern> = emptyMap(),
    val toggles: Map<String, Boolean> = mapOf(
        "noRoast" to false,
        "soulless" to false,
        "moodcast" to false
    )
) {
    // ✅ ADD THIS COMPANION OBJECT
    companion object {
        fun default() = AiProfile() // Uses primary constructor defaults
    }
}

data class TaskPattern(
    val taskId: Long,
    val fuzzyWindowStart: LocalTime,
    val fuzzyWindowEnd: LocalTime,
    val completionTimes: List<LocalDateTime>,
    val forgetCount: Int,
    val ignoreCount: Int,
    val skipCount: Int
)

enum class NotificationAction {
    COMPLETED, FORGOT, IGNORED, SKIPPED
}