package com.solo.soloplayer.playback

sealed interface PlaybackEvent {
    data class Start(val media: PlayableMedia, val positionMs: Long) : PlaybackEvent
    data class Progress(val media: PlayableMedia, val positionMs: Long, val isPaused: Boolean) : PlaybackEvent
    data class Pause(val media: PlayableMedia, val positionMs: Long) : PlaybackEvent
    data class Resume(val media: PlayableMedia, val positionMs: Long) : PlaybackEvent
    data class Seek(val media: PlayableMedia, val positionMs: Long) : PlaybackEvent
    data class Stop(val media: PlayableMedia, val positionMs: Long) : PlaybackEvent
}
