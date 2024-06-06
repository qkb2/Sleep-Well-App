package com.sleepwellapp.ui.app

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.sleepwellapp.datalayer.MainViewModel
import com.sleepwellapp.datalayer.MotionCount
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.Alignment

@Composable
fun StatisticsScreen(viewModel: MainViewModel, padding: PaddingValues) {
    val nightTimes by viewModel.nightTimes.collectAsState()
    val motionCounts by viewModel.motionCounts.collectAsState()
    val averageSleepLength = viewModel.getAverageSleepLength()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(16.dp), // Extra padding
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
//        TODO add banner here
        Text(text = "Statistics")
        Spacer(modifier = Modifier.height(16.dp))


        if (motionCounts.isEmpty()){
            Text(text = "No data so far.")
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = "Start Tracking your sleep to see your activity.")
        }else {
            Text(text = "Nightly Activity:")
            Spacer(modifier = Modifier.height(16.dp))
            LazyColumn {
                items(nightTimes) { nightTime ->
//                   add a horizontal line to divide items
                    Canvas(modifier = Modifier.fillMaxWidth()) {
                        drawLine(
                            color = Color.Gray,
                            start = Offset(0f, 0f),
                            end = Offset(size.width, 0f),
                            strokeWidth = 1f
                        )
                    }

//                    filter out those that that are more then a week old
                    val motionCountsForDay = viewModel.getMotionCountsForDay(nightTime.startDay)
                        .filter { it.timestamp > System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000 }
                    if (motionCountsForDay.isNotEmpty()){
                        Column(
                            modifier = Modifier.padding(vertical = 8.dp),
                        ) {
                            Text(text = nightTime.startDay)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(text = "You woke up a total of ${motionCountsForDay.size} times.")
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(text = "Instead of sleeping from ${nightTime.sleepTime} to ${nightTime.wakeUpTime}.")
                        }
                    } else {
                        Column(
                            modifier = Modifier.padding(vertical = 8.dp),
                        ) {
                            Text(text = nightTime.startDay)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(text = "You slept perfectly that night.")
                        }
                    }
                }
            }
        }
    }
}
