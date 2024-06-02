package com.example.sleepwellapp.datalayer

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val db = Room.databaseBuilder(application, AppDatabase::class.java, "app-db").build()
    private val dayTimeDao = db.dayTimeDao()
    private val motionCountDao = db.motionCountDao()

    private val _dayTimes = MutableStateFlow<List<DayTimeEntity>>(emptyList())
    val dayTimes: StateFlow<List<DayTimeEntity>> = _dayTimes

    private val cryptoManager = CryptoManager()
    private val userPreferences = UserPreferences(application, cryptoManager)

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
            loadDayTimes()
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
            initializeDays()
        }
    }

    private fun initializeDays() {
        viewModelScope.launch {
            val daysOfWeek = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
            withContext(Dispatchers.IO) {
                daysOfWeek.forEach { day ->
                    if (dayTimeDao.getByDay(day) == null) {
                        dayTimeDao.insert(DayTimeEntity(day = day, wakeUpTime = "07:00", sleepTime = "22:00"))
                    }
                }
            }
            loadDayTimes()
        }
    }

    private fun loadDayTimes() {
        viewModelScope.launch {
            _dayTimes.value = withContext(Dispatchers.IO) { dayTimeDao.getAll() }
        }
    }

    fun updateDayTime(dayTime: DayTimeEntity) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { dayTimeDao.update(dayTime) }
            loadDayTimes()
        }
    }

    fun addDayTime(day: String, wakeUpTime: String, sleepTime: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                dayTimeDao.insert(DayTimeEntity(day = day, wakeUpTime = wakeUpTime, sleepTime = sleepTime))
            }
            loadDayTimes()
        }
    }

    fun removeDayTime(dayTime: DayTimeEntity) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { dayTimeDao.delete(dayTime) }
            loadDayTimes()
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
            withContext(Dispatchers.IO) { dayTimeDao.deleteAll() }
            _dayTimes.value = emptyList()
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

    private fun cleanOldData() {
        viewModelScope.launch(Dispatchers.IO) {
            val calendar = Calendar.getInstance()
            calendar.add(Calendar.DAY_OF_YEAR, -7)
            val cutoffDate = calendar.time
            motionCountDao.deleteOldData(cutoffDate.time)
        }
    }
}