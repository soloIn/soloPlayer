package com.solo.soloplayer.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.solo.soloplayer.data.local.entity.EmbyServerEntity
import com.solo.soloplayer.data.local.entity.MediaItemEntity
import com.solo.soloplayer.data.local.entity.MovieEntity
import com.solo.soloplayer.data.local.entity.SmbAccountEntity
import com.solo.soloplayer.domain.repository.EmbyServerRepository
import com.solo.soloplayer.domain.repository.SmbAccountRepository
import com.solo.soloplayer.domain.repository.SyncRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val movieDao: com.solo.soloplayer.data.local.dao.MovieDao,
    private val mediaItemDao: com.solo.soloplayer.data.local.dao.MediaItemDao,
    private val embyServerRepository: EmbyServerRepository,
    private val smbAccountRepository: SmbAccountRepository,
    private val syncRepository: SyncRepository
) : ViewModel() {

    // Movies from database
    val movies: StateFlow<List<MovieEntity>> = movieDao.getAllMovies()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Media items for Continue Watching
    val continueWatching: StateFlow<List<MediaItemEntity>> = mediaItemDao.getAllMediaItems()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Syncing state
    val isSyncing: StateFlow<Boolean> = syncRepository.isSyncing()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    // Emby Servers
    val embyServers: StateFlow<List<EmbyServerEntity>> = embyServerRepository.getAllServers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // SMB Accounts/Paths
    val smbAccounts: StateFlow<List<SmbAccountEntity>> = smbAccountRepository.getAllAccounts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Last Sync Time State
    private val _lastSyncTime = MutableStateFlow(0L)
    val lastSyncTime: StateFlow<Long> = _lastSyncTime.asStateFlow()

    // Sync Interval in Minutes (default: 60)
    private val _syncInterval = MutableStateFlow(60)
    val syncInterval: StateFlow<Int> = _syncInterval.asStateFlow()

    init {
        refreshLastSyncTime()
    }

    fun refreshLastSyncTime() {
        viewModelScope.launch {
            _lastSyncTime.value = syncRepository.getLastSyncTime()
        }
    }

    fun setSyncInterval(minutes: Int) {
        _syncInterval.value = minutes
    }

    fun triggerManualSync() {
        viewModelScope.launch {
            val server = embyServerRepository.getConnectedServer()
            if (server != null) {
                syncRepository.syncMovies(
                    serverUrl = server.serverUrl,
                    userId = server.userId,
                    token = server.accessToken
                )
                refreshLastSyncTime()
            } else {
                // If no server connected, sync with a dummy or log error
            }
        }
    }

    fun addSmbFolder(path: String, name: String = "SMB Share") {
        viewModelScope.launch {
            val account = SmbAccountEntity(
                id = java.util.UUID.randomUUID().toString(),
                serverAddress = path,
                shareName = name,
                username = "",
                password = "",
                domain = null
            )
            smbAccountRepository.saveAccount(account)
        }
    }

    fun removeSmbFolder(id: String) {
        viewModelScope.launch {
            smbAccountRepository.deleteAccount(id)
        }
    }

    fun addEmbyServer(url: String, name: String) {
        viewModelScope.launch {
            val server = EmbyServerEntity(
                id = java.util.UUID.randomUUID().toString(),
                serverName = name,
                serverUrl = url,
                accessToken = "dummy_token",
                userId = "dummy_user",
                isConnected = false
            )
            embyServerRepository.saveServer(server)
        }
    }

    fun connectEmbyServer(id: String) {
        viewModelScope.launch {
            embyServerRepository.connectServer(id)
        }
    }

    fun removeEmbyServer(id: String) {
        viewModelScope.launch {
            embyServerRepository.deleteServer(id)
        }
    }
}
