package com.example.sleepwellapp.ui.app

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.sleepwellapp.datalayer.MainViewModel

@Composable
fun StatisticsScreen(viewModel: MainViewModel, padding: PaddingValues) {
    Column(modifier = Modifier.padding(padding)) {
        Text(text = "Here be statistics")
    }
}