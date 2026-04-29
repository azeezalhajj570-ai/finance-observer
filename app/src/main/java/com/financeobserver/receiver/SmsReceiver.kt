package com.financeobserver.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import com.financeobserver.FinanceObserverApp
import com.financeobserver.model.ParsedEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * BroadcastReceiver that intercepts incoming SMS messages and parses payment-related ones.
 * 
 * Requires READ_SMS and RECEIVE_SMS permissions.
 * High priority (999) ensures we receive SMS before other apps.
 */
class SmsReceiver : BroadcastReceiver() {

    private val receiverScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        private const val TAG = "SmsReceiver"

        // Known payment SMS senders
        val PAYMENT_SENDERS = setOf(
            "Chase", "CHASE", "ChaseAlerts",
            "Bank of America", "BofA", "BOFA",
            "Venmo", "VENMO",
            "PayPal", "PAYPAL",
            "Google Pay", "GPay",
            "Cash App", "CashApp",
            "Zelle", "ZELLE",
            "Stripe", "STRIPE",
            "Square", "SQUARE",
            "Wells Fargo", "WELLSFARGO",
            "USAA",
            "Capital One", "CAPITALONE",
            "Discover", "DISCOVER",
            "Citi", "CITI",
            "AMEX", "American Express"
        )

        // Payment-related keywords in SMS body
        val PAYMENT_KEYWORDS = listOf(
            "paid", "payment", "purchase", "charge", "spent",
            "received", "sent", "transfer", "deposit",
            "your card", "account ending", "transaction",
            "$", "USD", "receipt", "confirmation"
        )
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            return
        }

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (messages.isNullOrEmpty()) {
            return
        }

        // Combine multi-part SMS
        val fullMessage = messages.joinToString("") { it.messageBody ?: "" }
        val sender = messages.firstOrNull()?.originatingAddress ?: "Unknown"

        // Quick filter: skip non-payment SMS
        if (!isLikelyPaymentSms(sender, fullMessage)) {
            return
        }

        Log.d(TAG, "Payment SMS from $sender: ${fullMessage.take(100)}")

        // Process in background
        receiverScope.launch {
            try {
                val app = context.applicationContext as FinanceObserverApp
                val parsedEvent = app.parserRegistry.parseSms(sender, fullMessage)

                // Check for duplicates
                val isDuplicate = app.transactionRepository.isDuplicate(parsedEvent.merchant, parsedEvent.amount, lookbackMinutes = 5)

                if (!isDuplicate) {
                    val transactionId = app.transactionRepository.insertParsedEvent(parsedEvent)
                    Log.d(TAG, "Stored SMS transaction: id=$transactionId, merchant=${parsedEvent.merchant}, amount=${parsedEvent.amount}")

                    // Run detection engines
                    if (parsedEvent.isParsed) {
                        app.subscriptionDetector.analyzeNewTransaction(parsedEvent)
                        app.anomalyDetector.analyzeNewTransaction(parsedEvent)
                    }
                } else {
                    Log.d(TAG, "Duplicate SMS transaction, skipping: ${parsedEvent.merchant} ${parsedEvent.amount}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error processing SMS from $sender", e)
            }
        }
    }

    private fun isLikelyPaymentSms(sender: String, message: String): Boolean {
        // Check sender first
        if (sender in PAYMENT_SENDERS) {
            return true
        }

        // Check message content for payment keywords
        return PAYMENT_KEYWORDS.any { keyword ->
            message.contains(keyword, ignoreCase = true)
        }
    }
}
