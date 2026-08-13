package com.moonkata.plantwater.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface WateringLogDao {

    // 캘린더 화면 - 해당 식물의 전체 기록, 최신순
    @Query("SELECT * FROM watering_logs WHERE plantId = :plantId ORDER BY wateredAt DESC")
    fun observeByPlant(plantId: Long): Flow<List<WateringLog>>

    // 캘린더 상단 "총 N번" 카운트
    @Query("SELECT COUNT(*) FROM watering_logs WHERE plantId = :plantId")
    fun observeCountByPlant(plantId: Long): Flow<Int>

    @Insert
    suspend fun insert(log: WateringLog): Long
}
