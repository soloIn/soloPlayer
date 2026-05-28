package com.solo.soloplayer.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.solo.soloplayer.data.local.entity.MediaItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MediaItemDao {
    @Query("SELECT * FROM media_items ORDER BY lastPlayedTime DESC")
    fun getAllMediaItems(): Flow<List<MediaItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertMediaItem(mediaItem: MediaItemEntity): Long

    @Query("DELETE FROM media_items WHERE id = :id")
    fun deleteMediaItem(id: String): Int
}
