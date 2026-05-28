package com.solo.soloplayer.playback

data class PlayableMedia(
    val id: String, // Emby Item Id
    val title: String,
    val path: String, // SMB or local or HTTP path
    val startPositionTicks: Long = 0L,
    val mediaType: String? = null // ISO, MKV, MP4, etc.
)
