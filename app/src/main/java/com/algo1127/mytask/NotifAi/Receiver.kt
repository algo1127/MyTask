package com.algo1127.mytask.NotifAi

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.algo1127.mytask.MyTaskApplication
import com.algo1127.mytask.NotifAi.model.NotificationAction

class Receiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getLongExtra("taskId", -1)
        val actionString = intent.getStringExtra("action")

        // ✅ Debug logging
        android.util.Log.d("Receiver", "=================================")
        android.util.Log.d("Receiver", "Notification button tapped!")
        android.util.Log.d("Receiver", "Task ID: $taskId")
        android.util.Log.d("Receiver", "Action: $actionString")
        android.util.Log.d("Receiver", "Intent action: ${intent.action}")
        android.util.Log.d("Receiver", "=================================")

        if (actionString.isNullOrBlank()) {
            android.util.Log.e("Receiver", "Action string is null or blank!")
            return
        }

        if (taskId == -1L) {
            android.util.Log.e("Receiver", "Task ID is -1! Something went wrong.")
            return
        }

        try {
            val action = NotificationAction.valueOf(actionString.uppercase())
            val notifAi = (context.applicationContext as MyTaskApplication).notifAi
            notifAi.onTap(action, taskId)
            android.util.Log.d("Receiver", "✅ onTap called successfully for $action")
        } catch (e: Exception) {
            android.util.Log.e("Receiver", "❌ Error processing action: ${e.message}", e)
        }
    }
}