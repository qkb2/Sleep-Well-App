package com.sleepwellapp.ui.app

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.sleepwellapp.datalayer.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel = viewModel()) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    var expanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Main Menu") },
                actions = {
                    IconButton(onClick = { viewModel.toggleDarkMode() }) {
                        Icon(Icons.Default.Star, contentDescription = "Toggle Dark Mode")
                    }
                    IconButton(onClick = { expanded = true }) {
                        Icon(Icons.Default.Settings, contentDescription = "Options")
                    }
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        DropdownMenuItem(
                            onClick = {
                                viewModel.logout()
                                expanded = false
                            },
                            text = {
                                Text(text = "Log out")
                            }
                        )
                        DropdownMenuItem(
                            onClick = {
                                viewModel.toggleDarkMode()
                                expanded = false
                            },
                            text = {
                                Text(text = "Toggle Dark Mode")
                            }
                        )
                        DropdownMenuItem(
                            onClick = {
                                viewModel.clearAllData()
                                expanded = false
                            },
                            text = {
                                Text(text = "Clear All Data")
                            }
                        )
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Home, contentDescription = "Main Screen") },
                    label = { Text("Main") },
                    selected = currentRoute == "main",
                    onClick = {
                        navController.navigate("main") {
                            popUpTo(navController.graph.startDestinationId)
                            launchSingleTop = true
                        }
                    }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Info, contentDescription = "Statistics") },
                    label = { Text("Statistics") },
                    selected = currentRoute == "statistics",
                    onClick = {
                        navController.navigate("statistics") {
                            popUpTo(navController.graph.startDestinationId)
                            launchSingleTop = true
                        }
                    }
                )
            }
        }
    ) { padding ->
        AppNavGraph(navController, viewModel, padding)
    }
}

@Composable
fun AppNavGraph(navController: NavHostController, viewModel: MainViewModel, padding: PaddingValues) {
    NavHost(navController = navController, startDestination = "main") {
        composable("main") {
            TimeScreen(viewModel, padding)
        }
        composable("statistics") {
            StatisticsScreen(viewModel, padding)
        }
    }
}
