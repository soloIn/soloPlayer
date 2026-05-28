package com.solo.soloplayer

import com.solo.soloplayer.playback.ExoPlayerEngine
import com.solo.soloplayer.playback.PlayableMedia
import com.solo.soloplayer.playback.PlaybackDispatcher
import com.solo.soloplayer.playback.PlaybackEvent
import com.solo.soloplayer.playback.PlaybackManager
import com.solo.soloplayer.playback.VlcPlayerEngine
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import javax.inject.Provider

@OptIn(ExperimentalCoroutinesApi::class)
class PlaybackTest {

    @Before
    fun setUp() {
        Dispatchers.setMain(Dispatchers.Unconfined)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testTicksConversions() {
        val ms = 1500L
        val expectedTicks = 15000000L
        
        // 1 ms = 10,000 ticks
        val actualTicks = ms * 10000L
        assertEquals(expectedTicks, actualTicks)

        val actualMs = expectedTicks / 10000L
        assertEquals(ms, actualMs)
    }

    @Test
    fun testPlaybackDispatcherRouting_Iso() {
        val mockExoEngine = mockk<ExoPlayerEngine>()
        val mockVlcEngine = mockk<VlcPlayerEngine>()

        val exoProvider = Provider { mockExoEngine }
        val vlcProvider = Provider { mockVlcEngine }

        val dispatcher = PlaybackDispatcher(exoProvider, vlcProvider)

        val isoMedia = PlayableMedia(
            id = "1",
            title = "Test ISO Movie",
            path = "smb://192.168.1.100/Share/movie.iso",
            mediaType = "ISO"
        )

        val engine = dispatcher.getEngine(isoMedia)
        assertEquals(mockVlcEngine, engine)
    }

    @Test
    fun testPlaybackDispatcherRouting_Mkv() {
        val mockExoEngine = mockk<ExoPlayerEngine>()
        val mockVlcEngine = mockk<VlcPlayerEngine>()

        val exoProvider = Provider { mockExoEngine }
        val vlcProvider = Provider { mockVlcEngine }

        val dispatcher = PlaybackDispatcher(exoProvider, vlcProvider)

        val mkvMedia = PlayableMedia(
            id = "2",
            title = "Test MKV Movie",
            path = "smb://192.168.1.100/Share/movie.mkv",
            mediaType = "MKV"
        )

        val engine = dispatcher.getEngine(mkvMedia)
        assertEquals(mockExoEngine, engine)
    }

    @Test
    fun testPlaybackDispatcherRouting_PathSuffixIso() {
        val mockExoEngine = mockk<ExoPlayerEngine>()
        val mockVlcEngine = mockk<VlcPlayerEngine>()

        val exoProvider = Provider { mockExoEngine }
        val vlcProvider = Provider { mockVlcEngine }

        val dispatcher = PlaybackDispatcher(exoProvider, vlcProvider)

        val media = PlayableMedia(
            id = "3",
            title = "Test Suffix Movie",
            path = "smb://192.168.1.100/Share/movie.iso",
            mediaType = "VIDEO"
        )

        val engine = dispatcher.getEngine(media)
        assertEquals(mockVlcEngine, engine)
    }

    @Test
    fun testPlaybackManagerEvents() {
        val mockExoEngine = mockk<ExoPlayerEngine>(relaxed = true)
        val mockVlcEngine = mockk<VlcPlayerEngine>(relaxed = true)

        val exoProvider = Provider { mockExoEngine }
        val vlcProvider = Provider { mockVlcEngine }

        val dispatcher = PlaybackDispatcher(exoProvider, vlcProvider)
        val manager = PlaybackManager(dispatcher)

        val media = PlayableMedia(
            id = "4",
            title = "Test Movie",
            path = "smb://192.168.1.100/Share/movie.mkv",
            mediaType = "MKV"
        )

        val events = mutableListOf<PlaybackEvent>()
        val job = GlobalScope.launch(Dispatchers.Unconfined) {
            manager.playbackEvents.collect { events.add(it) }
        }

        manager.play(media)
        manager.pause()
        manager.resume()
        manager.seekTo(3000L)
        manager.stop()

        assertEquals(5, events.size)
        assertTrue(events[0] is PlaybackEvent.Start)
        assertTrue(events[1] is PlaybackEvent.Pause)
        assertTrue(events[2] is PlaybackEvent.Resume)
        assertTrue(events[3] is PlaybackEvent.Seek)
        assertTrue(events[4] is PlaybackEvent.Stop)

        job.cancel()
    }
}
