package com.financeobserver.service

import android.app.Notification
import android.content.Intent
import android.os.IBinder
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.financeobserver.FinanceObserverApp
import com.financeobserver.model.ParsedEvent
import com.financeobserver.parser.ParserRegistry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Notification Listener Service that captures all notifications and filters for payment-related ones.
 * 
 * This service requires the user to grant Notification Access in Settings.
 * It runs in the background and processes notifications as they arrive.
 * 
 * Key design decisions:
 * - Processes notifications on a background coroutine scope
 * - Uses the ParserRegistry to parse each notification
 * - Stores parsed events in the Room database
 * - Logs all parse attempts for analytics
 */
class PaymentNotificationListener : NotificationListenerService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val app: FinanceObserverApp get() = application as FinanceObserverApp
    private val parserRegistry: ParserRegistry get() = app.parserRegistry
    private val transactionRepository: TransactionRepository get() = app.transactionRepository

    companion object {
        private const val TAG = "PaymentNotificationListener"

        // Known payment/finance app package names to prioritize
        val PAYMENT_APP_PACKAGES = setOf(
            "com.chase.mobile",
            "com.chase.sig.android",
            "com.infonow.bofa",
            "com.bankofamerica.digitalwallet",
            "com.venmo",
            "com.venmo.android",
            "com.paypal.android.p2pmobile",
            "com.google.android.apps.nbu.paisa.user",
            "com.google.android.apps.walletnfcrel",
            "com.squareup.cash",
            "com.zellepay.zelle",
            "com.stripe.android",
            "com.squareup",
            "com.wf.wellsfargomobile",
            "com.usaa.mobile.android.usaa",
            "com.capitalone",
            "com.discover.mobile.banking",
            "com.citi.citimobile"
        )

        // Payment-related keywords to filter notifications
        val PAYMENT_KEYWORDS = listOf(
            "paid", "payment", "purchase", "charge", "spent", "bought",
            "received", "sent", "transfer", "deposit", "withdrawal",
            "subscription", "renewal", "auto-pay", "autopay",
            "your card", "debit card", "credit card", "account ending",
            "$", "USD", "transaction", "receipt",
            // Arabic keywords
            "اضيف", "خصم", "تحويل", "دفع", "استلمت", "إيداع",
            "سحبت", "إرسال", "استلام", "حوالة", "رصيد",
            "ر.ي", "YER", "ر.س", "SAR", "ريال", "مبلغ",
            "شحن", "شراء", "مشتريات", "بطاقة", "محفظة",
            "صراف", "بنك", "جوال", "سداد", "اصدار"
        )
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service created")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val packageName = sbn.packageName
        val notification = sbn.notification

        // Quick filter: skip non-payment apps that don't contain payment keywords
        if (!isLikelyPaymentNotification(packageName, notification)) {
            return
        }

        // Process in background to avoid blocking the notification service
        serviceScope.launch {
            try {
                processNotification(sbn)
            } catch (e: Exception) {
                Log.e(TAG, "Error processing notification from $packageName", e)
            }
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        // No action needed - we process on post, not remove
    }

    override fun onBind(intent: Intent?): IBinder? {
        return super.onBind(intent)
    }

    /**
     * Quick filter to determine if a notification might be payment-related.
     * This avoids expensive parsing for irrelevant notifications.
     */
    private fun isLikelyPaymentNotification(
        packageName: String,
        notification: Notification
    ): Boolean {
        // Always process known payment apps
        if (packageName in PAYMENT_APP_PACKAGES) {
            return true
        }

        // Check notification text for payment keywords
        val extras = notification.extras
        val title = extras?.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
        val text = extras?.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
        val bigText = extras?.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString() ?: ""

        val combinedText = "$title $text $bigText"

        return PAYMENT_KEYWORDS.any { keyword ->
            combinedText.contains(keyword, ignoreCase = true)
        }
    }

    /**
     * Process a notification: extract text, parse, dedup, and store.
     */
    private suspend fun processNotification(sbn: StatusBarNotification) {
        val packageName = sbn.packageName
        val extras = sbn.notification.extras

        val title = extras?.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
        val text = extras?.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
        val bigText = extras?.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString() ?: ""

        // Combine all text sources
        val fullText = if (bigText.isNotEmpty()) "$title $bigText" else "$title $text"

        if (fullText.isBlank()) {
            Log.d(TAG, "Empty notification from $packageName, skipping")
            return
        }

        Log.d(TAG, "Processing notification from $packageName (length=${fullText.length})")

        // Parse the notification using the registry
        val parsedEvent = parserRegistry.parseNotification(
            packageName = packageName,
            title = title,
            text = fullText
        )

        // Atomic check-and-insert to prevent race conditions
        val transactionId = transactionRepository.tryInsert(parsedEvent)

        if (transactionId < 0) {
            Log.d(TAG, "Duplicate notification detected, skipping: ${parsedEvent.merchant} ${parsedEvent.amount}")
            return
        }

        Log.d(TAG, "Stored transaction: id=$transactionId, merchant=${parsedEvent.merchant}, amount=${parsedEvent.amount}, parsed=${parsedEvent.isParsed}")

        // If parsed successfully, run detection engines
        if (parsedEvent.isParsed) {
            app.subscriptionDetector.analyzeNewTransaction(parsedEvent)
            app.anomalyDetector.analyzeNewTransaction(parsedEvent)
        }
    }

    /**
     * Check if notification access is granted.
     */
    fun isNotificationAccessEnabled(): Boolean {
        val enabledPackages = android.provider.Settings.Secure.getString(
            contentResolver,
            "enabled_notification_listeners"
        ) ?: ""
        return enabledPackages.contains(packageName)
    }
}
