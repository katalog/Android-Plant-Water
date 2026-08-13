package com.moonkata.plantwater.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "plants")
data class Plant(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val species: String,
    val photoUri: String? = null,
    val wateringIntervalDays: Int,
    val reminderHour: Int,
    val reminderMinute: Int,
    val lastWateredAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)
