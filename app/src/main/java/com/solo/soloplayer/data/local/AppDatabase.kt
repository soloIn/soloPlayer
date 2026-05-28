package com.solo.soloplayer.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.solo.soloplayer.data.local.dao.*
import com.solo.soloplayer.data.local.entity.*

@Database(
    entities = [
        MediaItemEntity::class,
        EmbyServerEntity::class,
        SmbAccountEntity::class,
        MovieEntity::class,
        ChapterEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun mediaItemDao(): MediaItemDao
    abstract fun embyServerDao(): EmbyServerDao
    abstract fun smbAccountDao(): SmbAccountDao
    abstract fun movieDao(): MovieDao
    abstract fun chapterDao(): ChapterDao
}
