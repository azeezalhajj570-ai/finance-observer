package com.financeobserver.parser

import com.financeobserver.model.ParsedEvent
import com.financeobserver.model.SourceType
import com.financeobserver.model.TransactionType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.regex.Pattern

/**
 * Parser for Chase Bank notifications and SMS.
 * 
 * Sample notification: "Chase: You spent $47.32 at TARGET on your debit card ending 1234."
 * Sample SMS: "Chase: Purchase of $47.32 at TARGET on 04/28. Debit card ending 1234."
 */
class ChaseParser : PaymentParser {
    override val parserId = "chase"
    override val supportedPackages = listOf(
        "com.chase.mobile",
        "com.chase.sig.android"
    )
    override val supportedSenders = listOf("Chase", "CHASE", "ChaseAlerts")
    override val priority = 90

    // Pattern: "You spent $XX.XX at MERCHANT" or "Purchase of $XX.XX at MERCHANT"
    private val amountMerchantPattern = Pattern.compile(
        "(?:spent|purchase of|payment of|charge of)\\s+\\$?([\\d,]+\\.\\d{2})\\s+at\\s+([\\w\\s&]+?)(?:\\s+on|\\s+\\.|$)",
        Pattern.CASE_INSENSITIVE
    )

    // Pattern for refund: "refund of $XX.XX from MERCHANT"
    private val refundPattern = Pattern.compile(
        "refund (?:of )?\\$?([\\d,]+\\.\\d{2})\\s+(?:from|at)\\s+([\\w\\s&]+?)(?:\\s+on|\\s+\\.|$)",
        Pattern.CASE_INSENSITIVE
    )

    override fun canParseNotification(
        packageName: String,
        title: String?,
        text: String
    ): Boolean {
        return packageName in supportedPackages ||
               title?.contains("Chase", ignoreCase = true) == true ||
               text.contains("Chase", ignoreCase = true)
    }

    override fun canParseSms(sender: String, text: String): Boolean {
        return sender in supportedSenders
    }

    override fun parseNotification(
        packageName: String,
        title: String?,
        text: String
    ): ParsedEvent? {
        val combinedText = "${title ?: ""} $text"
        return parseText(combinedText, packageName)
    }

    override fun parseSms(
        sender: String,
        text: String
    ): ParsedEvent? {
        return parseText(text, null, sender)
    }

    private fun parseText(
        text: String,
        sourceApp: String? = null,
        sender: String? = null
    ): ParsedEvent? {
        // Check for refund first
        val refundMatch = refundPattern.matcher(text)
        if (refundMatch.find()) {
            val amount = refundMatch.group(1)?.replace(",", "")?.toDoubleOrNull() ?: return null
            val merchant = refundMatch.group(2)?.trim() ?: "Unknown"

            return ParsedEvent(
                source = if (sourceApp != null) SourceType.NOTIFICATION else SourceType.SMS,
                sourceApp = sourceApp,
                senderNumber = sender,
                rawText = text,
                capturedAt = Date(),
                merchant = merchant,
                amount = amount,
                currency = "USD",
                date = Date(),
                parserName = parserId,
                confidenceScore = 0.9f,
                isParsed = true
            )
        }

        // Check for purchase
        val match = amountMerchantPattern.matcher(text)
        if (match.find()) {
            val amount = match.group(1)?.replace(",", "")?.toDoubleOrNull() ?: return null
            val merchant = match.group(2)?.trim() ?: "Unknown"

            // Determine transaction type from context
            val transactionType = when {
                text.contains("refund", ignoreCase = true) -> TransactionType.REFUND
                text.contains("payment", ignoreCase = true) -> TransactionType.PURCHASE
                text.contains("transfer", ignoreCase = true) -> TransactionType.TRANSFER_OUT
                else -> TransactionType.PURCHASE
            }

            return ParsedEvent(
                source = if (sourceApp != null) SourceType.NOTIFICATION else SourceType.SMS,
                sourceApp = sourceApp,
                senderNumber = sender,
                rawText = text,
                capturedAt = Date(),
                merchant = merchant,
                amount = amount,
                currency = "USD",
                date = Date(),
                parserName = parserId,
                confidenceScore = 0.95f,
                isParsed = true
            )
        }

        // Couldn't parse with specific patterns
        return null
    }
}
