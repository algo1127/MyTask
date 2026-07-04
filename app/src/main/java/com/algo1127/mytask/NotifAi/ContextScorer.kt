package com.algo1127.mytask.NotifAi

object ContextScorer {

    /**
     * Returns a delta (-0.40 … +0.20) to add to PatternAnalyzer.slotScore().
     * Positive = this is a great moment to notify; negative = back off.
     */
    fun contextDelta(ctx: DeviceContext?): Double {
        if (ctx == null) return 0.0

        // Hard blocks — return large penalty immediately
        if (ctx.isIdle)        return -0.35   // phone has been sitting dark
        if (ctx.inFocusApp)    return -0.40   // user is in a call/meeting

        var delta = 0.0

        // Phone is actively in use — good window
        if (ctx.unlockCount >= 3)    delta += 0.10
        if (ctx.screenOnMinutes >= 20) delta += 0.08

        // User just came off social/messaging — phone's in hand
        val socialApps = setOf(
            "com.instagram.android",
            "com.twitter.android",
            "com.whatsapp",
            "org.telegram.messenger",
            "com.google.android.apps.messaging"
        )
        if (ctx.activeAppPackage in socialApps) delta += 0.15

        // Productivity context — possibly receptive to task reminders
        val productivityApps = setOf(
            "com.google.android.gm",          // Gmail
            "com.microsoft.office.outlook",
            "com.todoist",
            "com.google.android.calendar"
        )
        if (ctx.activeAppPackage in productivityApps) delta += 0.12

        // Entertainment/gaming — bad time for work/study tasks
        val entertainmentApps = setOf(
            "com.netflix.mediaclient",
            "com.spotify.music",
            "com.google.android.youtube",
            "com.valvesoftware.android.steam.community"
        )
        if (ctx.activeAppPackage in entertainmentApps) delta -= 0.15

        return delta.coerceIn(-0.40, 0.20)
    }

    /**
     * True if we should skip this notification entirely based on device state.
     * Called BEFORE slotScore check — a hard gate.
     */
    fun shouldHardBlock(ctx: DeviceContext?): Boolean {
        if (ctx == null) return false
        return ctx.isIdle || ctx.inFocusApp
    }
}