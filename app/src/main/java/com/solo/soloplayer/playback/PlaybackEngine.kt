package com.solo.soloplayer.playback

import android.view.SurfaceView
import android.view.TextureView
import kotlinx.coroutines.flow.StateFlow

interface PlaybackEngine {
    val playbackState: StateFlow<PlaybackState>
    val currentPositionMs: Long
    val durationMs: Long

    fun initialize()
    fun play(media: PlayableMedia)
    fun pause()
    fun resume()
    fun seekTo(positionMs: Long)
    fun stop()
    fun release()
    fun setVideoOutput(surfaceView: SurfaceView?, textureView: TextureView? = null)
}
