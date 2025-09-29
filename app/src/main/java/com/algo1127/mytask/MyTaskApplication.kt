
package com.algo1127.mytask

import android.app.Application
import com.algo1127.mytask.NotifAi.NotifAi

class MyTaskApplication : Application() {
    lateinit var notifAi: NotifAi

    override fun onCreate() {
        super.onCreate()
        try {
            notifAi = NotifAi(this)
            android.util.Log.d("MyTaskApplication", "NotifAi initialized")
        } catch (e: Exception) {
            android.util.Log.e("MyTaskApplication", "Failed to initialize NotifAi: ${e.message}", e)
        }
    }
}