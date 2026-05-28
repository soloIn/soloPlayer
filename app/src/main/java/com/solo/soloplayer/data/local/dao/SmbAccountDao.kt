package com.solo.soloplayer.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.solo.soloplayer.data.local.entity.SmbAccountEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SmbAccountDao {
    @Query("SELECT * FROM smb_accounts")
    fun getAllAccounts(): Flow<List<SmbAccountEntity>>

    @Query("SELECT * FROM smb_accounts")
    fun getAllAccountsOnce(): List<SmbAccountEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAccount(account: SmbAccountEntity): Long

    @Query("DELETE FROM smb_accounts WHERE id = :id")
    fun deleteAccountById(id: String): Int
}
