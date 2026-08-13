package com.moonkata.plantwater.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PlantDao {

    // 홈 화면 리스트용 — 최근 등록순
    @Query("SELECT * FROM plants ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<Plant>>

    // 캘린더/정보 화면 진입 시 단일 식물 관찰
    @Query("SELECT * FROM plants WHERE id = :plantId")
    fun observeById(plantId: Long): Flow<Plant?>

    @Insert
    suspend fun insert(plant: Plant): Long

    @Update
    suspend fun update(plant: Plant)

    @Delete
    suspend fun delete(plant: Plant)

    // 캘린더 화면의 삭제 버튼 — WateringLog는 외래키 CASCADE로 같이 정리됨
    @Query("DELETE FROM plants WHERE id = :plantId")
    suspend fun deleteById(plantId: Long)

    @Query("UPDATE plants SET lastWateredAt = :timestamp WHERE id = :plantId")
    suspend fun updateLastWatered(plantId: Long, timestamp: Long)

    // 5번(일정 수정) 화면에서 주기/알림시간만 바꿀 때
    @Query(
        """
        UPDATE plants
        SET wateringIntervalDays = :intervalDays, reminderHour = :hour, reminderMinute = :minute
        WHERE id = :plantId
        """
    )
    suspend fun updateSchedule(plantId: Long, intervalDays: Int, hour: Int, minute: Int)
}
