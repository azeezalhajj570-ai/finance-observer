package com.financeobserver.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class NotificationStatus {
    PENDING,
    SENT,
    FAILED,
    SUPPRESSED
}

@Entity(tableName = "transaction_notifications")
data class TransactionNotification(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val transactionId: Long,
    val recipientId: String = "device_user",
    val channel: String = "in_app",
    val status: NotificationStatus = NotificationStatus.PENDING,
    val createdAt: Long = System.currentTimeMillis(),
    val sentAt: Long? = null,
    val errorMessage: String? = null,
    val attemptCount: Int = 0,
    val lastAttemptAt: Long? = null
)
