package com.solo.soloplayer.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Ignore

@Entity(tableName = "emby_servers")
data class EmbyServerEntity(
    @PrimaryKey
    val id: String,
    val serverName: String,
    val serverUrl: String,
    val userId: String,
    val userName: String,
    val accessToken: String,
    val isConnected: Boolean,
    val alias: String
) {
    @Ignore
    constructor(
        id: String,
        serverName: String,
        serverUrl: String,
        accessToken: String,
        userId: String,
        isConnected: Boolean
    ) : this(
        id = id,
        serverName = serverName,
        serverUrl = serverUrl,
        userId = userId,
        userName = "",
        accessToken = accessToken,
        isConnected = isConnected,
        alias = ""
    )
}
