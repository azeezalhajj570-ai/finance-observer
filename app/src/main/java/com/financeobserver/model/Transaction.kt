package com.financeobserver.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

/**
 * Represents a parsed financial transaction from any source (notification, SMS, etc.)
 */
@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    // Source information
    val source: SourceType,          // NOTIFICATION, SMS, MANUAL
    val sourceApp: String?,          // e.g., "com.chase.mobile", "com.venmo"
    val rawText: String,             // Original notification/SMS text

    // Parsed transaction data
    val merchant: String,            // Extracted merchant name
    val amount: Double,              // Transaction amount (always positive)
    val currency: String = "USD",
    val category: String? = null,    // Auto-detected category
    val transactionType: TransactionType = TransactionType.PURCHASE,

    // Temporal data
    val timestamp: Date,             // When the transaction occurred
    val capturedAt: Date,            // When we captured it

    // Confidence and status
    val confidenceScore: Float = 1.0f, // 0.0-1.0, how confident we are in the parse
    val isParsed: Boolean = true,     // False if we couldn't parse it
    val isDuplicate: Boolean = false,
    val duplicateOfId: Long? = null,

    // Metadata
    val location: String? = null,
    val isFlagged: Boolean = false,   // Anomaly flag
    val flagReason: String? = null,
    val isSubscription: Boolean = false,
    val subscriptionId: Long? = null,
    val notes: String? = null
)

enum class SourceType {
    NOTIFICATION,
    SMS,
    MANUAL
}

enum class TransactionType {
    PURCHASE,
    REFUND,
    TRANSFER_IN,
    TRANSFER_OUT,
    SUBSCRIPTION,
    FEE,
    UNKNOWN
}
