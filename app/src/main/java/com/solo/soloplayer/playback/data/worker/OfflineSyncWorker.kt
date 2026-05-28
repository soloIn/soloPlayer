package com.solo.soloplayer.playback.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.solo.soloplayer.data.remote.EmbyApi
import com.solo.soloplayer.data.remote.PlaybackProgressRequest
import com.solo.soloplayer.playback.data.local.OfflineDatabase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

@HiltWorker
class OfflineSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val embyApi: EmbyApi,
    private val offlineDatabase: OfflineDatabase
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val dao = offlineDatabase.offlineProgressDao()
        val allProgress = dao.getAll()

        if (allProgress.isEmpty()) {
            return@withContext Result.success()
        }

        for (progress in allProgress) {
            val request = PlaybackProgressRequest(
                ItemId = progress.itemId,
                MediaSourceId = progress.itemId,
                PositionTicks = progress.positionTicks,
                IsPaused = (progress.eventType == "PAUSE"),
                IsMuted = false,
                VolumeLevel = 100,
                EventName = when (progress.eventType) {
                    "START" -> "PlaybackStart"
                    "STOP" -> "PlaybackStop"
                    "PAUSE" -> "Pause"
                    "RESUME" -> "Unpause"
                    else -> "TimeUpdate"
                }
            )

            try {
                val response = when (progress.eventType) {
                    "START" -> {
                        val url = "${progress.serverUrl.trimEnd('/')}/Sessions/Playing"
                        embyApi.reportPlaybackStart(url, progress.token, request)
                    }
                    "STOP" -> {
                        val url = "${progress.serverUrl.trimEnd('/')}/Sessions/Playing/Stopped"
                        embyApi.reportPlaybackStopped(url, progress.token, request)
                    }
                    else -> {
                        val url = "${progress.serverUrl.trimEnd('/')}/Sessions/Playing/Progress"
                        embyApi.reportPlaybackProgress(url, progress.token, request)
                    }
                }

                if (response.isSuccessful) {
                    dao.delete(progress)
                } else {
                    return@withContext Result.retry()
                }
            } catch (e: IOException) {
                return@withContext Result.retry()
            } catch (e: Exception) {
                e.printStackTrace()
                return@withContext Result.retry()
            }
        }

        return@withContext Result.success()
    }
}
