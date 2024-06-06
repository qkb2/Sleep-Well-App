package com.sleepwellapp.services

import android.app.Application
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.core.content.ContextCompat.getSystemService
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.sleepwellapp.datalayer.AppDatabase
import com.sleepwellapp.datalayer.MainViewModel
import com.sleepwellapp.datalayer.MotionCount
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.Locale
import kotlin.math.sqrt

class MotionDetectionWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params), SensorEventListener {
    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private var lightSensor: Sensor? = null
    private var motionDetected = false
    private var lightDetected = false
    private var lightLevel = 0f

    override suspend fun doWork(): Result {
        return try {
            sensorManager = applicationContext.getSystemService(Context.SENSOR_SERVICE) as SensorManager
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
            lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)

            accelerometer?.also { acc ->
                sensorManager.registerListener(this, acc, SensorManager.SENSOR_DELAY_NORMAL)
            }

            lightSensor?.also { light ->
                sensorManager.registerListener(this, light, SensorManager.SENSOR_DELAY_NORMAL)
            }

            val motionCountDao = AppDatabase.getInstance(applicationContext).motionCountDao()
            val day = Calendar.getInstance().getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, Locale.getDefault()) ?: "Unknown"
            val lastCount = motionCountDao.getLastCountForDay(day)
            if (motionDetected || lightDetected) {
                if (lastCount != null) {
                    motionCountDao.updateCount(lastCount.id, lastCount.count + 1)
                } else {
                    motionCountDao.insert(MotionCount(day = day, count = 1))
                }
            }


            // Unregister listeners after the delay
            sensorManager.unregisterListener(this)

            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            when (it.sensor.type) {
                Sensor.TYPE_ACCELEROMETER -> {
                    val x = it.values[0]
                    val y = it.values[1]
                    val z = it.values[2]

                    val acceleration = sqrt((x * x + y * y + z * z).toDouble()).toFloat()

                    if (acceleration > 12) { // threshold for motion detection
                        motionDetected = true

                    }
                }
                Sensor.TYPE_LIGHT -> {
                    lightLevel = it.values[0]

                    if (lightLevel > 100) { // threshold for light detection (adjust as necessary)
                        lightDetected = true
                    }
                }
            }
        }
    }
}
