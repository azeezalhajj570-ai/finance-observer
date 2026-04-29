package com.financeobserver.detector

import com.financeobserver.database.SubscriptionDao
import com.financeobserver.database.TransactionDao
import com.financeobserver.model.BillingCycle
import com.financeobserver.model.ParsedEvent
import com.financeobserver.model.Subscription
import com.financeobserver.model.Transaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Date
import java.util.concurrent.TimeUnit

/**
 * Detects recurring subscriptions from transaction patterns.
 * 
 * Algorithm:
 * 1. Group transactions by merchant
 * 2. For each merchant with 2+ transactions, check for regular intervals
 * 3. Classify the billing cycle based on the interval pattern
 * 4. Calculate confidence based on consistency of amount and timing
 */
class SubscriptionDetector(
    private val transactionDao: TransactionDao,
    private val subscriptionDao: SubscriptionDao
) {
    companion object {
        private const val MIN_OCCURRENCES = 2  // Minimum transactions to detect a subscription
        private const val AMOUNT_TOLERANCE = 0.10  // 10% tolerance for amount variation
        private const val INTERVAL_TOLERANCE_DAYS = 3  // 3 days tolerance for interval variation
    }

    /**
     * Analyze a new transaction for potential subscription patterns.
     */
    suspend fun analyzeNewTransaction(event: ParsedEvent) {
        withContext(Dispatchers.IO) {
            val merchant = event.merchant ?: return@withContext
            val amount = event.amount ?: return@withContext

            // Get all transactions for this merchant
            val merchantTransactions = transactionDao.searchByMerchant(merchant)
            if (merchantTransactions.size < MIN_OCCURRENCES) return@withContext

            // Check if we already have this subscription
            val existing = subscriptionDao.getByMerchant(merchant)
            if (existing != null && existing.userDismissed) return@withContext

            // Analyze for recurring pattern
            val pattern = detectRecurringPattern(merchantTransactions)
            if (pattern == null) return@withContext

            if (existing != null) {
                // Update existing subscription
                subscriptionDao.update(
                    existing.copy(
                        occurrenceCount = merchantTransactions.size,
                        lastSeen = Date(),
                        estimatedAmount = pattern.averageAmount,
                        nextExpectedDate = pattern.nextExpectedDate,
                        confidence = pattern.confidence
                    )
                )
            } else {
                // Create new subscription
                val subscription = Subscription(
                    merchant = merchant,
                    estimatedAmount = pattern.averageAmount,
                    cycle = pattern.cycle,
                    confidence = pattern.confidence,
                    firstSeen = merchantTransactions.last().timestamp,
                    lastSeen = Date(),
                    occurrenceCount = merchantTransactions.size,
                    nextExpectedDate = pattern.nextExpectedDate
                )
                subscriptionDao.insert(subscription)
            }
        }
    }

    /**
     * Detect recurring payment pattern in a list of transactions.
     */
    private fun detectRecurringPattern(transactions: List<Transaction>): RecurringPattern? {
        if (transactions.size < MIN_OCCURRENCES) return null

        // Sort by date
        val sorted = transactions.sortedBy { it.timestamp }

        // Calculate intervals between transactions
        val intervals = mutableListOf<Long>()
        for (i in 1 until sorted.size) {
            val daysBetween = TimeUnit.MILLISECONDS.toDays(
                sorted[i].timestamp.time - sorted[i - 1].timestamp.time
            )
            if (daysBetween > 0) {
                intervals.add(daysBetween)
            }
        }

        if (intervals.isEmpty()) return null

        // Check for consistent intervals
        val averageInterval = intervals.average()
        val intervalVariance = intervals.map { (it - averageInterval) * (it - averageInterval) }.average()

        // Too much variance = not a subscription
        if (intervalVariance > INTERVAL_TOLERANCE_DAYS * INTERVAL_TOLERANCE_DAYS) return null

        // Check for consistent amounts
        val amounts = sorted.map { it.amount }
        val averageAmount = amounts.average()
        val amountVariance = amounts.map { (it - averageAmount) * (it - averageAmount) }.average()
        val amountCoefficientOfVariation = if (averageAmount > 0) Math.sqrt(amountVariance) / averageAmount else 1.0

        // Too much amount variation = probably not a subscription
        if (amountCoefficientOfVariation > AMOUNT_TOLERANCE) return null

        // Determine billing cycle
        val cycle = determineBillingCycle(averageInterval)

        // Calculate next expected date
        val lastTransaction = sorted.last()
        val nextExpectedDate = Date(lastTransaction.timestamp.time + (averageInterval.toLong() * 24 * 60 * 60 * 1000))

        // Calculate confidence
        val confidence = calculateConfidence(
            occurrenceCount = sorted.size,
            intervalConsistency = 1.0 - (Math.sqrt(intervalVariance) / averageInterval).coerceIn(0.0, 1.0),
            amountConsistency = 1.0 - amountCoefficientOfVariation
        )

        return RecurringPattern(
            cycle = cycle,
            averageAmount = averageAmount,
            averageIntervalDays = averageInterval,
            nextExpectedDate = nextExpectedDate,
            confidence = confidence
        )
    }

    private fun determineBillingCycle(averageIntervalDays: Double): BillingCycle {
        return when {
            averageIntervalDays <= 2 -> BillingCycle.DAILY
            averageIntervalDays <= 10 -> BillingCycle.WEEKLY
            averageIntervalDays <= 20 -> BillingCycle.BIWEEKLY
            averageIntervalDays <= 40 -> BillingCycle.MONTHLY
            averageIntervalDays <= 100 -> BillingCycle.QUARTERLY
            else -> BillingCycle.YEARLY
        }
    }

    private fun calculateConfidence(
        occurrenceCount: Int,
        intervalConsistency: Double,
        amountConsistency: Double
    ): Float {
        // More occurrences = higher confidence
        val occurrenceScore = (occurrenceCount.coerceAtMost(10) / 10.0)
        
        // Weighted average
        val confidence = (occurrenceScore * 0.3 + intervalConsistency * 0.4 + amountConsistency * 0.3)
        return confidence.toFloat().coerceIn(0.0f, 1.0f)
    }

    /**
     * Get all detected subscriptions.
     */
    suspend fun getActiveSubscriptions(): List<Subscription> {
        return subscriptionDao.getActiveSubscriptions()
    }

    /**
     * Get total monthly subscription cost.
     */
    suspend fun getTotalMonthlyCost(): Double {
        return subscriptionDao.getTotalMonthlySubscriptions() ?: 0.0
    }
}

data class RecurringPattern(
    val cycle: BillingCycle,
    val averageAmount: Double,
    val averageIntervalDays: Double,
    val nextExpectedDate: Date,
    val confidence: Float
)
