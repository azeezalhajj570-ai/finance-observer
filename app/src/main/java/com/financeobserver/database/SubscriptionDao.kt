package com.financeobserver.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.financeobserver.model.Subscription

@Dao
interface SubscriptionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(subscription: Subscription): Long

    @Update
    suspend fun update(subscription: Subscription)

    @Query("SELECT * FROM subscriptions WHERE isActive = 1 ORDER BY estimatedAmount DESC")
    suspend fun getActiveSubscriptions(): List<Subscription>

    @Query("SELECT * FROM subscriptions ORDER BY lastSeen DESC")
    suspend fun getAllSubscriptions(): List<Subscription>

    @Query("SELECT * FROM subscriptions WHERE merchant = :merchant LIMIT 1")
    suspend fun getByMerchant(merchant: String): Subscription?

    @Query("SELECT * FROM subscriptions WHERE nextExpectedDate <= :date AND isActive = 1")
    suspend fun getUpcomingSubscriptions(date: java.util.Date): List<Subscription>

    @Query("UPDATE subscriptions SET isActive = 0, isCancelled = 1, cancelledDate = :date WHERE id = :id")
    suspend fun cancelSubscription(id: Long, date: java.util.Date)

    @Query("UPDATE subscriptions SET userConfirmed = 1 WHERE id = :id")
    suspend fun confirmSubscription(id: Long)

    @Query("UPDATE subscriptions SET userDismissed = 1 WHERE id = :id")
    suspend fun dismissSubscription(id: Long)

    @Query("SELECT SUM(estimatedAmount) FROM subscriptions WHERE isActive = 1")
    suspend fun getTotalMonthlySubscriptions(): Double?
}
