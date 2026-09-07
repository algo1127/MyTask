package com.algo1127.mytask.NotifAi

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.algo1127.mytask.MyTaskApplication
import com.algo1127.mytask.NotifAi.model.NotificationAction
import kotlinx.coroutines.launch

class Receiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getLongExtra("taskId", -1)
        
        if (intent.action == "com.algo1127.mytask.ACTION_TRIGGER_COUNTDOWN") {
            val countdownId = intent.getLongExtra("countdownId", -1)
            val title = intent.getStringExtra("countdownTitle") ?: "Timer"
            android.util.Log.d("Receiver", "Countdown triggered for $title ($countdownId)")
            
            val notifAi = (context.applicationContext as MyTaskApplication).notifAi
            notifAi.showTimerDoneNotification(countdownId, title)
            return
        }

        if (intent.action == "com.algo1127.mytask.ACTION_TRIGGER_REMINDER") {
            android.util.Log.d("Receiver", "Reminder triggered for task $taskId")
            val notifAi = (context.applicationContext as MyTaskApplication).notifAi
            // To send a notification we need the full Task object.
            // For now, we'll try to retrieve it from persistence.
            val persistence = com.algo1127.mytask.NotifAi.persistence.Persistence(context)
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                val tasks = persistence.getTasks()
                val task = tasks.find { it.id == taskId }
                if (task != null) {
                    notifAi.evaluateTask(task)
                }
            }
            return
        }

        val actionString = intent.getStringExtra("action")

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
            notifAi.onTaskAction(taskId, action)  // ✅ FIXED: Correct method name
            android.util.Log.d("Receiver", "✅ onTaskAction called successfully for $action")
        } catch (e: Exception) {
            android.util.Log.e("Receiver", "❌ Error processing action: ${e.message}", e)
        }
    }
}