package com.financeobserver.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.financeobserver.model.Subscription
import com.financeobserver.model.Transaction

@Database(
    entities = [Transaction::class, Subscription::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
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
                .fallbackToDestructiveMigration()
                .setJournalMode(JournalMode.WRITE_AHEAD_LOGGING)
                .addCallback(object : RoomDatabase.Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        db.execSQL("CREATE INDEX IF NOT EXISTS idx_transactions_timestamp ON transactions(timestamp)")
                        db.execSQL("CREATE INDEX IF NOT EXISTS idx_transactions_merchant ON transactions(merchant)")
                        db.execSQL("CREATE INDEX IF NOT EXISTS idx_transactions_source ON transactions(source)")
                        db.execSQL("CREATE INDEX IF NOT EXISTS idx_transactions_flagged ON transactions(isFlagged)")
                        db.execSQL("CREATE INDEX IF NOT EXISTS idx_subscriptions_active ON subscriptions(isActive)")
                    }
                })
                .build()
        }

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Example: database.execSQL("ALTER TABLE transactions ADD COLUMN notes TEXT")
            }
        }
    }
}
