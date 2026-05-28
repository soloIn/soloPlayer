package com.solo.soloplayer.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "movies")
data class MovieEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val originalTitle: String,
    val overview: String,
    val communityRating: Float,
    val productionYear: Int,
    val runTimeTicks: Long,
    val officialRating: String,
    val posterUrl: String,
    val backdropUrl: String,
    val videoType: String, // "ISO" or "MKV"
    val resolution: String, // "4K" or "1080p"
    val hdrType: String, // "HDR10", "Dolby Vision", "SDR"
    val audioFormat: String, // "Atmos", "TrueHD", "DTS-HD", "Unknown"
    val rawFilePath: String,
    val isWatched: Boolean,
    val resumeTicks: Long,
    val lastSyncTime: Long
)
