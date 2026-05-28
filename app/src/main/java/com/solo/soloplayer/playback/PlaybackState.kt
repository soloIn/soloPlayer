package com.solo.soloplayer.playback

sealed interface PlaybackState {
    object Idle : PlaybackState
    object Buffering : PlaybackState
    object Playing : PlaybackState
    object Paused : PlaybackState
    object Ended : PlaybackState
    data class Error(val message: String) : PlaybackState
}
