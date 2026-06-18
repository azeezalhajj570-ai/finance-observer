package com.financeobserver.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.financeobserver.model.User

@Dao
interface UserDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(user: User)

    @Query("SELECT * FROM users WHERE phoneNumber = :phoneNumber")
    suspend fun findByPhone(phoneNumber: String): User?

    @Query("UPDATE users SET biometricEnabled = :enabled WHERE phoneNumber = :phoneNumber")
    suspend fun setBiometricEnabled(phoneNumber: String, enabled: Boolean)

    @Query("SELECT COUNT(*) FROM users")
    suspend fun getUserCount(): Int

    @Query("DELETE FROM users")
    suspend fun deleteAll()
}
