package com.sleepwellapp.services

import android.Manifest
import android.Manifest.permission.FOREGROUND_SERVICE_HEALTH
import android.app.Application
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.ActivityCompat
import androidx.core.app.JobIntentService
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.LifecycleService
import com.sleepwellapp.R
import com.sleepwellapp.datalayer.MainViewModel
import java.util.Calendar
import java.util.Locale


class MotionDetectionService : LifecycleService(), SensorEventListener {
    private lateinit var sensorManager: SensorManager
    private lateinit var accelerometer: Sensor
    private lateinit var viewModel: MainViewModel

    companion object {
        var isRunning = false
    }

    override fun onCreate() {
        super.onCreate()

        val notification = createNotification(
            "Time to Sleep", "It's time to go to bed!", applicationContext, 1)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH)
        } else {
            startForeground(1, notification)
        }

        viewModel = MainViewModel(application as Application)

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager

    }

    fun onActivation() {
        isRunning = true
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)!!
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL)
    }

    fun onDeactivation() {
        isRunning = false
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            val x = it.values[0]
            val y = it.values[1]
            val z = it.values[2]

            if (isSignificantMotion(x, y, z)) {
                val day = Calendar.getInstance().getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, Locale.getDefault()) ?: "Unknown"
                viewModel.recordMotion(day)
            }
        }
    }

    private fun isSignificantMotion(x: Float, y: Float, z: Float): Boolean {
        val threshold = 1.5f
        return x > threshold || y > threshold || z > threshold
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onDestroy() {
        createNotification(
            "Time to Wake Up", "It's time to wake up!", applicationContext, 1)
        super<LifecycleService>.onDestroy()
        isRunning = false
        sensorManager.unregisterListener(this)
    }

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        return null
    }

    private fun createNotification(
        title: String, text: String, applicationContext: Context, notId: Int) : Notification {
        val channelId = "SleepScheduleNotificationChannel"
        val channel = NotificationChannel(channelId, "Sleep Schedule Notifications", NotificationManager.IMPORTANCE_HIGH)
        val manager = applicationContext.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)

        return NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
    }
}
