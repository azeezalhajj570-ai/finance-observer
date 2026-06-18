package com.financeobserver.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.financeobserver.model.Account
import com.financeobserver.model.Subscription
import com.financeobserver.model.Transaction
import com.financeobserver.model.TransactionNotification
import com.financeobserver.model.User

@Database(
    entities = [Transaction::class, Subscription::class, Account::class, TransactionNotification::class, User::class],
    version = 4,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun transactionDao(): TransactionDao
    abstract fun subscriptionDao(): SubscriptionDao
    abstract fun accountDao(): AccountDao
    abstract fun notificationDao(): NotificationDao
    abstract fun userDao(): UserDao

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
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
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
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS accounts (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        bankName TEXT NOT NULL,
                        accountNumber TEXT,
                        cardType TEXT,
                        currency TEXT NOT NULL DEFAULT 'USD',
                        balance REAL NOT NULL DEFAULT 0.0,
                        isActive INTEGER NOT NULL DEFAULT 1,
                        createdAt INTEGER NOT NULL
                    )
                """)
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS transaction_notifications (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        transactionId INTEGER NOT NULL,
                        recipientId TEXT NOT NULL DEFAULT 'device_user',
                        channel TEXT NOT NULL DEFAULT 'in_app',
                        status TEXT NOT NULL DEFAULT 'PENDING',
                        createdAt INTEGER NOT NULL,
                        sentAt INTEGER,
                        errorMessage TEXT,
                        attemptCount INTEGER NOT NULL DEFAULT 0,
                        lastAttemptAt INTEGER
                    )
                """)
                database.execSQL("CREATE INDEX IF NOT EXISTS idx_notifications_transaction ON transaction_notifications(transactionId)")
                database.execSQL("CREATE INDEX IF NOT EXISTS idx_notifications_status ON transaction_notifications(status)")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS users (
                        phoneNumber TEXT NOT NULL PRIMARY KEY,
                        passwordHash TEXT NOT NULL,
                        biometricEnabled INTEGER NOT NULL DEFAULT 0,
                        createdAt INTEGER NOT NULL
                    )
                """)
            }
        }
    }
}
