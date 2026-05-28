package com.solo.soloplayer.playback.di

import android.content.Context
import androidx.room.Room
import com.solo.soloplayer.domain.repository.EmbyServerRepository
import com.solo.soloplayer.domain.repository.SmbAccountRepository
import com.solo.soloplayer.playback.ExoPlayerEngine
import com.solo.soloplayer.playback.VlcPlayerEngine
import com.solo.soloplayer.playback.data.local.OfflineDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object PlaybackModule {

    @Provides
    @Singleton
    fun provideOfflineDatabase(
        @ApplicationContext context: Context
    ): OfflineDatabase {
        return Room.databaseBuilder(
            context,
            OfflineDatabase::class.java,
            "offline_progress_database"
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    fun provideExoPlayerEngine(
        @ApplicationContext context: Context,
        embyServerRepository: EmbyServerRepository
    ): ExoPlayerEngine {
        return ExoPlayerEngine(context, embyServerRepository)
    }

    @Provides
    fun provideVlcPlayerEngine(
        @ApplicationContext context: Context,
        smbAccountRepository: SmbAccountRepository
    ): VlcPlayerEngine {
        return VlcPlayerEngine(context, smbAccountRepository)
    }
}
