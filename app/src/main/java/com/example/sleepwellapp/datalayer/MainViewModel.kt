package com.example.sleepwellapp.datalayer

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import com.example.sleepwellapp.services.MotionDetectionService
import com.example.sleepwellapp.services.ScheduleUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.util.Calendar

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val nightTimeDao = AppDatabase.getInstance(application).nightTimeDao()
    private val motionCountDao = AppDatabase.getInstance(application).motionCountDao()
    private val userPreferences = UserPreferences(application, CryptoManager())

    private val _nightTimes = MutableStateFlow<List<NightTimeEntity>>(emptyList())
    val nightTimes: StateFlow<List<NightTimeEntity>> = _nightTimes

    private val _isDarkMode = MutableStateFlow(false)
    val isDarkMode: StateFlow<Boolean> = _isDarkMode

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn

    init {
        viewModelScope.launch {
            val username = userPreferences.usernameFlow.first()
            val password = userPreferences.passwordFlow.first()
            _isLoggedIn.value = username != null && password != null
            _isDarkMode.value = userPreferences.darkModeFlow.first()
            loadNightTimes()
            withContext(Dispatchers.Main) {
                ScheduleUtil.scheduleNightlyNotifications(application)
            }
        }
    }

    fun toggleDarkMode() {
        viewModelScope.launch {
            val newMode = !_isDarkMode.value
            userPreferences.saveDarkModePreference(newMode)
            _isDarkMode.value = newMode
        }
    }

    fun saveCredentials(username: String, password: String) {
        viewModelScope.launch {
            userPreferences.saveCredentials(username, password)
            _isLoggedIn.value = true
            initializeNights()
        }
    }

    private fun initializeNights() {
        viewModelScope.launch {
            val daysOfWeek = DayOfWeek.entries.map { it.name }
            withContext(Dispatchers.IO) {
                daysOfWeek.forEachIndexed { index, startDay ->
                    val endDay = daysOfWeek[(index + 1) % daysOfWeek.size]
                    if (nightTimeDao.getByStartDay(startDay) == null) {
                        nightTimeDao.insert(NightTimeEntity(startDay = startDay, endDay = endDay, sleepTime = "22:00", wakeUpTime = "07:00", enabled = true))
                    }
                }
            }
            loadNightTimes()
        }
    }

    fun loadNightTimes() {
        viewModelScope.launch {
            _nightTimes.value = withContext(Dispatchers.IO) { nightTimeDao.getAll() }
        }
    }

    fun updateNightTime(nightTime: NightTimeEntity) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { nightTimeDao.update(nightTime) }
            loadNightTimes()
            // Check if service should be started or stopped immediately
            handleServiceUpdate(nightTime)
            withContext(Dispatchers.Main) {
                ScheduleUtil.scheduleNightlyNotifications(getApplication())
            }
        }
    }

    private fun handleServiceUpdate(nightTime: NightTimeEntity) {
        val currentDay = LocalDate.now().dayOfWeek.name
        if (nightTime.startDay != currentDay && nightTime.endDay != currentDay) return

        if (!nightTime.enabled) {
            ScheduleUtil.stopServiceImmediately(getApplication())
            return
        }

        val currentTime = LocalTime.now()
        val sleepTime = LocalTime.parse(nightTime.sleepTime)
        val wakeUpTime = LocalTime.parse(nightTime.wakeUpTime)

        when {
            currentTime.isAfter(sleepTime) || currentTime.isBefore(wakeUpTime) -> {
                if (!MotionDetectionService.isRunning) {
                    ScheduleUtil.startServiceImmediately(getApplication())
                }
            }
            currentTime.isAfter(wakeUpTime) && currentTime.isBefore(sleepTime) -> {
                if (MotionDetectionService.isRunning) {
                    ScheduleUtil.stopServiceImmediately(getApplication())
                }
            }
        }
    }

    fun addNightTime(startDay: String, endDay: String, sleepTime: String, wakeUpTime: String, enabled: Boolean) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                nightTimeDao.insert(NightTimeEntity(startDay = startDay, endDay = endDay, sleepTime = sleepTime, wakeUpTime = wakeUpTime, enabled = enabled))
            }
            loadNightTimes()
        }
    }

    fun removeNightTime(nightTime: NightTimeEntity) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { nightTimeDao.delete(nightTime) }
            loadNightTimes()
        }
    }

    fun logout() {
        viewModelScope.launch {
            userPreferences.clearCredentials()
            _isLoggedIn.value = false
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            userPreferences.clearAllData()
            withContext(Dispatchers.IO) { nightTimeDao.deleteAll() }
            _nightTimes.value = emptyList()
        }
    }

    fun recordMotion(day: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val lastCount = motionCountDao.getLastCountForDay(day)
            if (lastCount != null) {
                motionCountDao.updateCount(lastCount.id, lastCount.count + 1)
            } else {
                motionCountDao.insert(MotionCount(day = day, count = 1))
            }
        }
    }

    fun resetMotionCount(day: String) {
        viewModelScope.launch(Dispatchers.IO) {
            motionCountDao.insert(MotionCount(day = day, count = 0))
        }
    }

    fun cleanOldData() {
        viewModelScope.launch(Dispatchers.IO) {
            val calendar = Calendar.getInstance()
            calendar.add(Calendar.DAY_OF_YEAR, -7)
            val cutoffDate = calendar.time
            motionCountDao.deleteOldData(cutoffDate.time)
        }
    }
}