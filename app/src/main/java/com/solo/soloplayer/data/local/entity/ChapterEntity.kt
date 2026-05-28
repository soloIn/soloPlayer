package com.solo.soloplayer.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "chapters",
    foreignKeys = [
        ForeignKey(
            entity = MovieEntity::class,
            parentColumns = ["id"],
            childColumns = ["movieId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["movieId"])]
)
data class ChapterEntity(
    @PrimaryKey
    val id: String,
    val movieId: String,
    val name: String,
    val startPositionTicks: Long,
    val thumbnailUrl: String
)
