package com.moonkata.plantwater

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import com.moonkata.plantwater.data.local.Plant
import com.moonkata.plantwater.data.local.PlantDatabase
import com.moonkata.plantwater.data.local.PlantRepository
import com.moonkata.plantwater.recognition.PlantIdentification
import com.moonkata.plantwater.reminder.ReminderScheduler
import com.moonkata.plantwater.ui.calendar.CalendarScreen
import com.moonkata.plantwater.ui.camera.CameraScreen
import com.moonkata.plantwater.ui.home.HomeScreen
import com.moonkata.plantwater.ui.info.InfoScreen
import com.moonkata.plantwater.ui.recognition.RecognitionScreen
import com.moonkata.plantwater.ui.schedule.ScheduleScreen
import com.moonkata.plantwater.ui.theme.PlantWaterTheme
import kotlinx.coroutines.launch

private sealed interface Screen {
    data object Home : Screen
    data object Camera : Screen
    data class Recognizing(val photoUri: String) : Screen
    data class Info(val photoUri: String, val identification: PlantIdentification?) : Screen
    data class NewPlantSchedule(
        val photoUri: String?,
        val name: String,
        val species: String,
        val initialIntervalDays: Int
    ) : Screen
    data class PlantCalendar(val plantId: Long) : Screen
    data class EditPlantSchedule(
        val plantId: Long,
        val intervalDays: Int,
        val hour: Int,
        val minute: Int
    ) : Screen
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val repository = PlantRepository(PlantDatabase.getInstance(applicationContext))

        setContent {
            PlantWaterTheme {
                val context = LocalContext.current
                val scope = rememberCoroutineScope()
                val plants by repository.observePlants().collectAsState(initial = emptyList())
                var screen by remember { mutableStateOf<Screen>(Screen.Home) }

                val notificationPermissionLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission()
                ) { /* 거부해도 앱은 계속 쓸 수 있게 두고, 알림만 안 뜸 */ }

                LaunchedEffect(Unit) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }

                when (val currentScreen = screen) {
                    Screen.Home -> HomeScreen(
                        plants = plants,
                        onPlantClick = { plantId -> screen = Screen.PlantCalendar(plantId) },
                        onWaterNowClick = { plantId ->
                            scope.launch { repository.markWatered(plantId) }
                        },
                        onAddClick = { screen = Screen.Camera }
                    )

                    Screen.Camera -> CameraScreen(
                        onPhotoCaptured = { uri ->
                            screen = Screen.Recognizing(photoUri = uri.toString())
                        },
                        onClose = { screen = Screen.Home }
                    )

                    is Screen.Recognizing -> RecognitionScreen(
                        photoUri = currentScreen.photoUri.toUri(),
                        onResult = { identification ->
                            screen = Screen.Info(
                                photoUri = currentScreen.photoUri,
                                identification = identification
                            )
                        }
                    )

                    is Screen.Info -> InfoScreen(
                        photoUri = currentScreen.photoUri.toUri(),
                        identification = currentScreen.identification,
                        onConfirm = { name, species, intervalDays ->
                            screen = Screen.NewPlantSchedule(
                                photoUri = currentScreen.photoUri,
                                name = name,
                                species = species,
                                initialIntervalDays = intervalDays
                            )
                        },
                        onNavigateBack = { screen = Screen.Home }
                    )

                    is Screen.NewPlantSchedule -> ScheduleScreen(
                        isEditMode = false,
                        initialIntervalDays = currentScreen.initialIntervalDays,
                        onNavigateBack = { screen = Screen.Home },
                        onSave = { intervalDays, hour, minute ->
                            scope.launch {
                                val plantId = repository.addPlant(
                                    Plant(
                                        name = currentScreen.name,
                                        species = currentScreen.species,
                                        photoUri = currentScreen.photoUri,
                                        wateringIntervalDays = intervalDays,
                                        reminderHour = hour,
                                        reminderMinute = minute
                                    )
                                )
                                ReminderScheduler.schedule(context, plantId, intervalDays, hour, minute)
                                screen = Screen.Home
                            }
                        }
                    )

                    is Screen.PlantCalendar -> {
                        val plantId = currentScreen.plantId
                        val plant by remember(plantId) { repository.observePlant(plantId) }
                            .collectAsState(initial = null)
                        val logs by remember(plantId) { repository.observeLogs(plantId) }
                            .collectAsState(initial = emptyList())

                        CalendarScreen(
                            plant = plant,
                            logs = logs,
                            onWaterNowClick = {
                                scope.launch { repository.markWatered(plantId) }
                            },
                            onEditClick = {
                                plant?.let {
                                    screen = Screen.EditPlantSchedule(
                                        plantId = plantId,
                                        intervalDays = it.wateringIntervalDays,
                                        hour = it.reminderHour,
                                        minute = it.reminderMinute
                                    )
                                }
                            },
                            onDeleteConfirmed = {
                                scope.launch {
                                    repository.deletePlant(plantId)
                                    ReminderScheduler.cancel(context, plantId)
                                    screen = Screen.Home
                                }
                            },
                            onNavigateBack = { screen = Screen.Home },
                            onDebugTestReminderClick = { delayMinutes ->
                                ReminderScheduler.scheduleDebugTest(context, plantId, delayMinutes)
                            }
                        )
                    }

                    is Screen.EditPlantSchedule -> ScheduleScreen(
                        isEditMode = true,
                        initialIntervalDays = currentScreen.intervalDays,
                        initialHour = currentScreen.hour,
                        initialMinute = currentScreen.minute,
                        onNavigateBack = { screen = Screen.PlantCalendar(currentScreen.plantId) },
                        onSave = { intervalDays, hour, minute ->
                            scope.launch {
                                repository.updateSchedule(currentScreen.plantId, intervalDays, hour, minute)
                                ReminderScheduler.schedule(context, currentScreen.plantId, intervalDays, hour, minute)
                                screen = Screen.PlantCalendar(currentScreen.plantId)
                            }
                        }
                    )
                }
            }
        }
    }
}
