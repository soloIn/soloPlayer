package com.solo.soloplayer.data.local.dao

import androidx.room.*
import com.solo.soloplayer.data.local.entity.MovieEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MovieDao {
    @Query("SELECT * FROM movies ORDER BY title ASC")
    fun getAllMovies(): Flow<List<MovieEntity>>

    @Query("SELECT * FROM movies WHERE id = :id")
    fun getMovieById(id: String): MovieEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertMovie(movie: MovieEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertMovies(movies: List<MovieEntity>): List<Long>

    @Query("DELETE FROM movies WHERE id = :id")
    fun deleteMovieById(id: String): Int

    @Query("DELETE FROM movies")
    fun deleteAllMovies(): Int

    @Query("SELECT MAX(lastSyncTime) FROM movies")
    fun getMaxLastSyncTime(): Long?
}
