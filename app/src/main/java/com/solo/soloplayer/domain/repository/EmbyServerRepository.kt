package com.solo.soloplayer.domain.repository

import com.solo.soloplayer.data.local.entity.EmbyServerEntity
import com.solo.soloplayer.data.remote.AuthenticateByNameResponse
import com.solo.soloplayer.domain.model.DiscoveredServer
import kotlinx.coroutines.flow.Flow

interface EmbyServerRepository {
    fun getAllServers(): Flow<List<EmbyServerEntity>>
    suspend fun getConnectedServer(): EmbyServerEntity?
    suspend fun getServerById(id: String): EmbyServerEntity?
    suspend fun saveServer(server: EmbyServerEntity)
    suspend fun deleteServer(id: String)
    suspend fun connectServer(id: String)
    suspend fun disconnectAll()
    suspend fun discoverServers(): List<DiscoveredServer>
    suspend fun validateCredentials(url: String, username: String, pw: String): AuthenticateByNameResponse
}
