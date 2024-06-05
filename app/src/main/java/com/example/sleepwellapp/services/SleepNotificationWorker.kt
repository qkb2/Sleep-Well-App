package com.example.sleepwellapp.services

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.sleepwellapp.R

class SleepNotificationWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        createNotification(
            "Time to Sleep", "It's time to go to bed!", applicationContext, 1)

        // Start the motion detection service
//        val serviceIntent = Intent(applicationContext, MotionDetectionService::class.java)
//        applicationContext.startForegroundService(serviceIntent)
        ScheduleUtil.startDetectionWorker(applicationContext)

        return Result.success()
    }
}

class WakeNotificationWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        createNotification(
            "Time to Wake Up", "It's time to wake up!", applicationContext, 2)

        // Stop the motion detection service
//        val serviceIntent = Intent(applicationContext, MotionDetectionService::class.java)
//        applicationContext.stopService(serviceIntent)

        ScheduleUtil.stopDetectionWorker(applicationContext)

        return Result.success()
    }
}

private fun createNotification(title: String, text: String, applicationContext: Context, notId: Int) {
    val channelId = "SleepScheduleNotificationChannel"
    val channel = NotificationChannel(channelId, "Sleep Schedule Notifications", NotificationManager.IMPORTANCE_HIGH)
    val manager = applicationContext.getSystemService(NotificationManager::class.java)
    manager.createNotificationChannel(channel)

    val notification = NotificationCompat.Builder(applicationContext, channelId)
        .setSmallIcon(R.drawable.ic_launcher_foreground)
        .setContentTitle(title)
        .setContentText(text)
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .build()

    with(NotificationManagerCompat.from(applicationContext)) {
        if (ActivityCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        }
        notify(notId, notification)
    }
}