package com.financeobserver.service

import com.financeobserver.database.TransactionDao
import com.financeobserver.model.ParsedEvent
import com.financeobserver.model.Transaction
import com.financeobserver.model.TransactionType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.Date

/**
 * Repository for transaction operations.
 * Handles parsing, deduplication, and storage.
 */
class TransactionRepository(
    private val transactionDao: TransactionDao
) {
    private val dedupMutex = Mutex()

    /**
     * Insert a parsed event as a transaction.
     * Returns the new transaction ID.
     */
    suspend fun insertParsedEvent(event: ParsedEvent): Long {
        return withContext(Dispatchers.IO) {
            val transaction = Transaction(
                source = event.source,
                sourceApp = event.sourceApp,
                rawText = event.rawText,
                merchant = event.merchant ?: "Unknown",
                amount = event.amount ?: 0.0,
                currency = event.currency ?: "USD",
                timestamp = event.date ?: Date(),
                capturedAt = event.capturedAt,
                confidenceScore = event.confidenceScore,
                isParsed = event.isParsed,
                transactionType = if (event.merchant?.contains("refund", ignoreCase = true) == true)
                    TransactionType.REFUND else TransactionType.PURCHASE
            )
            transactionDao.insert(transaction)
        }
    }

    /**
     * Atomically check for duplicates and insert if not duplicate.
     * Returns the new transaction ID, or -1 if duplicate.
     */
    suspend fun tryInsert(event: ParsedEvent, lookbackMinutes: Int = 5): Long {
        dedupMutex.withLock {
            val merchant = event.merchant
            val amount = event.amount
            if (merchant != null && amount != null) {
                val cutoffTime = Date(System.currentTimeMillis() - lookbackMinutes * 60 * 1000)
                if (transactionDao.countByDedupKey(merchant, amount, cutoffTime) > 0) {
                    return -1L
                }
            }
            val transaction = Transaction(
                source = event.source,
                sourceApp = event.sourceApp,
                rawText = event.rawText,
                merchant = event.merchant ?: "Unknown",
                amount = event.amount ?: 0.0,
                currency = event.currency ?: "USD",
                timestamp = event.date ?: Date(),
                capturedAt = event.capturedAt,
                confidenceScore = event.confidenceScore,
                isParsed = event.isParsed,
                transactionType = if (event.merchant?.contains("refund", ignoreCase = true) == true)
                    TransactionType.REFUND else TransactionType.PURCHASE
            )
            return transactionDao.insert(transaction)
        }
    }

    /**
     * Get all transactions, ordered by timestamp descending.
     */
    suspend fun getAllTransactions(): List<Transaction> {
        return withContext(Dispatchers.IO) {
            transactionDao.getAllTransactions()
        }
    }

    /**
     * Get transactions within a date range.
     */
    suspend fun getTransactionsInRange(startDate: Date, endDate: Date): List<Transaction> {
        return withContext(Dispatchers.IO) {
            transactionDao.getTransactionsInRange(startDate, endDate)
        }
    }

    /**
     * Get total spending for a date range.
     */
    suspend fun getTotalSpending(startDate: Date, endDate: Date): Double? {
        return withContext(Dispatchers.IO) {
            transactionDao.getTotalSpending(startDate, endDate)
        }
    }

    /**
     * Get spending by category for a date range.
     */
    suspend fun getSpendingByCategory(startDate: Date, endDate: Date): Map<String, Double> {
        return withContext(Dispatchers.IO) {
            transactionDao.getSpendingByCategory(startDate, endDate)
                .associate { it.category to it.total }
        }
    }

    /**
     * Get recent transactions (last N).
     */
    suspend fun getRecentTransactions(limit: Int = 50): List<Transaction> {
        return withContext(Dispatchers.IO) {
            transactionDao.getRecentTransactions(limit)
        }
    }

    /**
     * Get transaction count.
     */
    suspend fun getTransactionCount(): Int {
        return withContext(Dispatchers.IO) {
            transactionDao.getTransactionCount()
        }
    }

    /**
     * Delete a transaction.
     */
    suspend fun deleteTransaction(id: Long) {
        withContext(Dispatchers.IO) {
            transactionDao.deleteTransaction(id)
        }
    }

    /**
     * Flag a transaction as anomalous.
     */
    suspend fun flagTransaction(id: Long, reason: String) {
        withContext(Dispatchers.IO) {
            transactionDao.flagTransaction(id, reason)
        }
    }
}
