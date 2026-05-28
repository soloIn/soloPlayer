package com.solo.soloplayer.playback

import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.os.IBinder
import com.solo.soloplayer.data.remote.EmbyApi
import com.solo.soloplayer.data.remote.PlaybackProgressRequest
import com.solo.soloplayer.domain.repository.EmbyServerRepository
import com.solo.soloplayer.playback.data.local.OfflineDatabase
import com.solo.soloplayer.playback.data.local.OfflineProgressEntity
import com.solo.soloplayer.playback.data.worker.OfflineSyncWorker
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@AndroidEntryPoint
class PlaybackSyncService : Service() {

    @Inject
    lateinit var playbackManager: PlaybackManager

    @Inject
    lateinit var embyApi: EmbyApi

    @Inject
    lateinit var embyServerRepository: EmbyServerRepository

    @Inject
    lateinit var offlineDatabase: OfflineDatabase

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var eventJob: Job? = null
    private var heartbeatJob: Job? = null
    private var currentMedia: PlayableMedia? = null

    override fun onCreate() {
        super.onCreate()
        
        eventJob = serviceScope.launch {
            playbackManager.playbackEvents.collect { event ->
                when (event) {
                    is PlaybackEvent.Start -> {
                        currentMedia = event.media
                        reportProgress(event.media, event.positionMs, "START")
                        startHeartbeatTimer()
                    }
                    is PlaybackEvent.Resume -> {
                        currentMedia = event.media
                        reportProgress(event.media, event.positionMs, "RESUME")
                        startHeartbeatTimer()
                    }
                    is PlaybackEvent.Pause -> {
                        currentMedia = event.media
                        stopHeartbeatTimer()
                        reportProgress(event.media, event.positionMs, "PAUSE", isPaused = true)
                    }
                    is PlaybackEvent.Seek -> {
                        currentMedia = event.media
                        reportProgress(event.media, event.positionMs, "PROGRESS")
                    }
                    is PlaybackEvent.Stop -> {
                        currentMedia = null
                        stopHeartbeatTimer()
                        reportProgress(event.media, event.positionMs, "STOP")
                    }
                    is PlaybackEvent.Progress -> {
                        currentMedia = event.media
                        reportProgress(event.media, event.positionMs, "PROGRESS", isPaused = event.isPaused)
                    }
                }
            }
        }
    }

    private fun startHeartbeatTimer() {
        heartbeatJob?.cancel()
        heartbeatJob = serviceScope.launch {
            while (isActive) {
                delay(10000L) // 10 seconds heartbeat
                val media = currentMedia ?: break
                val engine = playbackManager.activeEngine.value ?: break
                
                reportProgress(media, engine.currentPositionMs, "PROGRESS")
            }
        }
    }

    private fun stopHeartbeatTimer() {
        heartbeatJob?.cancel()
        heartbeatJob = null
    }

    private suspend fun reportProgress(
        media: PlayableMedia,
        positionMs: Long,
        eventType: String,
        isPaused: Boolean = false
    ) {
        val server = embyServerRepository.getConnectedServer() ?: return
        val serverUrl = server.serverUrl.trimEnd('/')
        val token = server.accessToken
        val positionTicks = positionMs * 10000L

        val request = PlaybackProgressRequest(
            ItemId = media.id,
            MediaSourceId = media.id,
            PositionTicks = positionTicks,
            IsPaused = isPaused,
            IsMuted = false,
            VolumeLevel = 100,
            EventName = when (eventType) {
                "START" -> "PlaybackStart"
                "STOP" -> "PlaybackStop"
                "PAUSE" -> "Pause"
                "RESUME" -> "Unpause"
                else -> "TimeUpdate"
            }
        )

        if (!isNetworkConnected()) {
            saveToOfflineQueue(serverUrl, token, media.id, positionTicks, eventType)
            return
        }

        try {
            val response = when (eventType) {
                "START" -> {
                    val url = "$serverUrl/Sessions/Playing"
                    embyApi.reportPlaybackStart(url, token, request)
                }
                "STOP" -> {
                    val url = "$serverUrl/Sessions/Playing/Stopped"
                    embyApi.reportPlaybackStopped(url, token, request)
                }
                else -> {
                    val url = "$serverUrl/Sessions/Playing/Progress"
                    embyApi.reportPlaybackProgress(url, token, request)
                }
            }

            if (!response.isSuccessful) {
                saveToOfflineQueue(serverUrl, token, media.id, positionTicks, eventType)
            }
        } catch (e: java.io.IOException) {
            saveToOfflineQueue(serverUrl, token, media.id, positionTicks, eventType)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private suspend fun saveToOfflineQueue(
        serverUrl: String,
        token: String,
        itemId: String,
        positionTicks: Long,
        eventType: String
    ) = withContext(Dispatchers.IO) {
        val entity = OfflineProgressEntity(
            serverUrl = serverUrl,
            token = token,
            itemId = itemId,
            positionTicks = positionTicks,
            eventType = eventType,
            timestamp = System.currentTimeMillis()
        )
        offlineDatabase.offlineProgressDao().insert(entity)
        scheduleOfflineSync()
    }

    private fun scheduleOfflineSync() {
        val constraints = androidx.work.Constraints.Builder()
            .setRequiredNetworkType(androidx.work.NetworkType.CONNECTED)
            .build()

        val syncWorkRequest = androidx.work.OneTimeWorkRequestBuilder<OfflineSyncWorker>()
            .setConstraints(constraints)
            .build()

        androidx.work.WorkManager.getInstance(applicationContext)
            .enqueueUniqueWork(
                "OfflineSyncWorker",
                androidx.work.ExistingWorkPolicy.KEEP,
                syncWorkRequest
            )
    }

    private fun isNetworkConnected(): Boolean {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = cm.activeNetworkInfo
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        eventJob?.cancel()
        heartbeatJob?.cancel()
        super.onDestroy()
    }
}
