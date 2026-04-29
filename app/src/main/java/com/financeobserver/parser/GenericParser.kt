package com.financeobserver.parser

import com.financeobserver.model.ParsedEvent
import com.financeobserver.model.SourceType
import java.util.Date

/**
 * Generic fallback parser that attempts to extract transaction data from ANY notification/SMS.
 * This is the last parser tried - it catches transactions from apps without specific parsers.
 * 
 * Strategy: Look for dollar amounts and nearby merchant-like text.
 * Confidence is lower than app-specific parsers.
 */
class GenericParser : PaymentParser {
    override val parserId = "generic"
    override val supportedPackages = listOf("*")  // Matches all packages
    override val supportedSenders = listOf("*")   // Matches all senders
    override val priority = 0  // Always tried last

    // Match dollar amounts: $12.34, $1,234.56, 12.34 USD
    private val dollarPattern = Regex("\\$?([\\d,]+\\.\\d{2})\\s*(?:USD|EUR|GBP)?", RegexOption.IGNORE_CASE)

    // Common payment keywords that suggest a transaction
    private val paymentKeywords = listOf(
        "paid", "payment", "purchase", "charge", "spent", "bought",
        "received", "sent", "transfer", "deposit", "withdrawal",
        "subscription", "renewal", "auto-pay", "autopay"
    )

    override fun canParseNotification(
        packageName: String,
        title: String?,
        text: String
    ): Boolean {
        // Always returns true - this is the fallback
        return hasPaymentIndication(text) || hasPaymentIndication(title ?: "")
    }

    override fun canParseSms(
        sender: String,
        text: String
    ): Boolean {
        return hasPaymentIndication(text)
    }

    override fun parseNotification(
        packageName: String,
        title: String?,
        text: String
    ): ParsedEvent? {
        val combinedText = "${title ?: ""} $text".trim()
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
        // Find dollar amounts
        val amounts = dollarPattern.findAll(text).toList()
        if (amounts.isEmpty()) return null

        // Take the largest amount (usually the transaction amount)
        val amount = amounts
            .mapNotNull { it.groupValues[1].replace(",", "").toDoubleOrNull() }
            .maxOrNull() ?: return null

        // Try to extract merchant name
        val merchant = extractMerchantName(text, amount)

        // Determine if this is incoming or outgoing
        val isIncoming = text.contains("received", ignoreCase = true) ||
                         text.contains("paid you", ignoreCase = true) ||
                         text.contains("deposit", ignoreCase = true)

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
            confidenceScore = 0.5f,  // Low confidence - generic parse
            isParsed = true
        )
    }

    private fun hasPaymentIndication(text: String): Boolean {
        return paymentKeywords.any { text.contains(it, ignoreCase = true) } ||
               dollarPattern.containsMatchIn(text)
    }

    private fun extractMerchantName(text: String, amount: Double): String {
        // Strategy: Look for text near the dollar amount
        val dollarIndex = text.indexOf("$")
        if (dollarIndex < 0) return "Unknown"

        // Look for "at MERCHANT" pattern
        val atPattern = Regex("at\\s+([A-Z][A-Za-z\\s&]+?)(?:\\s+on|\\s+\\.|\\s+for|$)")
        atPattern.find(text)?.let { match ->
            val name = match.groupValues[1].trim()
            if (name.length > 1 && name.length < 50) return name
        }

        // Look for "from MERCHANT" pattern
        val fromPattern = Regex("from\\s+([A-Z][A-Za-z\\s&]+?)(?:\\s+on|\\s+\\.|\\s+for|$)")
        fromPattern.find(text)?.let { match ->
            val name = match.groupValues[1].trim()
            if (name.length > 1 && name.length < 50) return name
        }

        // Fallback: use the source app name or sender
        // Remove common words and try to find a proper noun
        val words = text.split(" ")
            .filter { it.length > 2 }
            .filter { it[0].isUpperCase() }
            .filter { !it.matches(Regex("^(You|Your|The|And|For|On|At|In|With|This|That|Have|Has|Was|Were|Are|Is|Am|Not|But|Or|Nor)$")) }

        return if (words.isNotEmpty()) {
            words.take(3).joinToString(" ")
        } else {
            "Unknown"
        }
    }
}
