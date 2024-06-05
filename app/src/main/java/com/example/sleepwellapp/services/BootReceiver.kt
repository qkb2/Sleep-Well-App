package com.example.sleepwellapp.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.room.Room
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.sleepwellapp.datalayer.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.TemporalAdjusters
import java.util.concurrent.TimeUnit

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // Schedule the WorkManager tasks
            ScheduleUtil.scheduleNightlyNotifications(context)
            ScheduleCleanup.scheduleCleanup(context)
        }
    }
}

object ScheduleUtil {
    private const val WORKER_TAG_PREFIX = "notification_worker_"

    fun scheduleNightlyNotifications(context: Context) {
        val workManager = WorkManager.getInstance(context)
        val daysOfWeek = DayOfWeek.entries.toTypedArray()

        val db = Room.databaseBuilder(context, AppDatabase::class.java, "app-db").build()
        val nightTimeDao = db.nightTimeDao()

        CoroutineScope(Dispatchers.IO).launch {
            daysOfWeek.forEach { startDay ->
                val nightTime = nightTimeDao.getByStartDay(startDay.name)
                if (nightTime != null) {
                    if (nightTime.enabled) {
                        // Schedule sleep notification
                        val sleepWorkRequest = OneTimeWorkRequestBuilder<SleepNotificationWorker>()
                            .setInitialDelay(calculateDelayUntilNext(startDay, LocalTime.parse(nightTime.sleepTime)), TimeUnit.MILLISECONDS)
                            .addTag("${WORKER_TAG_PREFIX}${startDay.name}_sleep")
                            .build()

                        // Schedule wake-up notification
                        val endDay = DayOfWeek.valueOf(nightTime.endDay)
                        val wakeWorkRequest = OneTimeWorkRequestBuilder<WakeNotificationWorker>()
                            .setInitialDelay(calculateDelayUntilNext(endDay, LocalTime.parse(nightTime.wakeUpTime)), TimeUnit.MILLISECONDS)
                            .addTag("${WORKER_TAG_PREFIX}${endDay.name}_wake")
                            .build()

                        workManager.enqueueUniqueWork("${WORKER_TAG_PREFIX}${startDay.name}_sleep", ExistingWorkPolicy.REPLACE, sleepWorkRequest)
                        workManager.enqueueUniqueWork("${WORKER_TAG_PREFIX}${endDay.name}_wake", ExistingWorkPolicy.REPLACE, wakeWorkRequest)
                    } else {
                        // Cancel existing workers for this day
                        workManager.cancelAllWorkByTag("${WORKER_TAG_PREFIX}${startDay.name}_sleep")
                        workManager.cancelAllWorkByTag("${WORKER_TAG_PREFIX}${nightTime.endDay}_wake")
                    }
                }
            }
        }
    }

    private fun calculateDelayUntilNext(dayOfWeek: DayOfWeek, time: LocalTime): Long {
        val now = LocalDateTime.now()
        var targetDayTime = now.with(TemporalAdjusters.nextOrSame(dayOfWeek)).with(time)
        if (targetDayTime.isBefore(now)) {
            targetDayTime = targetDayTime.plusWeeks(1)
        }
        return Duration.between(now, targetDayTime).toMillis()
    }

    // Add methods to start and stop the service immediately
    fun startServiceImmediately(context: Context) {
//        val serviceIntent = Intent(context, MotionDetectionService::class.java)
//        context.startForegroundService(serviceIntent)
        startDetectionWorker(context)

    }

    fun stopServiceImmediately(context: Context) {
//        val serviceIntent = Intent(context, MotionDetectionService::class.java)
//        context.stopService(serviceIntent)
        stopDetectionWorker(context)
    }

    fun startDetectionWorker(context: Context) {
        val motionDetectionWorkRequest =
            PeriodicWorkRequestBuilder<MotionDetectionWorker>(10, TimeUnit.MINUTES)
                .addTag("detection_worker")
                .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "MotionDetectionWorker",
            ExistingPeriodicWorkPolicy.UPDATE,
            motionDetectionWorkRequest
        )
    }

    fun stopDetectionWorker(context: Context) {
        WorkManager.getInstance(context).cancelAllWorkByTag("detection_worker")
    }
}