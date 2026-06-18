package com.financeobserver.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.financeobserver.model.Account

@Dao
interface AccountDao {
    @Query("SELECT * FROM accounts WHERE isActive = 1 ORDER BY createdAt DESC")
    suspend fun getActiveAccounts(): List<Account>

    @Query("SELECT * FROM accounts ORDER BY createdAt DESC")
    suspend fun getAllAccounts(): List<Account>

    @Query("SELECT * FROM accounts WHERE id = :id")
    suspend fun getById(id: Long): Account?

    @Insert
    suspend fun insert(account: Account): Long

    @Update
    suspend fun update(account: Account)

    @Delete
    suspend fun delete(account: Account)

    @Query("SELECT SUM(balance) FROM accounts WHERE isActive = 1")
    suspend fun getTotalBalance(): Double?
}
