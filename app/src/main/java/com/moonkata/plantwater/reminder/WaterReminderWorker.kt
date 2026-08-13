package com.moonkata.plantwater.reminder

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.moonkata.plantwater.MainActivity
import com.moonkata.plantwater.R
import com.moonkata.plantwater.data.local.Plant
import com.moonkata.plantwater.data.local.PlantDatabase
import com.moonkata.plantwater.data.local.PlantRepository
import kotlinx.coroutines.flow.first

class WaterReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val plantId = inputData.getLong(KEY_PLANT_ID, -1L)
        if (plantId < 0) return Result.failure()

        val repository = PlantRepository(PlantDatabase.getInstance(applicationContext))
        val plant = repository.observePlant(plantId).first() ?: return Result.success()

        showNotification(plant)
        return Result.success()
    }

    private fun showNotification(plant: Plant) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ActivityCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val notificationId = plant.id.toInt()

        val markWateredIntent = Intent(applicationContext, WaterActionReceiver::class.java).apply {
            action = WaterActionReceiver.ACTION_MARK_WATERED
            putExtra(WaterActionReceiver.EXTRA_PLANT_ID, plant.id)
            putExtra(WaterActionReceiver.EXTRA_NOTIFICATION_ID, notificationId)
        }
        val markWateredPendingIntent = PendingIntent.getBroadcast(
            applicationContext,
            notificationId * 10 + 1,
            markWateredIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snoozeIntent = Intent(applicationContext, WaterActionReceiver::class.java).apply {
            action = WaterActionReceiver.ACTION_SNOOZE
            putExtra(WaterActionReceiver.EXTRA_NOTIFICATION_ID, notificationId)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            applicationContext,
            notificationId * 10 + 2,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val contentIntent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            applicationContext,
            notificationId,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(applicationContext, NotificationConstants.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_drop)
            .setContentTitle("${plant.name} 물 줄 시간이에요")
            .setContentText("${plant.wateringIntervalDays}일마다 물주기")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(contentPendingIntent)
            .setAutoCancel(true)
            .addAction(0, "물 줬어요", markWateredPendingIntent)
            .addAction(0, "나중에", snoozePendingIntent)
            .build()

        NotificationManagerCompat.from(applicationContext).notify(notificationId, notification)
    }

    companion object {
        const val KEY_PLANT_ID = "plant_id"
    }
}
