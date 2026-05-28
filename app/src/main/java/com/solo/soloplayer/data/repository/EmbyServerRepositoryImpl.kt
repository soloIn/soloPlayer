package com.solo.soloplayer.data.repository

import com.google.gson.Gson
import com.solo.soloplayer.data.local.dao.EmbyServerDao
import com.solo.soloplayer.data.local.entity.EmbyServerEntity
import com.solo.soloplayer.data.remote.AuthenticateByNameRequest
import com.solo.soloplayer.data.remote.AuthenticateByNameResponse
import com.solo.soloplayer.data.remote.EmbyApi
import com.solo.soloplayer.di.IoDispatcher
import com.solo.soloplayer.domain.model.DiscoveredServer
import com.solo.soloplayer.domain.repository.EmbyServerRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.SocketTimeoutException
import javax.inject.Inject

class EmbyServerRepositoryImpl @Inject constructor(
    private val embyServerDao: EmbyServerDao,
    private val embyApi: EmbyApi,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : EmbyServerRepository {

    private val gson = Gson()

    override fun getAllServers(): Flow<List<EmbyServerEntity>> {
        return embyServerDao.getAllServers()
    }

    override suspend fun getConnectedServer(): EmbyServerEntity? = withContext(ioDispatcher) {
        embyServerDao.getConnectedServer()
    }

    override suspend fun getServerById(id: String): EmbyServerEntity? = withContext(ioDispatcher) {
        embyServerDao.getServerById(id)
    }

    override suspend fun saveServer(server: EmbyServerEntity) = withContext(ioDispatcher) {
        embyServerDao.insertServer(server)
        Unit
    }

    override suspend fun deleteServer(id: String) = withContext(ioDispatcher) {
        embyServerDao.deleteServerById(id)
        Unit
    }

    override suspend fun connectServer(id: String) = withContext(ioDispatcher) {
        embyServerDao.disconnectAll()
        embyServerDao.setConnected(id)
        Unit
    }

    override suspend fun disconnectAll() = withContext(ioDispatcher) {
        embyServerDao.disconnectAll()
        Unit
    }

    override suspend fun discoverServers(): List<DiscoveredServer> = withContext(ioDispatcher) {
        val discoveredList = mutableListOf<DiscoveredServer>()
        var socket: DatagramSocket? = null
        try {
            socket = DatagramSocket().apply {
                broadcast = true
                soTimeout = 1500
            }
            val message = "who is EmbyServer?".toByteArray(Charsets.UTF_8)
            val packet = DatagramPacket(
                message,
                message.size,
                InetAddress.getByName("255.255.255.255"),
                7359
            )
            socket.send(packet)

            val buffer = ByteArray(2048)
            val startTime = System.currentTimeMillis()
            while (System.currentTimeMillis() - startTime < 1500) {
                try {
                    val receivePacket = DatagramPacket(buffer, buffer.size)
                    socket.receive(receivePacket)
                    val responseStr = String(receivePacket.data, 0, receivePacket.length, Charsets.UTF_8).trim()
                    
                    val response = gson.fromJson(responseStr, EmbyUdpResponse::class.java)
                    if (response != null) {
                        val discovered = DiscoveredServer(
                            address = response.Address ?: "",
                            id = response.Id ?: "",
                            name = response.Name ?: ""
                        )
                        if (discoveredList.none { it.id == discovered.id }) {
                            discoveredList.add(discovered)
                        }
                    }
                } catch (e: SocketTimeoutException) {
                    break
                } catch (e: Exception) {
                    // Ignore parsing or socket error for specific packet
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            socket?.close()
        }
        discoveredList
    }

    override suspend fun validateCredentials(
        url: String,
        username: String,
        pw: String
    ): AuthenticateByNameResponse = withContext(ioDispatcher) {
        val fullUrl = "${url.trimEnd('/')}/Users/AuthenticateByName"
        val authHeader = "MediaBrowser Client=\"Android TV\", Device=\"soloPlayer\", DeviceId=\"soloplayer_tv_app\", Version=\"1.0.0\""
        val request = AuthenticateByNameRequest(Username = username, Pw = pw)
        try {
            val response = embyApi.authenticateByName(fullUrl, request, authHeader)
            
            // Save the verified server entity
            val server = EmbyServerEntity(
                id = response.User.Id,
                serverName = "Emby Server",
                serverUrl = url,
                userId = response.User.Id,
                userName = response.User.Name,
                accessToken = response.AccessToken,
                isConnected = true,
                alias = "Emby Server"
            )
            embyServerDao.disconnectAll()
            embyServerDao.insertServer(server)
            
            response
        } catch (e: Exception) {
            // Rethrow and ensure no server is saved
            throw e
        }
    }

    private data class EmbyUdpResponse(
        val Address: String?,
        val Id: String?,
        val Name: String?
    )
}
