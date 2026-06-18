package com.financeobserver

import android.app.Application
import com.financeobserver.database.AppDatabase
import com.financeobserver.detector.AnomalyDetector
import com.financeobserver.detector.SubscriptionDetector
import com.financeobserver.parser.ParserRegistry
import com.financeobserver.service.AuthManager
import com.financeobserver.service.TransactionNotifier
import com.financeobserver.service.TransactionRepository

/**
 * Application class that initializes all core components.
 * This is the entry point for dependency injection (manual, no Dagger/Hilt for MVP).
 */
class FinanceObserverApp : Application() {

    // Core components
    lateinit var database: AppDatabase
        private set

    lateinit var parserRegistry: ParserRegistry
        private set

    lateinit var transactionRepository: TransactionRepository
        private set

    lateinit var subscriptionDetector: SubscriptionDetector
        private set

    lateinit var anomalyDetector: AnomalyDetector
        private set

    lateinit var transactionNotifier: TransactionNotifier
        private set

    lateinit var authManager: AuthManager
        private set

    override fun onCreate() {
        super.onCreate()

        // Initialize database
        database = AppDatabase.getInstance(this)

        // Initialize parser registry
        parserRegistry = ParserRegistry()

        // Initialize repository
        transactionRepository = TransactionRepository(database.transactionDao())

        // Initialize detectors
        subscriptionDetector = SubscriptionDetector(
            transactionDao = database.transactionDao(),
            subscriptionDao = database.subscriptionDao()
        )

        anomalyDetector = AnomalyDetector(
            transactionDao = database.transactionDao()
        )

        transactionNotifier = TransactionNotifier(
            context = this,
            notificationDao = database.notificationDao()
        )

        authManager = AuthManager(
            context = this,
            userDao = database.userDao()
        )
    }
}
