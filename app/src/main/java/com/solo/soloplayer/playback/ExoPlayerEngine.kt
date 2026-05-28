package com.solo.soloplayer.playback

import android.content.Context
import android.view.SurfaceView
import android.view.TextureView
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.solo.soloplayer.domain.repository.EmbyServerRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ExoPlayerEngine(
    private val context: Context,
    private val embyServerRepository: EmbyServerRepository
) : PlaybackEngine {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var exoPlayer: ExoPlayer? = null
    
    private val _playbackState = MutableStateFlow<PlaybackState>(PlaybackState.Idle)
    override val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    private var videoSurfaceView: SurfaceView? = null
    private var videoTextureView: TextureView? = null
    private var currentMedia: PlayableMedia? = null

    override val currentPositionMs: Long
        get() = exoPlayer?.currentPosition ?: 0L

    override val durationMs: Long
        get() = exoPlayer?.duration ?: 0L

    @OptIn(UnstableApi::class)
    override fun initialize() {
        if (exoPlayer != null) return

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
            .build()

        exoPlayer = ExoPlayer.Builder(context)
            .setAudioAttributes(audioAttributes, true)
            .build()
            .apply {
                addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(state: Int) {
                        when (state) {
                            Player.STATE_BUFFERING -> _playbackState.value = PlaybackState.Buffering
                            Player.STATE_READY -> {
                                if (playWhenReady) {
                                    _playbackState.value = PlaybackState.Playing
                                } else {
                                    _playbackState.value = PlaybackState.Paused
                                }
                            }
                            Player.STATE_ENDED -> _playbackState.value = PlaybackState.Ended
                            Player.STATE_IDLE -> _playbackState.value = PlaybackState.Idle
                        }
                    }

                    override fun onPlayWhenReadyChanged(ready: Boolean, reason: Int) {
                        val player = exoPlayer ?: return
                        if (player.playbackState == Player.STATE_READY) {
                            _playbackState.value = if (ready) PlaybackState.Playing else PlaybackState.Paused
                        }
                    }

                    override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                        _playbackState.value = PlaybackState.Error(error.message ?: "ExoPlayer Error")
                    }
                })
            }

        // Attach surface or texture if already set
        videoSurfaceView?.let { exoPlayer?.setVideoSurfaceView(it) }
        videoTextureView?.let { exoPlayer?.setVideoTextureView(it) }
    }

    override fun play(media: PlayableMedia) {
        currentMedia = media
        _playbackState.value = PlaybackState.Buffering
        
        scope.launch {
            val resolvedUrl = if (media.path.startsWith("smb://", ignoreCase = true)) {
                resolveEmbyStreamUrl(media.id) ?: media.path
            } else {
                media.path
            }

            val player = exoPlayer ?: return@launch
            val mediaItem = MediaItem.fromUri(resolvedUrl)
            player.setMediaItem(mediaItem)
            
            val startPosMs = media.startPositionTicks / 10000L
            if (startPosMs > 0) {
                player.seekTo(startPosMs)
            }
            
            player.prepare()
            player.play()
        }
    }

    private suspend fun resolveEmbyStreamUrl(itemId: String): String? = withContext(Dispatchers.IO) {
        val server = embyServerRepository.getConnectedServer() ?: return@withContext null
        val serverUrl = server.serverUrl.trimEnd('/')
        val token = server.accessToken
        "$serverUrl/Videos/$itemId/stream?static=true&api_key=$token"
    }

    override fun pause() {
        exoPlayer?.pause()
    }

    override fun resume() {
        exoPlayer?.play()
    }

    override fun seekTo(positionMs: Long) {
        exoPlayer?.seekTo(positionMs)
    }

    override fun stop() {
        exoPlayer?.stop()
        _playbackState.value = PlaybackState.Idle
    }

    override fun release() {
        exoPlayer?.release()
        exoPlayer = null
        _playbackState.value = PlaybackState.Idle
    }

    override fun setVideoOutput(surfaceView: SurfaceView?, textureView: TextureView?) {
        this.videoSurfaceView = surfaceView
        this.videoTextureView = textureView
        val player = exoPlayer ?: return
        if (surfaceView != null) {
            player.setVideoSurfaceView(surfaceView)
        } else if (textureView != null) {
            player.setVideoTextureView(textureView)
        } else {
            player.clearVideoSurface()
        }
    }
}
