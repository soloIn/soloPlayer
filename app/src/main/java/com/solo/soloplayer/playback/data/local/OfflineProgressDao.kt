package com.solo.soloplayer.playback.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Delete

@Dao
interface OfflineProgressDao {
    @Insert
    fun insert(progress: OfflineProgressEntity): Long

    @Query("SELECT * FROM offline_progress ORDER BY timestamp ASC")
    fun getAll(): List<OfflineProgressEntity>

    @Delete
    fun delete(progress: OfflineProgressEntity): Int

    @Query("DELETE FROM offline_progress WHERE id = :id")
    fun deleteById(id: Long): Int
}
