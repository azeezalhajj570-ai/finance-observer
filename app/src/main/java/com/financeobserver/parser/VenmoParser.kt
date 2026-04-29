package com.financeobserver.parser

import com.financeobserver.model.ParsedEvent
import com.financeobserver.model.SourceType
import java.util.Date

/**
 * Parser for Venmo notifications and SMS.
 * 
 * Sample notification: "Venmo: John Smith paid you $25.00 for Dinner"
 * Sample notification: "Venmo: You paid Sarah Johnson $15.00 for Coffee"
 * Sample SMS: "Venmo: You received a payment of $25.00 from John Smith"
 */
class VenmoParser : PaymentParser {
    override val parserId = "venmo"
    override val supportedPackages = listOf(
        "com.venmo",
        "com.venmo.android"
    )
    override val supportedSenders = listOf("Venmo", "VENMO")
    override val priority = 85

    // Pattern: "paid you $XX.XX" or "You paid $XX.XX" or "received a payment of $XX.XX"
    private val paidYouPattern = Regex("(\\w+\\s+\\w+)\\s+paid\\s+you\\s+\\$?([\\d,]+\\.\\d{2})", RegexOption.IGNORE_CASE)
    private val youPaidPattern = Regex("You\\s+paid\\s+(\\w+\\s+\\w+)\\s+\\$?([\\d,]+\\.\\d{2})", RegexOption.IGNORE_CASE)
    private val receivedPattern = Regex("received\\s+a\\s+payment\\s+of\\s+\\$?([\\d,]+\\.\\d{2})\\s+from\\s+(\\w+\\s+\\w+)", RegexOption.IGNORE_CASE)
    private val sentPattern = Regex("sent\\s+\\$?([\\d,]+\\.\\d{2})\\s+to\\s+(\\w+\\s+\\w+)", RegexOption.IGNORE_CASE)

    override fun canParseNotification(
        packageName: String,
        title: String?,
        text: String
    ): Boolean {
        return packageName in supportedPackages ||
               title?.contains("Venmo", ignoreCase = true) == true ||
               text.contains("Venmo", ignoreCase = true)
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
        // "X paid you $Y" - money received
        paidYouPattern.find(text)?.let { match ->
            val person = match.groupValues[1]
            val amount = match.groupValues[2].replace(",", "").toDoubleOrNull() ?: return null
            return createEvent(text, sourceApp, sender, amount, "Venmo from $person", 0.95f)
        }

        // "You paid X $Y" - money sent
        youPaidPattern.find(text)?.let { match ->
            val person = match.groupValues[1]
            val amount = match.groupValues[2].replace(",", "").toDoubleOrNull() ?: return null
            return createEvent(text, sourceApp, sender, amount, "Venmo to $person", 0.95f)
        }

        // "received a payment of $Y from X"
        receivedPattern.find(text)?.let { match ->
            val amount = match.groupValues[1].replace(",", "").toDoubleOrNull() ?: return null
            val person = match.groupValues[2]
            return createEvent(text, sourceApp, sender, amount, "Venmo from $person", 0.9f)
        }

        // "sent $Y to X"
        sentPattern.find(text)?.let { match ->
            val amount = match.groupValues[1].replace(",", "").toDoubleOrNull() ?: return null
            val person = match.groupValues[2]
            return createEvent(text, sourceApp, sender, amount, "Venmo to $person", 0.9f)
        }

        return null
    }

    private fun createEvent(
        rawText: String,
        sourceApp: String?,
        sender: String?,
        amount: Double,
        merchant: String,
        confidence: Float
    ): ParsedEvent {
        return ParsedEvent(
            source = if (sourceApp != null) SourceType.NOTIFICATION else SourceType.SMS,
            sourceApp = sourceApp,
            senderNumber = sender,
            rawText = rawText,
            capturedAt = Date(),
            merchant = merchant,
            amount = amount,
            currency = "USD",
            date = Date(),
            parserName = parserId,
            confidenceScore = confidence,
            isParsed = true
        )
    }
}
