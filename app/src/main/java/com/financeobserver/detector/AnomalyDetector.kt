package com.financeobserver.detector

import com.financeobserver.database.TransactionDao
import com.financeobserver.model.ParsedEvent
import com.financeobserver.model.Transaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Date
import java.util.concurrent.TimeUnit

/**
 * Detects anomalous transactions based on spending patterns.
 * 
 * Algorithm:
 * 1. Build a spending baseline per merchant and per category
 * 2. Flag transactions that deviate significantly from the baseline
 * 3. Consider location, time, and amount anomalies
 * 
 * Anomaly types:
 * - Amount anomaly: Transaction amount is significantly higher than usual for this merchant
 * - Frequency anomaly: More transactions than usual in a time period
 * - Location anomaly: Transaction in an unusual location (requires location permission)
 * - Time anomaly: Transaction at an unusual time
 */
class AnomalyDetector(
    private val transactionDao: TransactionDao
) {
    companion object {
        private const val AMOUNT_THRESHOLD_SIGMAS = 2.0  // Flag if amount is 2+ standard deviations above mean
        private const val MIN_TRANSACTIONS_FOR_BASELINE = 3  // Need at least 3 transactions to build a baseline
        private const val DAILY_SPENDING_MULTIPLIER = 3.0  // Flag if daily spending is 3x the average
    }

    /**
     * Analyze a new transaction for anomalies.
     */
    suspend fun analyzeNewTransaction(event: ParsedEvent) {
        withContext(Dispatchers.IO) {
            val merchant = event.merchant ?: return@withContext
            val amount = event.amount ?: return@withContext

            // Get historical transactions for this merchant
            val history = transactionDao.searchByMerchant(merchant)
            if (history.size < MIN_TRANSACTIONS_FOR_BASELINE) return@withContext

            // Check for amount anomaly
            val anomaly = detectAmountAnomaly(amount, history, merchant)
            if (anomaly != null) {
                // Flag the most recent transaction
                val latestTransaction = history.firstOrNull()
                if (latestTransaction != null) {
                    transactionDao.flagTransaction(latestTransaction.id, anomaly.reason)
                }
            }
        }
    }

    /**
     * Detect if a transaction amount is anomalous compared to history.
     */
    private fun detectAmountAnomaly(
        amount: Double,
        history: List<Transaction>,
        merchant: String
    ): AnomalyResult? {
        val amounts = history.map { it.amount }
        val mean = amounts.average()
        val stdDev = calculateStandardDeviation(amounts, mean)

        // If standard deviation is 0 (all same amount), any different amount is anomalous
        if (stdDev == 0.0) {
            if (amount != mean) {
                return AnomalyResult(
                    type = AnomalyType.AMOUNT_CHANGE,
                    reason = "Amount changed from usual $${String.format("%.2f", mean)} to $${String.format("%.2f", amount)} at $merchant",
                    severity = if (amount > mean) Severity.HIGH else Severity.MEDIUM
                )
            }
            return null
        }

        // Check if amount is significantly above the mean
        val zScore = (amount - mean) / stdDev
        if (zScore > AMOUNT_THRESHOLD_SIGMAS) {
            return AnomalyResult(
                type = AnomalyType.AMOUNT_SPIKE,
                reason = "Unusually high charge: $${String.format("%.2f", amount)} at $merchant (usual: $${String.format("%.2f", mean)} ± $${String.format("%.2f", stdDev)})",
                severity = when {
                    zScore > 4.0 -> Severity.CRITICAL
                    zScore > 3.0 -> Severity.HIGH
                    else -> Severity.MEDIUM
                }
            )
        }

        return null
    }

    /**
     * Check for daily spending anomalies.
     */
    suspend fun checkDailySpendingAnomaly(): List<AnomalyResult> {
        val results = mutableListOf<AnomalyResult>()
        val now = Date()
        val startOfDay = Date(now.time - TimeUnit.MILLISECONDS.convert(getHoursSinceMidnight(), TimeUnit.HOURS))

        // Get today's transactions
        val todayTransactions = transactionDao.getTransactionsInRange(startOfDay, now)
        if (todayTransactions.isEmpty()) return results

        val todayTotal = todayTransactions.sumOf { it.amount }

        // Get average daily spending for the past 30 days
        val thirtyDaysAgo = Date(now.time - TimeUnit.MILLISECONDS.convert(30, TimeUnit.DAYS))
        val pastTransactions = transactionDao.getTransactionsInRange(thirtyDaysAgo, startOfDay)

        if (pastTransactions.size < 7) return results  // Not enough data

        // Group by day and calculate average
        val dailyTotals = pastTransactions
            .groupBy { getDayKey(it.timestamp) }
            .mapValues { (_, txns) -> txns.sumOf { it.amount } }

        val averageDailySpending = dailyTotals.values.average()
        val dailyStdDev = calculateStandardDeviation(dailyTotals.values.toList(), averageDailySpending)

        if (dailyStdDev == 0.0) {
            if (todayTotal > averageDailySpending * DAILY_SPENDING_MULTIPLIER) {
                results.add(
                    AnomalyResult(
                        type = AnomalyType.DAILY_SPIKE,
                        reason = "Today's spending ($${String.format("%.2f", todayTotal)}) is ${String.format("%.1f", todayTotal / averageDailySpending)}x your daily average ($${String.format("%.2f", averageDailySpending)})",
                        severity = Severity.HIGH
                    )
                )
            }
        } else {
            val zScore = (todayTotal - averageDailySpending) / dailyStdDev
            if (zScore > AMOUNT_THRESHOLD_SIGMAS) {
                results.add(
                    AnomalyResult(
                        type = AnomalyType.DAILY_SPIKE,
                        reason = "Today's spending ($${String.format("%.2f", todayTotal)}) is unusually high (z-score: ${String.format("%.1f", zScore)})",
                        severity = if (zScore > 3.0) Severity.CRITICAL else Severity.HIGH
                    )
                )
            }
        }

        return results
    }

    private fun calculateStandardDeviation(values: List<Double>, mean: Double): Double {
        if (values.isEmpty()) return 0.0
        val variance = values.map { (it - mean) * (it - mean) }.average()
        return Math.sqrt(variance)
    }

    private fun getHoursSinceMidnight(): Long {
        val now = System.currentTimeMillis()
        val startOfDay = now - (now % (24 * 60 * 60 * 1000))
        return (now - startOfDay) / (60 * 60 * 1000)
    }

    private fun getDayKey(date: Date): String {
        return java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(date)
    }

    /**
     * Get all flagged transactions.
     */
    suspend fun getFlaggedTransactions(): List<Transaction> {
        return transactionDao.getFlaggedTransactions()
    }
}

enum class AnomalyType {
    AMOUNT_SPIKE,      // Single transaction is unusually high
    AMOUNT_CHANGE,     // Amount changed from usual pattern
    DAILY_SPIKE,       // Total daily spending is unusually high
    FREQUENCY_SPIKE,   // More transactions than usual
    LOCATION_ANOMALY,  // Transaction in unusual location
    TIME_ANOMALY       // Transaction at unusual time
}

enum class Severity {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}

data class AnomalyResult(
    val type: AnomalyType,
    val reason: String,
    val severity: Severity
)
