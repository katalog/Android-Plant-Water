package com.moonkata.plantwater.ui.schedule

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.moonkata.plantwater.ui.theme.PlantWaterTheme
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleScreen(
    isEditMode: Boolean = false,
    initialIntervalDays: Int = 7,
    initialHour: Int = 9,
    initialMinute: Int = 0,
    onSave: (intervalDays: Int, hour: Int, minute: Int) -> Unit = { _, _, _ -> },
    onNavigateBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var intervalDays by rememberSaveable { mutableIntStateOf(initialIntervalDays) }
    val timePickerState = rememberTimePickerState(
        initialHour = initialHour,
        initialMinute = initialMinute,
        is24Hour = true
    )

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(text = if (isEditMode) "일정 수정" else "일정 등록") },
                navigationIcon = {
                    TextButton(onClick = onNavigateBack) {
                        Text(text = "닫기")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(32.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = "며칠마다 물 줄까요", style = MaterialTheme.typography.titleMedium)
                Text(text = "${intervalDays}일마다", style = MaterialTheme.typography.headlineSmall)
                Slider(
                    value = intervalDays.toFloat(),
                    onValueChange = { intervalDays = it.roundToInt() },
                    valueRange = 1f..30f,
                    steps = 28
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = "알림 받을 시간", style = MaterialTheme.typography.titleMedium)
                TimePicker(state = timePickerState)
            }

            Button(
                onClick = { onSave(intervalDays, timePickerState.hour, timePickerState.minute) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = if (isEditMode) "수정하기" else "알림 등록하기")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ScheduleScreenNewPreview() {
    PlantWaterTheme {
        ScheduleScreen(isEditMode = false)
    }
}

@Preview(showBackground = true)
@Composable
private fun ScheduleScreenEditPreview() {
    PlantWaterTheme {
        ScheduleScreen(isEditMode = true, initialIntervalDays = 14, initialHour = 8, initialMinute = 30)
    }
}
