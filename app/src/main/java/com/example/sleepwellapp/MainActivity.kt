package com.example.sleepwellapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.sleepwellapp.datalayer.MainViewModel
import com.example.sleepwellapp.ui.app.LoginScreen
import com.example.sleepwellapp.ui.app.MainScreen
import com.example.sleepwellapp.ui.app.StatisticsScreen
import com.example.sleepwellapp.ui.theme.SleepWellAppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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



