package com.solo.soloplayer.di

import android.content.Context
import androidx.room.Room
import com.solo.soloplayer.data.local.AppDatabase
import com.solo.soloplayer.data.local.dao.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "soloplayer_database"
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    @Singleton
    fun provideMediaItemDao(database: AppDatabase): MediaItemDao {
        return database.mediaItemDao()
    }

    @Provides
    @Singleton
    fun provideEmbyServerDao(database: AppDatabase): EmbyServerDao {
        return database.embyServerDao()
    }

    @Provides
    @Singleton
    fun provideSmbAccountDao(database: AppDatabase): SmbAccountDao {
        return database.smbAccountDao()
    }

    @Provides
    @Singleton
    fun provideMovieDao(database: AppDatabase): MovieDao {
        return database.movieDao()
    }

    @Provides
    @Singleton
    fun provideChapterDao(database: AppDatabase): ChapterDao {
        return database.chapterDao()
    }
}
