package com.financeobserver.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.financeobserver.model.NotificationStatus
import com.financeobserver.model.TransactionNotification

@Dao
interface NotificationDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(notification: TransactionNotification): Long

    @Query("""
        SELECT * FROM transaction_notifications 
        WHERE transactionId = :transactionId AND recipientId = :recipientId
    """)
    suspend fun findByTransactionAndRecipient(
        transactionId: Long,
        recipientId: String = "device_user"
    ): TransactionNotification?

    @Query("SELECT * FROM transaction_notifications WHERE status = :status ORDER BY createdAt DESC")
    suspend fun findByStatus(status: NotificationStatus): List<TransactionNotification>

    @Query("""
        UPDATE transaction_notifications 
        SET status = 'SENT', sentAt = :sentAt 
        WHERE id = :id
    """)
    suspend fun markSent(id: Long, sentAt: Long = System.currentTimeMillis())

    @Query("""
        UPDATE transaction_notifications 
        SET status = :status, errorMessage = :errorMessage, 
            attemptCount = attemptCount + 1, lastAttemptAt = :lastAttemptAt 
        WHERE id = :id
    """)
    suspend fun markFailed(
        id: Long,
        status: NotificationStatus = NotificationStatus.FAILED,
        errorMessage: String?,
        lastAttemptAt: Long = System.currentTimeMillis()
    )

    @Query("SELECT * FROM transaction_notifications ORDER BY createdAt DESC LIMIT :limit")
    suspend fun getRecentNotifications(limit: Int = 50): List<TransactionNotification>

    @Query("SELECT COUNT(*) FROM transaction_notifications WHERE transactionId = :transactionId")
    suspend fun countByTransaction(transactionId: Long): Int

    @Query("SELECT COUNT(*) FROM transaction_notifications WHERE status = :status")
    suspend fun countByStatus(status: NotificationStatus): Int
}
