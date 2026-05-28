package com.solo.soloplayer.di

import com.solo.soloplayer.data.repository.EmbyServerRepositoryImpl
import com.solo.soloplayer.data.repository.SmbAccountRepositoryImpl
import com.solo.soloplayer.data.repository.SyncRepositoryImpl
import com.solo.soloplayer.domain.repository.EmbyServerRepository
import com.solo.soloplayer.domain.repository.SmbAccountRepository
import com.solo.soloplayer.domain.repository.SyncRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindEmbyServerRepository(
        impl: EmbyServerRepositoryImpl
    ): EmbyServerRepository

    @Binds
    @Singleton
    abstract fun bindSmbAccountRepository(
        impl: SmbAccountRepositoryImpl
    ): SmbAccountRepository

    @Binds
    @Singleton
    abstract fun bindSyncRepository(
        impl: SyncRepositoryImpl
    ): SyncRepository
}
