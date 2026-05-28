package com.solo.soloplayer.data.repository

import com.solo.soloplayer.data.local.dao.SmbAccountDao
import com.solo.soloplayer.data.local.entity.SmbAccountEntity
import com.solo.soloplayer.di.IoDispatcher
import com.solo.soloplayer.domain.repository.SmbAccountRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject

class SmbAccountRepositoryImpl @Inject constructor(
    private val smbAccountDao: SmbAccountDao,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : SmbAccountRepository {

    override fun getAllAccounts(): Flow<List<SmbAccountEntity>> {
        return smbAccountDao.getAllAccounts()
    }

    override suspend fun saveAccount(account: SmbAccountEntity) = withContext(ioDispatcher) {
        smbAccountDao.insertAccount(account)
        Unit
    }

    override suspend fun deleteAccount(id: String) = withContext(ioDispatcher) {
        smbAccountDao.deleteAccountById(id)
        Unit
    }

    override suspend fun getAccountForPath(path: String): SmbAccountEntity? = withContext(ioDispatcher) {
        val accounts = smbAccountDao.getAllAccountsOnce()
        if (accounts.isEmpty()) return@withContext null

        val normalizedPath = path.lowercase().trim()

        val prefixMatch = accounts.firstOrNull { account ->
            val addr = account.serverAddress.lowercase().trim()
            normalizedPath.startsWith(addr) || addr.startsWith(normalizedPath)
        }
        if (prefixMatch != null) return@withContext prefixMatch

        val pathHost = extractHost(normalizedPath)
        if (pathHost.isNotEmpty()) {
            val hostMatch = accounts.firstOrNull { account ->
                extractHost(account.serverAddress.lowercase().trim()) == pathHost ||
                account.serverIp.lowercase().trim() == pathHost
            }
            if (hostMatch != null) return@withContext hostMatch
        }

        null
    }

    private fun extractHost(path: String): String {
        var clean = path
        if (clean.startsWith("smb://")) {
            clean = clean.substring(6)
        }
        val firstSlash = clean.indexOf('/')
        return if (firstSlash != -1) {
            clean.substring(0, firstSlash)
        } else {
            clean
        }
    }
}
