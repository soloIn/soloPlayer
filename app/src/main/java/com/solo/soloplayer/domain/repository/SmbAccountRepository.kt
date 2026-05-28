package com.solo.soloplayer.domain.repository

import com.solo.soloplayer.data.local.entity.SmbAccountEntity
import kotlinx.coroutines.flow.Flow

interface SmbAccountRepository {
    fun getAllAccounts(): Flow<List<SmbAccountEntity>>
    suspend fun saveAccount(account: SmbAccountEntity)
    suspend fun deleteAccount(id: String)
    suspend fun getAccountForPath(path: String): SmbAccountEntity?
}
