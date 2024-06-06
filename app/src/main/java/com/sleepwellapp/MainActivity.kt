package com.sleepwellapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sleepwellapp.datalayer.MainViewModel
import com.sleepwellapp.ui.app.LoginScreen
import com.sleepwellapp.ui.app.MainScreen
import com.sleepwellapp.ui.theme.SleepWellAppTheme

class MainActivity : ComponentActivity() {

//    private lateinit var auth: AuthRepository
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

//        auth = AuthRepository()

        setContent {
            val viewModel: MainViewModel = viewModel()
            val isLoggedIn by viewModel.isLoggedIn.collectAsState()
            val isDarkMode by viewModel.isDarkMode.collectAsState()

            SleepWellAppTheme(darkTheme = isDarkMode) {
                if (isLoggedIn) {
                    MainScreen(viewModel)
                } else {
                    LoginScreen(viewModel)
                }
            }
        }
    }
}



