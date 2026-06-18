package com.financeobserver.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.financeobserver.R
import com.financeobserver.database.NotificationDao
import com.financeobserver.model.NotificationStatus
import com.financeobserver.model.ParsedEvent
import com.financeobserver.model.TransactionNotification
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class TransactionNotifier(
    private val context: Context,
    private val notificationDao: NotificationDao
) {
    companion object {
        private const val TAG = "TransactionNotifier"
        const val CHANNEL_ID = "transaction_alerts"
        const val CHANNEL_NAME = "Transaction Alerts"

        private const val NOTIFICATION_ID_BASE = 2000
    }

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications for newly captured financial transactions"
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    suspend fun notifyTransactionCreated(transactionId: Long, event: ParsedEvent) {
        withContext(Dispatchers.IO) {
            try {
                val existing = notificationDao.findByTransactionAndRecipient(transactionId)
                if (existing != null) {
                    Log.d(TAG, "Notification already exists for transaction $transactionId, skipping")
                    return@withContext
                }

                val notificationRecord = TransactionNotification(
                    transactionId = transactionId,
                    channel = "in_app",
                    status = NotificationStatus.PENDING
                )
                val notifId = notificationDao.insert(notificationRecord)

                sendAndroidNotification(transactionId, event)

                notificationDao.markSent(notifId)
                Log.d(TAG, "Notification sent for transaction $transactionId")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send notification for transaction $transactionId", e)
                try {
                    val record = notificationDao.findByTransactionAndRecipient(transactionId)
                    if (record != null) {
                        notificationDao.markFailed(
                            id = record.id,
                            errorMessage = "${e::class.simpleName}: ${e.message}"
                        )
                    }
                } catch (inner: Exception) {
                    Log.e(TAG, "Failed to record notification failure", inner)
                }
            }
        }
    }

    private fun sendAndroidNotification(transactionId: Long, event: ParsedEvent) {
        val merchant = event.merchant ?: "Unknown"
        val amount = event.amount?.let { String.format("%.2f", it) } ?: "?"
        val currency = event.currency ?: "USD"

        val title = "New Transaction Captured"
        val text = "$currency $amount at $merchant"

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID_BASE + (transactionId % Int.MAX_VALUE).toInt(), notification)
    }
}
