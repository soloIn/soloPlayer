package com.solo.soloplayer.domain.repository

import kotlinx.coroutines.flow.Flow

interface SyncRepository {
    fun isSyncing(): Flow<Boolean>
    suspend fun syncMovies(serverUrl: String, userId: String, token: String): Result<Unit>
    suspend fun getLastSyncTime(): Long
}
