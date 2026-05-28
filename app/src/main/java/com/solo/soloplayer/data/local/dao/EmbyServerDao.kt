package com.solo.soloplayer.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.solo.soloplayer.data.local.entity.EmbyServerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EmbyServerDao {
    @Query("SELECT * FROM emby_servers")
    fun getAllServers(): Flow<List<EmbyServerEntity>>

    @Query("SELECT * FROM emby_servers WHERE isConnected = 1 LIMIT 1")
    fun getConnectedServer(): EmbyServerEntity?

    @Query("SELECT * FROM emby_servers WHERE id = :id")
    fun getServerById(id: String): EmbyServerEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertServer(server: EmbyServerEntity): Long

    @Query("DELETE FROM emby_servers WHERE id = :id")
    fun deleteServerById(id: String): Int

    @Query("UPDATE emby_servers SET isConnected = 0")
    fun disconnectAll(): Int

    @Query("UPDATE emby_servers SET isConnected = 1 WHERE id = :id")
    fun setConnected(id: String): Int
}
