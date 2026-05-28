package com.solo.soloplayer.playback

import android.view.SurfaceView
import android.view.TextureView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaybackManager @Inject constructor(
    private val dispatcher: PlaybackDispatcher
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _currentMedia = MutableStateFlow<PlayableMedia?>(null)
    val currentMedia: StateFlow<PlayableMedia?> = _currentMedia.asStateFlow()

    private val _activeEngine = MutableStateFlow<PlaybackEngine?>(null)
    val activeEngine: StateFlow<PlaybackEngine?> = _activeEngine.asStateFlow()

    private val _playbackEvents = MutableSharedFlow<PlaybackEvent>(extraBufferCapacity = 64)
    val playbackEvents: SharedFlow<PlaybackEvent> = _playbackEvents.asSharedFlow()

    private var videoSurfaceView: SurfaceView? = null
    private var videoTextureView: TextureView? = null

    init {
        scope.launch {
            _activeEngine.collectLatest { engine ->
                engine?.playbackState?.collect { state ->
                    val media = _currentMedia.value ?: return@collect
                    when (state) {
                        PlaybackState.Ended -> {
                            stop()
                        }
                        is PlaybackState.Error -> {
                            _playbackEvents.emit(PlaybackEvent.Stop(media, engine.currentPositionMs))
                            stop()
                        }
                        else -> {}
                    }
                }
            }
        }
    }

    fun play(media: PlayableMedia) {
        _activeEngine.value?.stop()
        _activeEngine.value?.release()

        _currentMedia.value = media
        val engine = dispatcher.getEngine(media)
        engine.initialize()

        engine.setVideoOutput(videoSurfaceView, videoTextureView)

        _activeEngine.value = engine
        engine.play(media)

        _playbackEvents.tryEmit(PlaybackEvent.Start(media, engine.currentPositionMs))
    }

    fun pause() {
        val media = _currentMedia.value ?: return
        val engine = _activeEngine.value ?: return
        engine.pause()
        _playbackEvents.tryEmit(PlaybackEvent.Pause(media, engine.currentPositionMs))
    }

    fun resume() {
        val media = _currentMedia.value ?: return
        val engine = _activeEngine.value ?: return
        engine.resume()
        _playbackEvents.tryEmit(PlaybackEvent.Resume(media, engine.currentPositionMs))
    }

    fun seekTo(positionMs: Long) {
        val media = _currentMedia.value ?: return
        val engine = _activeEngine.value ?: return
        engine.seekTo(positionMs)
        _playbackEvents.tryEmit(PlaybackEvent.Seek(media, positionMs))
    }

    fun stop() {
        val media = _currentMedia.value ?: return
        val engine = _activeEngine.value ?: return
        _playbackEvents.tryEmit(PlaybackEvent.Stop(media, engine.currentPositionMs))
        engine.stop()
        engine.release()
        _activeEngine.value = null
        _currentMedia.value = null
    }

    fun release() {
        _activeEngine.value?.release()
        _activeEngine.value = null
        _currentMedia.value = null
    }

    fun setVideoOutput(surfaceView: SurfaceView?, textureView: TextureView?) {
        this.videoSurfaceView = surfaceView
        this.videoTextureView = textureView
        _activeEngine.value?.setVideoOutput(surfaceView, textureView)
    }
}
