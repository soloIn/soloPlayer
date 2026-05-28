package com.solo.soloplayer.data.local.dao

import androidx.room.*
import com.solo.soloplayer.data.local.entity.ChapterEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChapterDao {
    @Query("SELECT * FROM chapters WHERE movieId = :movieId ORDER BY startPositionTicks ASC")
    fun getChaptersForMovie(movieId: String): Flow<List<ChapterEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertChapters(chapters: List<ChapterEntity>): List<Long>

    @Query("DELETE FROM chapters WHERE movieId = :movieId")
    fun deleteChaptersForMovie(movieId: String): Int
}
