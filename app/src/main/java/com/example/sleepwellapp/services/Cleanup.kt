package com.example.sleepwellapp.services

import android.content.Context
import androidx.room.Room
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.example.sleepwellapp.datalayer.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.concurrent.TimeUnit

object ScheduleCleanup {
    fun scheduleCleanup(context: Context) {
        val workManager = WorkManager.getInstance(context)
        val cleanupRequest = PeriodicWorkRequestBuilder<CleanupWorker>(7, TimeUnit.DAYS).build()
        workManager.enqueueUniquePeriodicWork("cleanup_worker", ExistingPeriodicWorkPolicy.REPLACE, cleanupRequest)
    }
}

class CleanupWorker(appContext: Context, workerParams: WorkerParameters) :
    Worker(appContext, workerParams) {
    override fun doWork(): Result {
        val db = Room.databaseBuilder(applicationContext, AppDatabase::class.java, "app-db").build()
        val motionCountDao = db.motionCountDao()

        CoroutineScope(Dispatchers.IO).launch {
            val calendar = Calendar.getInstance()
            calendar.add(Calendar.DAY_OF_YEAR, -7)
            val cutoffDate = calendar.time
            motionCountDao.deleteOldData(cutoffDate.time)
        }

        return Result.success()
    }
}