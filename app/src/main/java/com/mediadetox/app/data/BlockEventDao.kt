package com.mediadetox.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface BlockEventDao {

    @Insert
    suspend fun insert(event: BlockEvent)

    @Query("SELECT * FROM block_events WHERE timestamp >= :startOfDay ORDER BY timestamp DESC")
    suspend fun getEventsToday(startOfDay: Long): List<BlockEvent>

    @Query("SELECT * FROM block_events WHERE timestamp >= :startOfWeek ORDER BY timestamp DESC")
    suspend fun getEventsThisWeek(startOfWeek: Long): List<BlockEvent>

    @Query("SELECT COUNT(*) FROM block_events WHERE eventType = 'BLOCKED' AND timestamp >= :startOfDay")
    suspend fun countBlockedToday(startOfDay: Long): Int

    @Query("SELECT COUNT(*) FROM block_events WHERE eventType = 'CAVED' AND timestamp >= :startOfDay")
    suspend fun countCavedToday(startOfDay: Long): Int
}
