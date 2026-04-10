package com.algo1127.mytask.NotifAi

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters

class EvaluationWorker(appContext: Context, params: WorkerParameters) : Worker(appContext, params) {
    override fun doWork(): Result {
        return try {
            val notifAi = (applicationContext as com.algo1127.mytask.MyTaskApplication).notifAi
            android.util.Log.d("NotifAi", "Periodic evaluation triggered")

            // v1: Stub - evaluate all pending tasks
            // v2: Full evaluation with UsageAnalyzer

            android.util.Log.d("NotifAi", "Periodic evaluation completed")
            Result.success()
        } catch (e: Exception) {
            android.util.Log.e("NotifAi", "Error in EvaluationWorker: ${e.message}", e)
            Result.retry()
        }
    }
}