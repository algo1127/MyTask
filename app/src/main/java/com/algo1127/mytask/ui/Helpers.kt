package com.algo1127.mytask.ui

import java.time.LocalTime

fun getGreeting(): String {
    val hour = LocalTime.now().hour
    return when {
        hour in 5..11  -> "Good morning ☀️"
        hour in 12..16 -> "Good afternoon"
        hour in 17..20 -> "Good evening 🌆"
        else           -> "Good night 🌙"
    }
}