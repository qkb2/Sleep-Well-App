package com.example.sleepwellapp.datalayer

import android.app.Application
import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
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

    private val repository: AuthRepository = AuthRepository()
    private val remoteDB: RemoteFirebaseDB = RemoteFirebaseDB()

//    for remote db
    val _remoteNightTimes = MutableLiveData<List<RemoteDayTimeEntity>>()
    val addSuccess = MutableLiveData<Boolean>()
    val deleteSuccess = MutableLiveData<Boolean>()
    val updateSuccess = MutableLiveData<Boolean>()



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
//            load local db
            forcePullRemoteDb()
        }
    }

    private suspend fun forcePullRemoteDb(){
        if (!_isLoggedIn.value) {
            Log.d("TAG", "force_pull_remote_db: not logged in")
            return
        }

        Log.d("TAG", "force_pull_remote_db: ")
        //pull from remote db
        _remoteNightTimes.value = remoteDB.fetchAllDayTimes()
        Log.d("TAG", "after loadRemoteDayTimes(: ")
        // check if remote is empty
        if (_remoteNightTimes.value.isNullOrEmpty()){
            // seed remote db
            Log.d("TAG", "force_pull_remote_db: seeding remote db")
            withContext(Dispatchers.IO) {
                _nightTimes.value.forEach {
                    Log.d("ADD", "Adding ${it.startDay}")
                    remoteDB.addDayTime(toRemoteDbFormat(it, repository.getUserId()))
                }
            }
        } else {
            Log.d("TAG", "force_pull_remote_db: remote db is not empty")
            // sync local db
            withContext(Dispatchers.IO) {
                nightTimeDao.deleteAll()
                _remoteNightTimes.value!!.forEach {
                    nightTimeDao.insert(toLocalDbFormat(it))
                    }
            }
        }
        // sync the data
        _remoteNightTimes.value = remoteDB.fetchAllDayTimes()
    }

//    private suspend fun syncPushRemoteDB(){
//        Log.d("TAG", "syncPushRemoteDB: ")
//        if (!_isLoggedIn.value){
//            return
//        }
//        //pull from remote db
//        _remoteNightTimes.value = remoteDB.fetchAllDayTimes()
//        // check if remote is empty
////        check which elements are in local db but not in remote db
//        val localDb = _nightTimes.value
//        val remoteDb = _remoteNightTimes.value
//        val localDbIds = localDb.map { it.id }
//        val remoteDbIds = remoteDb!!.map { it.id }
//        val toAdd = localDb.filter { !remoteDbIds.contains(it.id) }
//        val toDelete = remoteDb.filter { !localDbIds.contains(it.id) }
//        val toUpdate = localDb.filter { remoteDbIds.contains(it.id) }
//
//        toAdd.forEach {
//            remoteDB.addDayTime(toRemoteDbFormat(it, repository.getUserId()))
//        }
//
//        toDelete.forEach {
//            remoteDB.deleteDayTime(it.documentId)
//        }
//
//        toUpdate.forEach { localDayTimeEntity ->
//            val remoteDaytime = remoteDb.find {
//                it.id == localDayTimeEntity.id
//            }
//            if (remoteDaytime != null) {
//                //update remote db
//                remoteDB.updateDayTime(
//                    remoteDaytime.documentId,
//                    toRemoteDbFormat(localDayTimeEntity, repository.getUserId()))
//            }
//        }
//
//        // sync the data
//        withContext(Dispatchers.IO){
//            _remoteNightTimes.value = remoteDB.fetchAllDayTimes()
//        }
//
//
//    }

    fun toggleDarkMode() {
        viewModelScope.launch {
            val newMode = !_isDarkMode.value
            userPreferences.saveDarkModePreference(newMode)
            _isDarkMode.value = newMode
        }
    }

    fun saveCredentials() {
//        get uid from auth provider
        val username = loginUiState.userName
        val uid = repository.getUserId()
        viewModelScope.launch {
            userPreferences.saveCredentials(username, uid)
            _isLoggedIn.value = true
            initializeNights()
        }
        Log.d("TAG", "saveCredentials: $username")

//        this means login, so we need to seed the remote db
//        TODO this might be bad code
        viewModelScope.launch {
            forcePullRemoteDb()
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
        Log.d("TAG", "updateNightTime: ")
        // get the RemoteDayTimeEntity with the same id
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val remoteNightTime  = _remoteNightTimes.value!!.find { it.id == nightTime.id }
                if (remoteNightTime != null) {
                    //update remote db
                    Log.d("TAG", "updateNightTime: updating remote db")
                    remoteDB.updateDayTime(
                        remoteNightTime.documentId,
                        toRemoteDbFormat(nightTime, repository.getUserId())
                    )
                } else {
                    Log.d("TAG", "updateNightTime: missing remote elem")
                }
            }

            _remoteNightTimes.value = _remoteNightTimes.value!!.map {
                if (it.id == nightTime.id) {
                    toRemoteDbFormat(nightTime, repository.getUserId())
                } else {
                    it
                }
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
        // get last element or is there another way to get the id ??
        val last = _nightTimes.value.last()
        val nightTime = toRemoteDbFormat(last, repository.getUserId() )
        viewModelScope.launch {
            val remote = withContext(Dispatchers.IO) {
                remoteDB.addDayTime( nightTime )
            }
            if (remote != null) {
                _remoteNightTimes.value =
                    _remoteNightTimes.value!!.plus(remote)
            }
        }

    }

    fun removeNightTime(nightTime: NightTimeEntity) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { nightTimeDao.delete(nightTime) }
            loadNightTimes()
        }

        // get the RemoteDayTimeEntity with the same id
        viewModelScope.launch {
            val remoteNightTime  = _remoteNightTimes.value!!.find { it.id == nightTime.id }
            withContext(Dispatchers.IO) {
                if (remoteNightTime != null) {
                    //update remote db
                    remoteDB.deleteDayTime(remoteNightTime.documentId)
                }
            }
            _remoteNightTimes.value = _remoteNightTimes.value!!.filter { it.id != nightTime.id }
        }
    }

    fun logout() {
        viewModelScope.launch {
            userPreferences.clearCredentials()
            _isLoggedIn.value = false
            repository.logout()
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            userPreferences.clearAllData()
            withContext(Dispatchers.IO) { nightTimeDao.deleteAll() }
            _nightTimes.value = emptyList()
        }

        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                for (remoteNightTime in _remoteNightTimes.value!!) {
                    remoteDB.deleteDayTime(remoteNightTime.documentId)
                }
            }
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


//    +++++ LOG IN STUFF +++++
    val currentUser = repository.currentUser
    val hasUser: Boolean
        get() = repository.hasUser()

    var loginUiState by mutableStateOf(LoginUiState())
        private set

    fun toggleWantToRegister() {
        loginUiState = loginUiState.copy(wantToRegister = !loginUiState.wantToRegister)
    }

    fun onUserNameChange(userName: String) {
        loginUiState = loginUiState.copy(userName = userName)
    }

    fun onPasswordNameChange(password: String) {
        loginUiState = loginUiState.copy(password = password)
    }

    fun onUserNameChangeSignup(userName: String) {
        loginUiState = loginUiState.copy(userNameSignUp = userName)
    }

    fun onPasswordChangeSignup(password: String) {
        loginUiState = loginUiState.copy(passwordSignUp = password)
    }

    fun onConfirmPasswordChange(password: String) {
        loginUiState = loginUiState.copy(confirmPasswordSignUp = password)
    }

    private fun validateLoginForm() =
        loginUiState.userName.isNotBlank() &&
                loginUiState.password.isNotBlank()

    private fun validateSignupForm() =
        loginUiState.userNameSignUp.isNotBlank() &&
                loginUiState.passwordSignUp.isNotBlank() &&
                loginUiState.confirmPasswordSignUp.isNotBlank()


    fun createUser(context: Context) = viewModelScope.launch {
        try {
            if (!validateSignupForm()) {
                throw IllegalArgumentException("email and password can not be empty")
            }
            loginUiState = loginUiState.copy(isLoading = true)
            if (loginUiState.passwordSignUp !=
                loginUiState.confirmPasswordSignUp
            ) {
                throw IllegalArgumentException(
                    "Passwords do not match"
                )
            }
            loginUiState = loginUiState.copy(signUpError = null)
            repository.createUser(
                loginUiState.userNameSignUp,
                loginUiState.passwordSignUp
            ) { isSuccessful ->
                if (isSuccessful) {
                    Toast.makeText(
                        context,
                        "success Login",
                        Toast.LENGTH_SHORT
                    ).show()
                    loginUiState = loginUiState.copy(isSuccessLogin = true)
                } else {
                    Toast.makeText(
                        context,
                        "Failed Login",
                        Toast.LENGTH_SHORT
                    ).show()
                    loginUiState = loginUiState.copy(isSuccessLogin = false)
                }

            }


        } catch (e: Exception) {
            loginUiState = loginUiState.copy(signUpError = e.localizedMessage)
            e.printStackTrace()
        } finally {
            loginUiState = loginUiState.copy(isLoading = false)
        }


    }

    fun loginUser(context: Context) = viewModelScope.launch {
        try {
            if (!validateLoginForm()) {
                throw IllegalArgumentException("email and password can not be empty")
            }
            loginUiState = loginUiState.copy(isLoading = true)
            loginUiState = loginUiState.copy(loginError = null)
            repository.login(
                loginUiState.userName,
                loginUiState.password
            ) { isSuccessful ->
                if (isSuccessful) {
                    Toast.makeText(
                        context,
                        "Login Successful",
                        Toast.LENGTH_SHORT
                    ).show()
                    loginUiState = loginUiState.copy(isSuccessLogin = true)
                } else {
                    Toast.makeText(
                        context,
                        "Login Failed",
                        Toast.LENGTH_LONG
                    ).show()
                    loginUiState = loginUiState.copy(isSuccessLogin = false)
                }

            }


        } catch (e: Exception) {
            loginUiState = loginUiState.copy(loginError = e.localizedMessage)
            e.printStackTrace()
        } finally {
            loginUiState = loginUiState.copy(isLoading = false)
        }

    }

}


data class LoginUiState(
    val userName: String = "",
    val password: String = "",
    val userNameSignUp: String = "",
    val passwordSignUp: String = "",
    val confirmPasswordSignUp: String = "",
    val isLoading: Boolean = false,
    val isSuccessLogin: Boolean = false,
    val signUpError: String? = null,
    val loginError: String? = null,
    val wantToRegister:Boolean = false,
)