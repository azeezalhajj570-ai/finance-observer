package com.financeobserver.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.financeobserver.model.Transaction
import java.util.Date

@Dao
interface TransactionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transaction: Transaction): Long

    @Update
    suspend fun update(transaction: Transaction)

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    suspend fun getAllTransactions(): List<Transaction>

    @Query("SELECT * FROM transactions WHERE timestamp BETWEEN :startDate AND :endDate ORDER BY timestamp DESC")
    suspend fun getTransactionsInRange(startDate: Date, endDate: Date): List<Transaction>

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentTransactions(limit: Int): List<Transaction>

    @Query("SELECT COUNT(*) FROM transactions WHERE source = :source")
    suspend fun countBySource(source: String): Int

    @Query("""
        SELECT COUNT(*) FROM transactions 
        WHERE merchant = :merchant 
        AND ABS(amount - :amount) < 0.01 
        AND timestamp >= :cutoffTime
    """)
    suspend fun countByDedupKey(merchant: String, amount: Double, cutoffTime: Date): Int

    @Query("""
        SELECT COUNT(*) FROM transactions 
        WHERE rawText LIKE '%' || :dedupKey || '%' 
        AND capturedAt >= :cutoffTime
    """)
    suspend fun countByDedupKey(dedupKey: String, cutoffTime: Date): Int

    @Query("SELECT SUM(amount) FROM transactions WHERE timestamp BETWEEN :startDate AND :endDate AND amount > 0")
    suspend fun getTotalSpending(startDate: Date, endDate: Date): Double?

    @Query("""
        SELECT category, SUM(amount) as total 
        FROM transactions 
        WHERE timestamp BETWEEN :startDate AND :endDate 
        AND category IS NOT NULL 
        GROUP BY category 
        ORDER BY total DESC
    """)
    suspend fun getSpendingByCategory(startDate: Date, endDate: Date): List<SpendingByCategory>

    @Query("SELECT COUNT(*) FROM transactions")
    suspend fun getTransactionCount(): Int

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteTransaction(id: Long)

    @Query("UPDATE transactions SET isFlagged = 1, flagReason = :reason WHERE id = :id")
    suspend fun flagTransaction(id: Long, reason: String)

    @Query("SELECT * FROM transactions WHERE isFlagged = 1 ORDER BY timestamp DESC")
    suspend fun getFlaggedTransactions(): List<Transaction>

    @Query("SELECT * FROM transactions WHERE isSubscription = 1 ORDER BY timestamp DESC")
    suspend fun getSubscriptionTransactions(): List<Transaction>

    @Query("""
        SELECT merchant, COUNT(*) as count, AVG(amount) as avgAmount 
        FROM transactions 
        WHERE timestamp >= :sinceDate 
        GROUP BY merchant 
        HAVING count >= :minOccurrences 
        ORDER BY count DESC
    """)
    suspend fun getFrequentMerchants(sinceDate: Date, minOccurrences: Int = 2): List<FrequentMerchant>

    @Query("SELECT * FROM transactions WHERE merchant LIKE '%' || :query || '%' ORDER BY timestamp DESC LIMIT 20")
    suspend fun searchByMerchant(query: String): List<Transaction>
}
