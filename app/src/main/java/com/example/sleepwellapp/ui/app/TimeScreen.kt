package com.example.sleepwellapp.ui.app

import android.app.TimePickerDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.sleepwellapp.datalayer.DayTimeEntity
import com.example.sleepwellapp.datalayer.MainViewModel
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Composable
fun TimeScreen(viewModel: MainViewModel, padding: PaddingValues) {
    Column(modifier = Modifier.padding(padding)) {
        val dayTimes by viewModel.dayTimes.collectAsState()

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(dayTimes) { dayTime ->
                DayTimeItem(dayTime, viewModel)
            }
        }
    }
}

@Composable
fun DayTimeItem(dayTime: DayTimeEntity, viewModel: MainViewModel) {
    val context = LocalContext.current

    var showWakeUpDialog by remember { mutableStateOf(false) }
    var showSleepDialog by remember { mutableStateOf(false) }
    var enforced by remember { mutableStateOf(dayTime.enforced) }

    if (showWakeUpDialog) {
        TimePickerDialog(
            context,
            { _, hourOfDay, minute ->
                val newWakeUpTime = LocalTime.of(hourOfDay, minute).format(DateTimeFormatter.ofPattern("HH:mm"))
                viewModel.updateDayTime(dayTime.copy(wakeUpTime = newWakeUpTime))
                showWakeUpDialog = false
            },
            LocalTime.parse(dayTime.wakeUpTime).hour,
            LocalTime.parse(dayTime.wakeUpTime).minute,
            true
        ).show()
        showWakeUpDialog = false
    }

    if (showSleepDialog) {
        TimePickerDialog(
            context,
            { _, hourOfDay, minute ->
                val newSleepTime = LocalTime.of(hourOfDay, minute).format(DateTimeFormatter.ofPattern("HH:mm"))
                viewModel.updateDayTime(dayTime.copy(sleepTime = newSleepTime))
                showSleepDialog = false
            },
            LocalTime.parse(dayTime.sleepTime).hour,
            LocalTime.parse(dayTime.sleepTime).minute,
            true
        ).show()
        showSleepDialog = false
    }

    Column(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth()
    ) {
        Text(text = dayTime.day, style = MaterialTheme.typography.titleMedium)
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            TextButton(onClick = { showWakeUpDialog = true }) {
                Text(text = "Wake Up: ${formatTime(dayTime.wakeUpTime)}")
            }
            TextButton(onClick = { showSleepDialog = true }) {
                Text(text = "Sleep: ${formatTime(dayTime.sleepTime)}")
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = enforced,
                onCheckedChange = {
                    enforced = it
                    viewModel.updateDayTime(dayTime.copy(enforced = it))
                }
            )
            Text(text = "Enforce Schedule")
        }
    }
}

@Composable
fun formatTime(timeString: String): String {
    val time = LocalTime.parse(timeString)
    return time.format(DateTimeFormatter.ofPattern("hh:mm a"))
}