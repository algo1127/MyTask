package com.algo1127.mytask.NotifAi


import com.algo1127.mytask.ui.TaskItem

class PhraseList {
    val nudgePhrases = listOf(
        "Time to tackle {title}!",
        "Hey, {title} is up—let's do this! 😊",
        "Quick nudge for {title}."
    )

    val shamePhrases = listOf(
        "Still putting off {title}? Come on!",
        "{title} is waiting—don't slack! 😒",
        "You know {title} needs attention..."
    )

    val roastPhrases = listOf(
        "Seriously? {title} is collecting dust—get on it! 🔥",
        "{title} again? You're better than this!",
        "Don't let {title} win the procrastination battle!"
    )

    val brutalPhrases = listOf(
        "Final call on {title}—or regret it tomorrow!",
        "{title} is judging you hard—finish it now!",
        "Enough is enough with {title}—do it or delete it!"
    )

    val praisePhrases = listOf(
        "Nice! {title} crushed—keep it up! 🏆",
        "Great job on {title}—streak going strong!",
        "You nailed {title}—high five! ✋"
    )

    val probePhrases = listOf(
        "Off usual for {title}? Everything okay?",
        "Unusual delay on {title}—slacking or busy?",
        "Not your normal timing for {title}—adjust?"
    )

    fun pickPhrase(item: TaskItem, scale: Int, mood: Mood): String {
        val basePhrases = when {
            scale <= 3 -> nudgePhrases
            scale <= 6 -> shamePhrases
            scale <= 8 -> roastPhrases
            else -> brutalPhrases
        }
        var phrase = basePhrases.random().replace("{title}", item.title)

        // Mood modifier
        phrase = when (mood) {
            Mood.HAPPY -> phrase + " 😊"
            Mood.CURIOUS -> "Curious: " + phrase + "?"
            Mood.MAD -> phrase.uppercase() + " 😡"
        }

        return phrase
    }
}

enum class Mood {
    HAPPY, CURIOUS, MAD
}