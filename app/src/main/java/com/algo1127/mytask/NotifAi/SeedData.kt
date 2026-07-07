package com.algo1127.mytask.NotifAi

import com.algo1127.mytask.ui.TaskCategory
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.ZoneId

object SeedData {
    /**
     * Generates a set of synthetic records representing a productive user.
     * Timestamps are set to ~10 days ago so they provide a baseline but
     * allow new data to carry more weight.
     */
    fun getPresetRecords(): List<UsageRecord> {
        val records = mutableListOf<UsageRecord>()
        val baseTime = LocalDateTime.now().minusDays(10)
        
        // --- WORK PATTERNS (9 AM, 2 PM focus) ---
        listOf(9, 14).forEach { hour ->
            // Mon-Fri
            (1..5).forEach { dow ->
                records.add(createSynthetic(UsageEvent.COMPLETED, TaskCategory.Work, hour, dow, baseTime))
                records.add(createSynthetic(UsageEvent.NOTIFICATION_SENT, TaskCategory.Work, hour, dow, baseTime))
            }
        }

        // --- STUDY PATTERNS (10 AM, 4 PM, 7 PM focus) ---
        listOf(10, 16, 19).forEach { hour ->
            // Mon-Sun
            (1..7).forEach { dow ->
                records.add(createSynthetic(UsageEvent.COMPLETED, TaskCategory.Study, hour, dow, baseTime))
                records.add(createSynthetic(UsageEvent.NOTIFICATION_SENT, TaskCategory.Study, hour, dow, baseTime))
            }
        }

        // --- PERSONAL PATTERNS (8 AM, 12 PM, 6 PM focus) ---
        listOf(8, 12, 18).forEach { hour ->
            // Mon-Sun
            (1..7).forEach { dow ->
                records.add(createSynthetic(UsageEvent.COMPLETED, TaskCategory.Personal, hour, dow, baseTime))
                records.add(createSynthetic(UsageEvent.NOTIFICATION_SENT, TaskCategory.Personal, hour, dow, baseTime))
            }
        }

        // --- DESIGN PATTER PATTERNS (11 AM, 3 PM focus) ---
        listOf(11, 15).forEach { hour ->
            // Mon-Fri
            (1..5).forEach { dow ->
                records.add(createSynthetic(UsageEvent.COMPLETED, TaskCategory.Design, hour, dow, baseTime))
                records.add(createSynthetic(UsageEvent.NOTIFICATION_SENT, TaskCategory.Design, hour, dow, baseTime))
            }
        }

        return records
    }

    private fun createSynthetic(
        event: UsageEvent,
        cat: TaskCategory,
        hour: Int,
        dow: Int,
        baseTime: LocalDateTime
    ): UsageRecord {
        // Adjust baseTime to match the DOW and Hour
        val target = baseTime.withHour(hour).withMinute(0)
        val currentDow = target.dayOfWeek.value
        val offsetDays = (dow - currentDow).toLong()
        val finalTime = target.plusDays(offsetDays)
        
        return UsageRecord(
            timestampMs = finalTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
            event = event,
            category = cat,
            taskId = -99L, // marker for synthetic
            hour = hour,
            dayOfWeek = dow,
            responseTimeMs = 120_000L // 2 minutes (good response)
        )
    }
}
