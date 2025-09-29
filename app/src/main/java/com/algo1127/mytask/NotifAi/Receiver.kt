package com.algo1127.mytask.NotifAi

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.algo1127.mytask.MyTaskApplication
import com.algo1127.mytask.NotifAi.model.NotificationAction

class Receiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getLongExtra("taskId", -1)
        val actionString = intent.getStringExtra("action") ?: return

        val action = NotificationAction.valueOf(actionString.uppercase())
        val notifAi = (context.applicationContext as MyTaskApplication).notifAi
        notifAi.onTap(action, taskId)
    }
}