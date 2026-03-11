package com.algo1127.mytask.NotifAi

import com.algo1127.mytask.ui.TaskItem
import com.algo1127.mytask.ui.TaskCategory
import java.time.LocalTime

class PhraseList {

    // ==================== NUDGE PHRASES (Scale 1-3) ====================
    val nudgePhrases = listOf(
        "Time to tackle {title}!",
        "Hey, {title} is up—let's do this! 😊",
        "Quick nudge for {title}.",
        "Psst... {title} is waiting! 👀",
        "Friendly reminder: {title} ⏰",
        "Whenever you're ready: {title}",
        "Just a little ping for {title}!",
        "{title} time! You got this 💪",
        "Gentle nudge: {title} calls!",
        "Hey hey, {title} is on the schedule!",
        "Don't forget about {title}!",
        "{title} is ready when you are!",
        "Quick check-in: {title} 📝",
        "Your future self thanks you for {title}!",
        "Small step time: {title}"
    )

    // ==================== SHAME PHRASES (Scale 4-6) ====================
    val shamePhrases = listOf(
        "Still putting off {title}? Come on!",
        "{title} is waiting—don't slack! 😒",
        "You know {title} needs attention...",
        "Hmm, {title} is getting impatient 👀",
        "{title} hasn't been forgotten... just ignored 😅",
        "Really? {title} is still pending?",
        "{title} is starting to feel neglected...",
        "Come on, {title} isn't gonna do itself!",
        "{title} is collecting cobwebs over here 🕸️",
        "Is {title} invisible? It's right there!",
        "{title} called, it wants attention 📞",
        "Your to-do list is judging {title}...",
        "{title} is still here. Just saying. 👀",
        "Procrastinating {title}? Bold choice.",
        "{title} is waiting... and waiting... and waiting..."
    )

    // ==================== ROAST PHRASES (Scale 7-8) ====================
    val roastPhrases = listOf(
        "Seriously? {title} is collecting dust—get on it! 🔥",
        "{title} again? You're better than this!",
        "Don't let {title} win the procrastination battle!",
        "WOAH. {title} is STILL not done? 😳",
        "{title} is officially offended at this point.",
        "At this point {title} has its own zip code 🏠",
        "{title} is aging like fine wine... but not in a good way 🍷",
        "Is {title} a permanent fixture now? 🖼️",
        "{title} has seen things. Mostly your excuses.",
        "Breaking: {title} still not done. More at 11. 📰",
        "{title} is starting to think you don't like it 💔",
        "Congratulations! {title} achieved 'Most Ignored' award 🏆",
        "{title} is filing a missing person report 🚨",
        "At this rate {title} will be in your obituary ⚰️",
        "{title} has better things to do. Like exist without you."
    )

    // ==================== BRUTAL PHRASES (Scale 9-10) ====================
    val brutalPhrases = listOf(
        "Final call on {title}—or regret it tomorrow!",
        "{title} is judging you hard—finish it now!",
        "Enough is enough with {title}—do it or delete it!",
        "CODE RED: {title} is in EMERGENCY MODE 🚨",
        "{title} has officially filed a complaint 📋",
        "LAST CHANCE for {title} before consequences hit!",
        "{title} is now a CRITICAL situation ⚠️",
        "ABANDON ALL EXCUSES. DO {title}. NOW. 🔥",
        "{title} has entered its final form... and it's ANGRY 😤",
        "This is your FINAL FINAL warning about {title}!",
        "{title} is literally screaming at this point 📢",
        "EMERGENCY ALERT: {title} requires IMMEDIATE action!",
        "{title} is one notification away from calling your mom 📱",
        "At this point {title} deserves therapy 💀",
        "DO {title} OR DON'T. BUT STOP IGNORING IT. 😤"
    )

    // ==================== PRAISE PHRASES (On Completion) ====================
    val praisePhrases = listOf(
        "Nice! {title} crushed—keep it up! 🏆",
        "Great job on {title}—streak going strong!",
        "You nailed {title}—high five! ✋",
        "BOOM! {title} is DONE! 🎉",
        "{title} conquered! What's next? 💪",
        "Look at you go! {title} complete! ⚡",
        "{title} didn't stand a chance! 😎",
        "Another W for {title}! Keep rolling! 🎲",
        "{title} complete! Future you is proud! 🌟",
        "YES! {title} is HISTORY! 📚",
        "Task slayer! {title} is GONE! ⚔️",
        "{title} complete! Celebrate! 🎊",
        "One step closer to greatness! {title} done! ",
        "{title} who? Oh right, you CRUSHED it! 💥",
        "Victory! {title} is officially conquered! 👑"
    )

    // ==================== PROBE PHRASES (Abnormal Timing) ====================
    val probePhrases = listOf(
        "Off usual for {title}? Everything okay?",
        "Unusual delay on {title}—slacking or busy?",
        "Not your normal timing for {title}—adjust?",
        "{title} is later than usual... rough day? 😕",
        "Hmm, {title} timing is off. All good?",
        "{title} is outside your normal window. Need help?",
        "Interesting... {title} is atypical for you 🤔",
        "{title} is later than your usual pattern. Okay?",
        "Noticing {title} is off-schedule. Everything alright?",
        "{title} timing is unusual. Want to reschedule?",
        "Your pattern says {title} should be done... 🤨",
        "{title} is breaking your usual streak. Why?",
        "Curious: {title} is later than normal. What's up?",
        "{title} doesn't match your usual rhythm 🎵",
        "Noticing a pattern break on {title}. Adjust?"
    )

    // ==================== CATEGORY-SPECIFIC PHRASES ====================
    val studyPhrases = listOf(
        "Books called, they want {title} done! 📚",
        "Brain gains await! {title} time! 🧠",
        "Knowledge doesn't acquire itself! {title}! 📖",
        "Future graduate self says do {title}! 🎓",
        "Study session: {title} is up! ✏️"
    )

    val workPhrases = listOf(
        "Professional you says: {title}! 💼",
        "Career goals need {title} done! 📈",
        "Boss mode activated: {title}! 👔",
        "Work hard, {title} harder! 💻",
        "Productivity peak: {title} time! ⚡"
    )

    val personalPhrases = listOf(
        "Self-care includes {title}! 💆",
        "Personal growth = {title} done! 🌱",
        "You deserve {title} completed! 💖",
        "Life admin: {title} is waiting! 📋",
        "Treat yourself: finish {title}! 🎁"
    )

    val designPhrases = listOf(
        "Creative vibes: {title} is calling! 🎨",
        "Design brain wants {title} done! ✨",
        "Artistic you says: {title} time! 🖌️",
        "Masterpiece pending: {title}! 🖼️",
        "Creative flow starts with {title}! 🌊"
    )

    // ==================== TIME-OF-DAY PHRASES ====================
    fun getTimeBasedPrefix(): String {
        val hour = LocalTime.now().hour
        return when {
            hour in 5..11 -> "☀️ Good morning! "
            hour in 12..16 -> "🌤️ Afternoon check-in: "
            hour in 17..20 -> "🌆 Evening reminder: "
            else -> "🌙 Late night nudge: "
        }
    }

    // ==================== MAIN PHRASE PICKER ====================
    fun pickPhrase(item: TaskItem, scale: Int, mood: Mood, forgetCount: Int = 0): String {
        // Get base phrases by scale
        val basePhrases = when {
            scale <= 3 -> nudgePhrases
            scale <= 6 -> shamePhrases
            scale <= 8 -> roastPhrases
            else -> brutalPhrases
        }

        // Add category-specific phrase occasionally (20% chance)
        val categoryPhrase = getCategoryPhrase(item.category)
        val useCategory = categoryPhrase != null && (1..100).random() <= 20

        // Pick the phrase
        var phrase = if (useCategory && scale <= 6) {
            categoryPhrase!!.replace("{title}", item.title)
        } else {
            basePhrases.random().replace("{title}", item.title)
        }

        // Add time prefix for low scales (friendly)
        if (scale <= 4 && (1..100).random() <= 30) {
            phrase = getTimeBasedPrefix() + phrase.lowercase()
        }

        // Add forget count modifier (more forgets = more intense)
        if (forgetCount >= 3) {
            phrase = "⚠️ (${forgetCount}x ignored) " + phrase
        }

        // Mood modifier
        phrase = when (mood) {
            Mood.HAPPY -> phrase + " 😊"
            Mood.CURIOUS -> "🤔 " + phrase + "?"
            Mood.MAD -> phrase.uppercase() + " 😡"
            Mood.DISAPPOINTED -> "😔 " + phrase
            Mood.EXCITED -> "🎉 " + phrase + "!!!"
        }

        return phrase
    }

    // ==================== CATEGORY PHRASE HELPER ====================
    private fun getCategoryPhrase(category: TaskCategory): String? {
        return when (category) {
            TaskCategory.Study -> studyPhrases.random()
            TaskCategory.Work -> workPhrases.random()
            TaskCategory.Personal -> personalPhrases.random()
            TaskCategory.Design -> designPhrases.random()
        }
    }

    // ==================== COMPLETION PHRASE ====================
    fun getCompletionPhrase(item: TaskItem): String {
        return praisePhrases.random().replace("{title}", item.title)
    }

    // ==================== ABNORMAL DAY PHRASE ====================
    fun getAbnormalDayPhrase(item: TaskItem, deviationMinutes: Long): String {
        val hoursLate = deviationMinutes / 60
        return when {
            hoursLate < 2 -> probePhrases.random().replace("{title}", item.title)
            hoursLate < 6 -> "🤨 {title} is ${hoursLate}h late. Pattern break detected.".replace("{title}", item.title)
            else -> "⚠️ {title} is ${hoursLate}h off. Everything okay?".replace("{title}", item.title)
        }
    }
}

enum class Mood {
    HAPPY,
    CURIOUS,
    MAD,
    DISAPPOINTED,
    EXCITED
}