package com.moonkata.plantwater.data.local

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow

class PlantRepository(
    private val db: PlantDatabase,
    private val plantDao: PlantDao = db.plantDao(),
    private val logDao: WateringLogDao = db.wateringLogDao()
) {

    fun observePlants(): Flow<List<Plant>> = plantDao.observeAll()

    fun observePlant(plantId: Long): Flow<Plant?> = plantDao.observeById(plantId)

    fun observeLogs(plantId: Long): Flow<List<WateringLog>> = logDao.observeByPlant(plantId)

    suspend fun addPlant(plant: Plant): Long = plantDao.insert(plant)

    suspend fun deletePlant(plantId: Long) = plantDao.deleteById(plantId)

    suspend fun updateSchedule(plantId: Long, intervalDays: Int, hour: Int, minute: Int) =
        plantDao.updateSchedule(plantId, intervalDays, hour, minute)

    // 홈 카드 / 캘린더 버튼 / 알림 액션 — 어디서 호출되든 이 함수 하나로 통일
    // log insert + lastWateredAt 갱신이 하나의 트랜잭션으로 묶여서, 둘 중 하나만 반영되는 일이 없음
    suspend fun markWatered(plantId: Long, timestamp: Long = System.currentTimeMillis()) {
        db.withTransaction {
            logDao.insert(WateringLog(plantId = plantId, wateredAt = timestamp))
            plantDao.updateLastWatered(plantId, timestamp)
        }
    }
}
