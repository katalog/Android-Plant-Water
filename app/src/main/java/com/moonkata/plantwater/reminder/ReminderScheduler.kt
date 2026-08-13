package com.moonkata.plantwater.reminder

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import java.util.Calendar
import java.util.concurrent.TimeUnit

// 식물별 반복 알림 예약/취소. 워크 이름을 plantId로 고정해서 재저장 시 새 요청이 기존 것을 대체하게 함
object ReminderScheduler {

    fun schedule(context: Context, plantId: Long, intervalDays: Int, hour: Int, minute: Int) {
        val request = PeriodicWorkRequestBuilder<WaterReminderWorker>(intervalDays.toLong(), TimeUnit.DAYS)
            .setInitialDelay(initialDelayMillis(hour, minute), TimeUnit.MILLISECONDS)
            .setInputData(workDataOf(WaterReminderWorker.KEY_PLANT_ID to plantId))
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            workName(plantId),
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    fun cancel(context: Context, plantId: Long) {
        WorkManager.getInstance(context).cancelUniqueWork(workName(plantId))
    }

    // 디버그 전용: 실제 반복 예약과 별개로 N분 뒤 1회짜리 테스트 알림을 발사.
    // WorkManager의 PeriodicWorkRequest는 최소 간격이 15분으로 고정돼 있어 1분/5분 반복은 불가능하므로,
    // 반복이 아닌 단발성 알림으로 노티/액션 버튼 동작만 빠르게 확인하는 용도.
    fun scheduleDebugTest(context: Context, plantId: Long, delayMinutes: Long) {
        val request = OneTimeWorkRequestBuilder<WaterReminderWorker>()
            .setInitialDelay(delayMinutes, TimeUnit.MINUTES)
            .setInputData(workDataOf(WaterReminderWorker.KEY_PLANT_ID to plantId))
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            debugWorkName(plantId),
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    private fun workName(plantId: Long) = "water_reminder_plant_$plantId"
    private fun debugWorkName(plantId: Long) = "water_reminder_debug_test_$plantId"

    // 오늘 hour:minute이 이미 지났으면 내일 그 시각까지의 지연시간을 계산
    internal fun initialDelayMillis(hour: Int, minute: Int, now: Calendar = Calendar.getInstance()): Long {
        val target = (now.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (!target.after(now)) {
            target.add(Calendar.DAY_OF_YEAR, 1)
        }
        return target.timeInMillis - now.timeInMillis
    }
}
