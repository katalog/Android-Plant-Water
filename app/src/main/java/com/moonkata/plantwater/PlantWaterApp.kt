package com.moonkata.plantwater

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.moonkata.plantwater.reminder.NotificationConstants

class PlantWaterApp : Application() {
    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NotificationConstants.CHANNEL_ID,
                "물주기 알림",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "식물 물주기 시간을 알려줍니다"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }
}
