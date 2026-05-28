package com.solo.soloplayer.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Ignore

@Entity(tableName = "smb_accounts")
data class SmbAccountEntity(
    @PrimaryKey
    val id: String,
    val serverIp: String,
    val serverAddress: String,
    val shareName: String,
    val username: String,
    val password: String,
    val domain: String?
) {
    @Ignore
    constructor(
        id: String,
        serverAddress: String,
        shareName: String,
        username: String,
        password: String,
        domain: String?
    ) : this(
        id = id,
        serverIp = "",
        serverAddress = serverAddress,
        shareName = shareName,
        username = username,
        password = password,
        domain = domain
    )
}
