package com.algo1127.mytask

import com.algo1127.mytask.NotifAi.PatternAnalyzer
import com.algo1127.mytask.NotifAi.UsageEvent
import com.algo1127.mytask.NotifAi.UsageRecord
import com.algo1127.mytask.ui.TaskCategory
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDateTime

class PatternAnalyzerTest {

    @Test
    fun testFocusEntropyScore_HighEntropy() {
        val now = System.currentTimeMillis()
        val records = listOf(
            UsageRecord(now - 1000, UsageEvent.NOTIFICATION_SENT, TaskCategory.Work, 1, 10, 0, 1, entropy = 5.0f, sessionDepth = 5),
            UsageRecord(now - 2000, UsageEvent.NOTIFICATION_SENT, TaskCategory.Work, 2, 10, 0, 1, entropy = 4.5f, sessionDepth = 8)
        )
        
        val score = PatternAnalyzer.focusEntropyScore(records)
        assertTrue("Score should be low for high entropy: $score", score < 0.4)
    }

    @Test
    fun testFocusEntropyScore_LowEntropyDeepFocus() {
        val now = System.currentTimeMillis()
        val records = listOf(
            UsageRecord(now - 1000, UsageEvent.NOTIFICATION_SENT, TaskCategory.Work, 1, 10, 0, 1, entropy = 0.1f, sessionDepth = 300),
            UsageRecord(now - 2000, UsageEvent.NOTIFICATION_SENT, TaskCategory.Work, 2, 10, 0, 1, entropy = 0.2f, sessionDepth = 400)
        )
        
        val score = PatternAnalyzer.focusEntropyScore(records)
        assertTrue("Score should be high for deep focus: $score", score > 0.7)
    }

    @Test
    fun testSlotScore_BayesianConvergence() {
        val records = mutableListOf<UsageRecord>()
        val now = System.currentTimeMillis()
        
        // Initial score should be near prior (~0.5)
        val initial = PatternAnalyzer.slotScore(records, TaskCategory.Work, 10, 1)
        assertEquals(0.5, initial, 0.1)
        
        // Add many completions
        repeat(20) {
            records.add(UsageRecord(now, UsageEvent.COMPLETED, TaskCategory.Work, it.toLong(), 10, 0, 1, responseTimeMs = 1000))
        }
        
        val successScore = PatternAnalyzer.slotScore(records, TaskCategory.Work, 10, 1)
        assertTrue("Score should increase with successes: $successScore", successScore > 0.7)
        
        // Add many ignores
        repeat(40) {
            records.add(UsageRecord(now, UsageEvent.IGNORED, TaskCategory.Work, (it + 20).toLong(), 10, 0, 1))
        }
        
        val failureScore = PatternAnalyzer.slotScore(records, TaskCategory.Work, 10, 1)
        assertTrue("Score should decrease with failures: $failureScore", failureScore < 0.3)
    }

    @Test
    fun testDeadSpaceDetection() {
        val records = mutableListOf<UsageRecord>()
        val now = System.currentTimeMillis()
        
        // Simulate consistent inactivity at 3 AM
        repeat(5) {
            records.add(UsageRecord(
                timestampMs = now - it * 86_400_000L,
                event = UsageEvent.NOTIFICATION_SENT,
                category = TaskCategory.Personal,
                taskId = it.toLong(),
                hour = 3,
                minute = 0,
                dayOfWeek = 1,
                screenOnMinutes = 0,
                unlockCount = 0
            ))
        }
        
        val deadScore = PatternAnalyzer.deadSpaceScore(records, 3, 1)
        assertTrue("Dead space score should be high for 3 AM: $deadScore", deadScore > 0.8)
        
        val slotScore = PatternAnalyzer.slotScore(records, TaskCategory.Personal, 3, 1)
        assertTrue("Slot score should be very low for dead space: $slotScore", slotScore < 0.2)
    }
}
