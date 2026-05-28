package com.solo.soloplayer.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "media_items")
data class MediaItemEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val url: String,
    val mediaType: String, // e.g., VIDEO, AUDIO, ISO
    val duration: Long = 0L,
    val lastPlaybackPosition: Long = 0L,
    val lastPlayedTime: Long = 0L
)
