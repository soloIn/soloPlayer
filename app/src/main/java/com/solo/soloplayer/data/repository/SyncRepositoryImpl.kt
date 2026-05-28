package com.solo.soloplayer.data.repository

import com.solo.soloplayer.data.local.AppDatabase
import com.solo.soloplayer.data.local.entity.ChapterEntity
import com.solo.soloplayer.data.local.entity.MovieEntity
import com.solo.soloplayer.data.remote.EmbyApi
import com.solo.soloplayer.data.remote.EmbyItemDto
import com.solo.soloplayer.di.IoDispatcher
import com.solo.soloplayer.domain.repository.SyncRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncRepositoryImpl @Inject constructor(
    private val database: AppDatabase,
    private val embyApi: EmbyApi,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : SyncRepository {

    private val _isSyncing = MutableStateFlow(false)

    override fun isSyncing(): Flow<Boolean> {
        return _isSyncing.asStateFlow()
    }

    override suspend fun getLastSyncTime(): Long = withContext(ioDispatcher) {
        database.movieDao().getMaxLastSyncTime() ?: 0L
    }

    override suspend fun syncMovies(
        serverUrl: String,
        userId: String,
        token: String
    ): Result<Unit> = withContext(ioDispatcher) {
        if (_isSyncing.value) {
            return@withContext Result.failure(IllegalStateException("Sync already in progress"))
        }
        _isSyncing.value = true
        try {
            val syncStartTime = System.currentTimeMillis()
            val lastSyncTime = getLastSyncTime()

            // 1. Get all library views
            val viewsUrl = "${serverUrl.trimEnd('/')}/Users/$userId/Views"
            val viewsResponse = embyApi.getLibraryViews(viewsUrl, token)
            val movieLibraries = viewsResponse.Items.filter {
                it.CollectionType?.equals("movies", ignoreCase = true) == true
            }

            // 2. Fetch movies recursively for each movie library view
            val allMovies = mutableListOf<EmbyItemDto>()
            val itemsUrl = "${serverUrl.trimEnd('/')}/Items"

            for (library in movieLibraries) {
                val queryMap = mutableMapOf<String, String>().apply {
                    put("ParentId", library.Id)
                    put("Recursive", "true")
                    put("IncludeItemTypes", "Movie")
                    put("Fields", "Path,MediaSources,MediaStreams,Overview,CommunityRating,OfficialRating,ProductionYear,OriginalTitle,UserData")
                    if (lastSyncTime > 0) {
                        val formatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
                            timeZone = TimeZone.getTimeZone("UTC")
                        }
                        put("MinDateLastSaved", formatter.format(Date(lastSyncTime)))
                    }
                }
                val itemsResponse = embyApi.getItems(itemsUrl, token, queryMap)
                allMovies.addAll(itemsResponse.Items)
            }

            // 3. For each movie, fetch chapters and map to local entities
            val movieEntities = mutableListOf<MovieEntity>()
            val chapterEntities = mutableListOf<ChapterEntity>()

            for (item in allMovies) {
                val movieEntity = com.solo.soloplayer.data.mapper.MediaParser.toMovieEntity(
                    item = item,
                    serverUrl = serverUrl,
                    token = token,
                    syncStartTime = syncStartTime
                )
                movieEntities.add(movieEntity)

                // Fetch chapters for the movie
                try {
                    val playbackInfoUrl = "${serverUrl.trimEnd('/')}/Items/${item.Id}/PlaybackInfo"
                    val playbackInfo = embyApi.getPlaybackInfo(playbackInfoUrl, token, userId)
                    val chapters = playbackInfo.MediaSources?.firstOrNull()?.Chapters ?: emptyList()
                    
                    chapters.forEachIndexed { index, chapterDto ->
                        val chapterImageUrl = "${serverUrl.trimEnd('/')}/Items/${item.Id}/Images/Chapter/$index?api_key=$token"
                        chapterEntities.add(
                            ChapterEntity(
                                id = "${item.Id}_chapter_$index",
                                movieId = item.Id,
                                name = chapterDto.Name ?: "Chapter ${index + 1}",
                                startPositionTicks = chapterDto.StartPositionTicks,
                                thumbnailUrl = chapterImageUrl
                            )
                        )
                    }
                } catch (e: Exception) {
                    // Log or handle single movie chapter sync failure, do not fail entire sync
                    e.printStackTrace()
                }
            }

            // 4. Save movies and chapters in a Room database transaction
            database.runInTransaction {
                if (movieEntities.isNotEmpty()) {
                    database.movieDao().insertMovies(movieEntities)
                }
                for (movie in movieEntities) {
                    database.chapterDao().deleteChaptersForMovie(movie.id)
                }
                if (chapterEntities.isNotEmpty()) {
                    database.chapterDao().insertChapters(chapterEntities)
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        } finally {
            _isSyncing.value = false
        }
    }
}
