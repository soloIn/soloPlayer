package com.solo.soloplayer

import com.solo.soloplayer.data.local.AppDatabase
import com.solo.soloplayer.data.local.dao.ChapterDao
import com.solo.soloplayer.data.local.dao.MovieDao
import com.solo.soloplayer.data.local.entity.ChapterEntity
import com.solo.soloplayer.data.local.entity.MovieEntity
import com.solo.soloplayer.data.remote.ChapterDto
import com.solo.soloplayer.data.remote.EmbyApi
import com.solo.soloplayer.data.remote.EmbyItemDto
import com.solo.soloplayer.data.remote.ItemsResponse
import com.solo.soloplayer.data.remote.LibraryViewDto
import com.solo.soloplayer.data.remote.MediaSourcePlaybackDto
import com.solo.soloplayer.data.remote.PlaybackInfoResponse
import com.solo.soloplayer.data.remote.ViewsResponse
import com.solo.soloplayer.data.repository.SyncRepositoryImpl
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalCoroutinesApi::class)
class SyncRepositoryTest {

    private lateinit var database: AppDatabase
    private lateinit var movieDao: MovieDao
    private lateinit var chapterDao: ChapterDao
    private lateinit var embyApi: EmbyApi
    private lateinit var syncRepository: SyncRepositoryImpl
    private val testDispatcher = UnconfinedTestDispatcher()

    private val serverUrl = "http://192.168.1.100:8096"
    private val userId = "user123"
    private val token = "token123"

    @Before
    fun setUp() {
        database = mockk(relaxed = true)
        movieDao = mockk(relaxed = true)
        chapterDao = mockk(relaxed = true)
        embyApi = mockk(relaxed = true)

        every { database.movieDao() } returns movieDao
        every { database.chapterDao() } returns chapterDao

        // Mock database transaction to execute Runnable immediately
        every { database.runInTransaction(any<Runnable>()) } answers {
            val runnable = firstArg<Runnable>()
            runnable.run()
        }

        syncRepository = SyncRepositoryImpl(
            database = database,
            embyApi = embyApi,
            ioDispatcher = testDispatcher
        )
    }

    @Test
    fun testInitialSync_noMinDateLastSaved() = runTest(testDispatcher) {
        // Setup database mock for first-time sync
        every { movieDao.getMaxLastSyncTime() } returns null

        // Setup API mock responses
        val libraryViews = ViewsResponse(
            Items = listOf(
                LibraryViewDto(Id = "lib1", Name = "Movies Library", CollectionType = "movies")
            )
        )
        coEvery { embyApi.getLibraryViews(any(), token) } returns libraryViews

        val itemsResponse = ItemsResponse(
            Items = listOf(
                EmbyItemDto(
                    Id = "movie1",
                    Name = "Test Movie",
                    OriginalTitle = null,
                    Overview = null,
                    CommunityRating = null,
                    ProductionYear = null,
                    RunTimeTicks = null,
                    OfficialRating = null,
                    Path = "/path/to/movie.mkv",
                    UserData = null,
                    MediaSources = null,
                    MediaStreams = null
                )
            )
        )

        val queryMapSlot = slot<Map<String, String>>()
        coEvery { embyApi.getItems(any(), token, capture(queryMapSlot)) } returns itemsResponse

        val playbackInfoResponse = PlaybackInfoResponse(
            MediaSources = listOf(
                MediaSourcePlaybackDto(
                    Id = "source1",
                    Chapters = listOf(
                        ChapterDto(Name = "Chapter 1", StartPositionTicks = 0, ImageTag = null)
                    )
                )
            )
        )
        coEvery { embyApi.getPlaybackInfo(any(), token, userId) } returns playbackInfoResponse

        // Run sync
        val result = syncRepository.syncMovies(serverUrl, userId, token)

        assertTrue(result.isSuccess)

        // Verify query maps did not contain MinDateLastSaved
        val capturedQuery = queryMapSlot.captured
        assertEquals("lib1", capturedQuery["ParentId"])
        assertEquals("true", capturedQuery["Recursive"])
        assertEquals("Movie", capturedQuery["IncludeItemTypes"])
        assertTrue(!capturedQuery.containsKey("MinDateLastSaved"))

        // Verify saving movies and chapters
        val moviesSlot = slot<List<MovieEntity>>()
        verify { movieDao.insertMovies(capture(moviesSlot)) }
        assertEquals(1, moviesSlot.captured.size)
        assertEquals("movie1", moviesSlot.captured[0].id)

        val chaptersSlot = slot<List<ChapterEntity>>()
        verify { chapterDao.insertChapters(capture(chaptersSlot)) }
        assertEquals(1, chaptersSlot.captured.size)
        assertEquals("movie1_chapter_0", chaptersSlot.captured[0].id)
    }

    @Test
    fun testIncrementalSync_usesMinDateLastSaved() = runTest(testDispatcher) {
        val lastSyncTime = 1716912000000L // 2024-05-28 16:00:00 UTC
        every { movieDao.getMaxLastSyncTime() } returns lastSyncTime

        // Setup API mock responses
        val libraryViews = ViewsResponse(
            Items = listOf(
                LibraryViewDto(Id = "lib1", Name = "Movies Library", CollectionType = "movies")
            )
        )
        coEvery { embyApi.getLibraryViews(any(), token) } returns libraryViews

        val itemsResponse = ItemsResponse(
            Items = listOf(
                EmbyItemDto(
                    Id = "movie2",
                    Name = "New Movie",
                    OriginalTitle = null,
                    Overview = null,
                    CommunityRating = null,
                    ProductionYear = null,
                    RunTimeTicks = null,
                    OfficialRating = null,
                    Path = "/path/to/new_movie.mkv",
                    UserData = null,
                    MediaSources = null,
                    MediaStreams = null
                )
            )
        )

        val queryMapSlot = slot<Map<String, String>>()
        coEvery { embyApi.getItems(any(), token, capture(queryMapSlot)) } returns itemsResponse

        val playbackInfoResponse = PlaybackInfoResponse(
            MediaSources = emptyList()
        )
        coEvery { embyApi.getPlaybackInfo(any(), token, userId) } returns playbackInfoResponse

        // Run sync
        val result = syncRepository.syncMovies(serverUrl, userId, token)

        assertTrue(result.isSuccess)

        // Verify query maps did contain MinDateLastSaved formatted correctly
        val capturedQuery = queryMapSlot.captured
        assertEquals("lib1", capturedQuery["ParentId"])
        assertTrue(capturedQuery.containsKey("MinDateLastSaved"))

        val formatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val expectedDateStr = formatter.format(Date(lastSyncTime))
        assertEquals(expectedDateStr, capturedQuery["MinDateLastSaved"])

        // Verify saving movies
        verify { movieDao.insertMovies(any()) }
    }
}
