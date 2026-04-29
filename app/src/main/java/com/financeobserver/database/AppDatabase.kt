package com.financeobserver.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.financeobserver.model.Subscription
import com.financeobserver.model.Transaction

/**
 * Room Database for Finance Observer.
 * 
 * Contains all tables: transactions, subscriptions.
 * 
 * Security note: For production, use SQLCipher for encryption:
 * - Replace RoomDatabase with net.zetetic:android-database-sqlcipher
 * - Add passphrase via SupportFactory
 */
@Database(
    entities = [Transaction::class, Subscription::class],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun transactionDao(): TransactionDao
    abstract fun subscriptionDao(): SubscriptionDao

    companion object {
        private const val DATABASE_NAME = "finance_observer.db"

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }
        }

        private fun buildDatabase(context: Context): AppDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                DATABASE_NAME
            )
                // Fallback to destructive migration for development
                // In production, implement proper migrations
                .fallbackToDestructiveMigration()
                // Enable WAL mode for better concurrent read performance
                .setJournalMode(JournalMode.WRITE_AHEAD_LOGGING)
                .addCallback(object : RoomDatabase.Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        // Create indexes for common queries
                        db.execSQL("CREATE INDEX IF NOT EXISTS idx_transactions_timestamp ON transactions(timestamp)")
                        db.execSQL("CREATE INDEX IF NOT EXISTS idx_transactions_merchant ON transactions(merchant)")
                        db.execSQL("CREATE INDEX IF NOT EXISTS idx_transactions_source ON transactions(source)")
                        db.execSQL("CREATE INDEX IF NOT EXISTS idx_transactions_flagged ON transactions(isFlagged)")
                        db.execSQL("CREATE INDEX IF NOT EXISTS idx_subscriptions_active ON subscriptions(isActive)")
                    }
                })
                .build()
        }

        // Migration from v1 to v2 (example - add new columns)
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Example: database.execSQL("ALTER TABLE transactions ADD COLUMN notes TEXT")
            }
        }
    }
}
