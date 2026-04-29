package com.financeobserver.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

/**
 * Represents a detected recurring subscription.
 */
@Entity(tableName = "subscriptions")
data class Subscription(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val merchant: String,
    val estimatedAmount: Double,       // Average amount per cycle
    val currency: String = "USD",
    val cycle: BillingCycle,           // WEEKLY, MONTHLY, YEARLY, etc.
    val confidence: Float,             // How confident we are it's recurring

    // Tracking
    val firstSeen: Date,
    val lastSeen: Date,
    val occurrenceCount: Int = 1,
    val nextExpectedDate: Date?,

    // Status
    val isActive: Boolean = true,
    val isCancelled: Boolean = false,
    val cancelledDate: Date? = null,

    // User interaction
    val userConfirmed: Boolean = false, // User verified this is a subscription
    val userDismissed: Boolean = false, // User said this is NOT a subscription
    val notes: String? = null
)

enum class BillingCycle {
    DAILY,
    WEEKLY,
    BIWEEKLY,
    MONTHLY,
    QUARTERLY,
    YEARLY,
    UNKNOWN
}
