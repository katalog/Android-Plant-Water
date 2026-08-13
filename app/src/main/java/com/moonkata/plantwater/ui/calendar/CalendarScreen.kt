package com.moonkata.plantwater.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.moonkata.plantwater.BuildConfig
import com.moonkata.plantwater.data.local.Plant
import com.moonkata.plantwater.data.local.WateringLog
import java.util.Calendar
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    plant: Plant?,
    logs: List<WateringLog>,
    onWaterNowClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteConfirmed: () -> Unit,
    onNavigateBack: () -> Unit,
    onDebugTestReminderClick: (delayMinutes: Long) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var displayedMonth by remember { mutableStateOf(Calendar.getInstance()) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    val wateredDayKeys = remember(logs) { logs.map { dayKey(it.wateredAt) }.toSet() }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(text = plant?.name ?: "") },
                navigationIcon = {
                    TextButton(onClick = onNavigateBack) {
                        Text(text = "닫기")
                    }
                },
                actions = {
                    TextButton(onClick = onEditClick) {
                        Text(text = "설정")
                    }
                    TextButton(onClick = { showDeleteDialog = true }) {
                        Text(text = "삭제")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            MonthHeader(
                displayedMonth = displayedMonth,
                onPreviousMonth = {
                    displayedMonth = (displayedMonth.clone() as Calendar).apply { add(Calendar.MONTH, -1) }
                },
                onNextMonth = {
                    displayedMonth = (displayedMonth.clone() as Calendar).apply { add(Calendar.MONTH, 1) }
                }
            )

            WeekDayHeader()

            MonthGrid(displayedMonth = displayedMonth, wateredDayKeys = wateredDayKeys)

            if (plant != null) {
                Text(
                    text = lastWateredSummary(plant),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                FilledTonalButton(onClick = onWaterNowClick, modifier = Modifier.fillMaxWidth()) {
                    Text(text = "물 줬어요")
                }

                if (BuildConfig.DEBUG) {
                    Text(
                        text = "디버그: 알림 테스트",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = { onDebugTestReminderClick(1L) }) {
                            Text(text = "1분 후 알림")
                        }
                        TextButton(onClick = { onDebugTestReminderClick(5L) }) {
                            Text(text = "5분 후 알림")
                        }
                    }
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(text = "식물을 삭제할까요?") },
            text = { Text(text = "삭제하면 물주기 기록과 알림도 함께 사라져요.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    onDeleteConfirmed()
                }) {
                    Text(text = "삭제")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(text = "취소")
                }
            }
        )
    }
}

@Composable
private fun MonthHeader(
    displayedMonth: Calendar,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextButton(onClick = onPreviousMonth) { Text(text = "◀") }
        Text(
            text = "${displayedMonth.get(Calendar.YEAR)}년 ${displayedMonth.get(Calendar.MONTH) + 1}월",
            style = MaterialTheme.typography.titleMedium
        )
        TextButton(onClick = onNextMonth) { Text(text = "▶") }
    }
}

@Composable
private fun WeekDayHeader(modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
        listOf("일", "월", "화", "수", "목", "금", "토").forEach { label ->
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun MonthGrid(displayedMonth: Calendar, wateredDayKeys: Set<Int>, modifier: Modifier = Modifier) {
    val calendar = (displayedMonth.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, 1) }
    val firstDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) // 1=일요일..7=토요일
    val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
    val year = calendar.get(Calendar.YEAR)
    val month = calendar.get(Calendar.MONTH)
    val todayKey = dayKey(System.currentTimeMillis())

    val leadingBlanks = firstDayOfWeek - 1
    val totalCells = leadingBlanks + daysInMonth
    val trailingBlanks = (7 - totalCells % 7).let { if (it == 7) 0 else it }
    val cells: List<Int?> = List(leadingBlanks) { null } + (1..daysInMonth).toList() + List(trailingBlanks) { null }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        cells.chunked(7).forEach { week ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                week.forEach { day ->
                    DayCell(
                        day = day,
                        isToday = day != null && dayKeyOf(year, month, day) == todayKey,
                        isWatered = day != null && dayKeyOf(year, month, day) in wateredDayKeys
                    )
                }
            }
        }
    }
}

@Composable
private fun RowScope.DayCell(day: Int?, isToday: Boolean, isWatered: Boolean) {
    Box(
        modifier = Modifier
            .weight(1f)
            .aspectRatio(1f)
            .padding(2.dp)
            .then(
                if (isWatered) Modifier.background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                else Modifier
            )
            .then(
                if (isToday) Modifier.border(1.dp, MaterialTheme.colorScheme.primary, CircleShape)
                else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        if (day != null) {
            Text(text = day.toString(), style = MaterialTheme.typography.bodyMedium)
        }
    }
}

private fun lastWateredSummary(plant: Plant): String {
    val lastWatered = plant.lastWateredAt ?: return "아직 물 준 기록이 없어요"
    val daysAgo = TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - lastWatered)
    return if (daysAgo <= 0) "오늘 물을 줬어요" else "마지막 물주기 ${daysAgo}일 전"
}

private fun dayKey(millis: Long): Int {
    val cal = Calendar.getInstance().apply { timeInMillis = millis }
    return dayKeyOf(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH))
}

private fun dayKeyOf(year: Int, month: Int, day: Int): Int = year * 10000 + month * 100 + day
