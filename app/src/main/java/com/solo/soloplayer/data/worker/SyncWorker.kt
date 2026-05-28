package com.solo.soloplayer.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.solo.soloplayer.domain.repository.EmbyServerRepository
import com.solo.soloplayer.domain.repository.SyncRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val embyServerRepository: EmbyServerRepository,
    private val syncRepository: SyncRepository
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val connectedServer = embyServerRepository.getConnectedServer()
            ?: return Result.failure()

        val result = syncRepository.syncMovies(
            serverUrl = connectedServer.serverUrl,
            userId = connectedServer.userId,
            token = connectedServer.accessToken
        )

        return if (result.isSuccess) {
            Result.success()
        } else {
            Result.retry()
        }
    }
}
