package com.solo.soloplayer.playback.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "offline_progress")
data class OfflineProgressEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val serverUrl: String,
    val token: String,
    val itemId: String,
    val positionTicks: Long,
    val eventType: String, // "START", "PROGRESS", "STOP"
    val timestamp: Long
)
