package com.moonkata.plantwater.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import com.moonkata.plantwater.data.local.PlantDatabase
import com.moonkata.plantwater.data.local.PlantRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

// 알림의 "물 줬어요" / "나중에" 액션 버튼 처리. 앱을 열지 않아도 동작해야 하므로 BroadcastReceiver로 구현
class WaterActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1)
        if (notificationId != -1) {
            NotificationManagerCompat.from(context).cancel(notificationId)
        }

        val plantId = intent.getLongExtra(EXTRA_PLANT_ID, -1L)
        if (intent.action != ACTION_MARK_WATERED || plantId < 0) return

        // BroadcastReceiver는 onReceive가 끝나면 죽을 수 있으므로 goAsync로 suspend 작업이 끝날 때까지 살려둠
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repository = PlantRepository(PlantDatabase.getInstance(context.applicationContext))
                repository.markWatered(plantId)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_MARK_WATERED = "com.moonkata.plantwater.action.MARK_WATERED"
        const val ACTION_SNOOZE = "com.moonkata.plantwater.action.SNOOZE"
        const val EXTRA_PLANT_ID = "extra_plant_id"
        const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
    }
}
