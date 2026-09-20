package com.algo1127.mytask.NotifAi

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters

class EvaluationWorker(appContext: Context, params: WorkerParameters) : Worker(appContext, params) {
    override fun doWork(): Result {
        return try {
            val notifAi = (applicationContext as com.algo1127.mytask.MyTaskApplication).notifAi
            val persistence = com.algo1127.mytask.NotifAi.persistence.Persistence(applicationContext)
            
            android.util.Log.d("NotifAi", "Periodic evaluation triggered")

            kotlinx.coroutines.runBlocking {
                val tasks = persistence.getTasks()
                tasks.filter { it.progress < 1.0f && it.focusState == com.algo1127.mytask.ui.models.FocusState.Active }
                    .forEach { task ->
                        notifAi.evaluateTask(task)
                    }
            }

            android.util.Log.d("NotifAi", "Periodic evaluation completed")
            Result.success()
        } catch (e: Exception) {
            android.util.Log.e("NotifAi", "Error in EvaluationWorker: ${e.message}", e)
            Result.retry()
        }
    }
}