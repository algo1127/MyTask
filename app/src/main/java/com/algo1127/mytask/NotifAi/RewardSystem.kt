package com.algo1127.mytask.NotifAi

import android.content.Context
import android.content.SharedPreferences
import java.time.LocalDate

data class RewardState(
    val streak: Int          = 0,
    val longestStreak: Int   = 0,
    val totalCompleted: Int  = 0,
    val weeklyCompleted: Int = 0,
    val momentum: Int        = 0,  // consecutive actions without ignoring
    val xp: Int              = 0,
    val level: Int           = 1
)

enum class MotivationLevel { GENTLE, NORMAL, FIRM, INTENSE }

class RewardSystem(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("reward_system", Context.MODE_PRIVATE)

    companion object {
        private const val XP_TASK       = 10
        private const val XP_STREAK_MUL = 3   // extra XP per streak day
        private const val XP_PER_LEVEL  = 100
    }

    fun getState() = RewardState(
        streak          = prefs.getInt("streak", 0),
        longestStreak   = prefs.getInt("longest_streak", 0),
        totalCompleted  = prefs.getInt("total_completed", 0),
        weeklyCompleted = prefs.getInt("weekly_completed", 0),
        momentum        = prefs.getInt("momentum", 0),
        xp              = prefs.getInt("xp", 0),
        level           = prefs.getInt("level", 1)
    )

    fun onCompleted() {
        val s = getState()
        val today = LocalDate.now().toString()
        val lastDay = prefs.getString("last_completion_date", "")

        val newStreak = when (lastDay) {
            today -> s.streak  // already completed today, no double-count
            LocalDate.now().minusDays(1).toString() -> s.streak + 1  // continuing streak
            else -> 1  // streak broken, restart
        }

        val bonusXp = newStreak * XP_STREAK_MUL
        val newXp   = s.xp + XP_TASK + bonusXp
        val newLevel = (newXp / XP_PER_LEVEL) + 1

        prefs.edit()
            .putInt("streak",           newStreak)
            .putInt("longest_streak",   maxOf(s.longestStreak, newStreak))
            .putInt("total_completed",  s.totalCompleted + 1)
            .putInt("weekly_completed", s.weeklyCompleted + 1)
            .putInt("momentum",         s.momentum + 1)
            .putInt("xp",               newXp)
            .putInt("level",            newLevel)
            .putString("last_completion_date", today)
            .apply()
    }

    fun onIgnored() {
        prefs.edit().putInt("momentum", maxOf(0, getState().momentum - 1)).apply()
    }

    fun onForgotten() {
        prefs.edit()
            .putInt("momentum", 0)   // full momentum reset on forget
            .apply()
    }

    /**
     * How assertive should the AI be?
     * Low streak + low trust + zero momentum = INTENSE
     */
    fun motivationLevel(trustScore: Float): MotivationLevel {
        val s = getState()
        return when {
            trustScore < 0.25f || (s.momentum == 0 && s.streak == 0) -> MotivationLevel.INTENSE
            s.streak >= 7 && trustScore > 0.6f                       -> MotivationLevel.GENTLE
            s.streak >= 3 || trustScore > 0.5f                       -> MotivationLevel.NORMAL
            s.momentum < 2 || trustScore < 0.4f                      -> MotivationLevel.FIRM
            else                                                      -> MotivationLevel.NORMAL
        }
    }

    fun streakMessage(): String {
        val s = getState()
        return when {
            s.streak >= 30 -> "🔥 ${s.streak} days. Genuinely impressive."
            s.streak >= 14 -> "⚡ ${s.streak}-day streak. Don't blow it now."
            s.streak >= 7  -> "🎯 One week strong!"
            s.streak >= 3  -> "✨ ${s.streak} days in a row — keep going"
            s.streak == 1  -> "Day 1. The hardest one. Good."
            else           -> "Start your streak today."
        }
    }

    fun levelUpMessage(oldLevel: Int, newLevel: Int): String? {
        if (newLevel <= oldLevel) return null
        return "⬆️ Level $newLevel! ${getState().xp} XP total"
    }
}