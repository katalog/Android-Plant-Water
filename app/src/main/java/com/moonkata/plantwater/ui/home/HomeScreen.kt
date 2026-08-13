package com.moonkata.plantwater.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.moonkata.plantwater.data.local.Plant
import com.moonkata.plantwater.ui.theme.PlantWaterTheme
import com.moonkata.plantwater.util.PhotoUtils
import java.util.concurrent.TimeUnit
import kotlin.math.ceil

@Composable
fun HomeScreen(
    plants: List<Plant>,
    onPlantClick: (Long) -> Unit = {},
    onWaterNowClick: (Long) -> Unit = {},
    onAddClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            FloatingActionButton(onClick = onAddClick) {
                Text(text = "+", style = MaterialTheme.typography.headlineSmall)
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(plants, key = { it.id }) { plant ->
                PlantCard(
                    plant = plant,
                    onClick = { onPlantClick(plant.id) },
                    onWaterNowClick = { onWaterNowClick(plant.id) }
                )
            }
        }
    }
}

@Composable
private fun PlantCard(
    plant: Plant,
    onClick: () -> Unit,
    onWaterNowClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val bitmap = remember(plant.photoUri) { PhotoUtils.decodeBitmap(plant.photoUri, maxDimension = 112) }
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(12.dp))
                    )
                } else {
                    Text(text = "🌱")
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(text = plant.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = wateringStatusText(plant),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            FilledTonalButton(onClick = onWaterNowClick) {
                Text(text = "물 줬어요")
            }
        }
    }
}

private fun wateringStatusText(plant: Plant, now: Long = System.currentTimeMillis()): String {
    val daysLeft = daysUntilNextWatering(plant, now)
    return when {
        daysLeft > 0 -> "다음 물주기까지 D-$daysLeft"
        daysLeft == 0L -> "오늘 물주는 날"
        else -> "물주기 D+${-daysLeft} 지난"
    }
}

private fun daysUntilNextWatering(plant: Plant, now: Long): Long {
    val baseTimestamp = plant.lastWateredAt ?: plant.createdAt
    val intervalMillis = TimeUnit.DAYS.toMillis(plant.wateringIntervalDays.toLong())
    val nextWateringAt = baseTimestamp + intervalMillis
    val dayMillis = TimeUnit.DAYS.toMillis(1)
    return ceil((nextWateringAt - now).toDouble() / dayMillis).toLong()
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    PlantWaterTheme {
        HomeScreen(plants = dummyPlants())
    }
}

internal fun dummyPlants(): List<Plant> {
    val now = System.currentTimeMillis()
    val day = TimeUnit.DAYS.toMillis(1)
    return listOf(
        Plant(
            id = 1,
            name = "몬스테라",
            species = "Monstera deliciosa",
            wateringIntervalDays = 7,
            reminderHour = 9,
            reminderMinute = 0,
            lastWateredAt = now - 5 * day
        ),
        Plant(
            id = 2,
            name = "산세베리아",
            species = "Sansevieria trifasciata",
            wateringIntervalDays = 14,
            reminderHour = 9,
            reminderMinute = 0,
            lastWateredAt = now - 16 * day
        ),
        Plant(
            id = 3,
            name = "금전수",
            species = "Zamioculcas zamiifolia",
            wateringIntervalDays = 10,
            reminderHour = 9,
            reminderMinute = 0,
            lastWateredAt = now - 3 * day
        )
    )
}
