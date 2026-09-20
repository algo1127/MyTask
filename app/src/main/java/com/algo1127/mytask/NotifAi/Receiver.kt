package com.algo1127.mytask.NotifAi

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.algo1127.mytask.MyTaskApplication
import com.algo1127.mytask.NotifAi.model.NotificationAction
import com.algo1127.mytask.data.MyTaskDatabase
import com.algo1127.mytask.ui.models.Task
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
            val minsBefore = intent.getIntExtra("minutesBefore", 0)
            android.util.Log.d("Receiver", "Reminder triggered for task $taskId (offset: $minsBefore)")
            val notifAi = (context.applicationContext as MyTaskApplication).notifAi
            val database = MyTaskDatabase.getDatabase(context)
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                val task = database.taskDao().getTaskById(taskId)
                if (task != null) {
                    notifAi.evaluateTask(task = task, isFromWorker = false, minutesBefore = minsBefore)
                } else {
                    android.util.Log.e("Receiver", "Task $taskId not found in database")
                }
            }
            return
        }

        if (intent.action == "com.algo1127.mytask.ACTION_TRIGGER_EVENT_REMINDER") {
            val eventId = intent.getLongExtra("eventId", -1)
            val minsBefore = intent.getIntExtra("minutesBefore", 0)
            android.util.Log.d("Receiver", "Event triggered for $eventId (offset: $minsBefore)")
            val notifAi = (context.applicationContext as MyTaskApplication).notifAi
            val database = MyTaskDatabase.getDatabase(context)
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                val event = database.eventDao().getAllEvents().find { it.id == eventId }
                if (event != null) {
                    notifAi.sendEventNotification(event, minsBefore)
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