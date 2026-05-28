package com.solo.soloplayer.playback

import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

@Singleton
class PlaybackDispatcher @Inject constructor(
    private val exoPlayerEngine: Provider<ExoPlayerEngine>,
    private val vlcPlayerEngine: Provider<VlcPlayerEngine>
) {
    fun getEngine(media: PlayableMedia): PlaybackEngine {
        val isIso = media.mediaType?.equals("ISO", ignoreCase = true) == true ||
                media.path.endsWith(".iso", ignoreCase = true)
        return if (isIso) {
            vlcPlayerEngine.get()
        } else {
            exoPlayerEngine.get()
        }
    }
}
