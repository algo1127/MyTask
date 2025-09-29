package com.algo1127.mytask.NotifAi.persistence

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.algo1127.mytask.NotifAi.model.AiProfile

class Persistence(
    private val context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences("ai_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun saveProfile(profile: AiProfile) {
        prefs.edit().putString("ai_profile", gson.toJson(profile)).apply()
    }

    fun loadProfile(): AiProfile {
        val json = prefs.getString("ai_profile", null)
        return if (json != null) {
            gson.fromJson(json, AiProfile::class.java)
        } else {
            AiProfile()
        }
    }
}