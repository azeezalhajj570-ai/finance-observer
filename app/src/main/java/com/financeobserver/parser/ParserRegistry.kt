package com.financeobserver.parser

import com.financeobserver.model.ParsedEvent
import com.financeobserver.model.SourceType
import java.util.Date

/**
 * Central registry for all payment parsers.
 * 
 * This is the make-or-break component of the architecture.
 * It manages parser registration, selection, and fallback.
 * 
 * Design decisions:
 * - Parsers are tried in priority order (highest first)
 * - First successful parse wins
 * - If no parser succeeds, falls back to GenericParser
 * - All parse attempts are logged for analytics
 */
class ParserRegistry {
    private val parsers = mutableListOf<PaymentParser>()
    private val parseLog = mutableListOf<ParseLogEntry>()

    init {
        // Register built-in parsers
        registerParser(ArabicPaymentParser())
        registerParser(ChaseParser())
        registerParser(BankOfAmericaParser())
        registerParser(VenmoParser())
        registerParser(PayPalParser())
        registerParser(GooglePayParser())
        registerParser(ApplePayParser())
        registerParser(CashAppParser())
        registerParser(ZelleParser())
        registerParser(StripeParser())
        registerParser(SquareParser())

        // Generic fallback parser (always last)
        registerParser(GenericParser())
    }

    /**
     * Register a new parser. Inserted in priority order.
     */
    fun registerParser(parser: PaymentParser) {
        // Remove existing parser with same ID if present
        parsers.removeAll { it.parserId == parser.parserId }
        
        // Insert in priority order
        val insertIndex = parsers.indexOfFirst { it.priority < parser.priority }
        if (insertIndex >= 0) {
            parsers.add(insertIndex, parser)
        } else {
            parsers.add(parser)
        }
    }

    /**
     * Parse a notification by trying all registered parsers in priority order.
     */
    fun parseNotification(
        packageName: String,
        title: String?,
        text: String
    ): ParsedEvent {
        val startTime = System.currentTimeMillis()

        // Try each parser in priority order
        for (parser in parsers) {
            try {
                if (parser.canParseNotification(packageName, title, text)) {
                    val result = parser.parseNotification(packageName, title, text)
                    if (result != null) {
                        logParse(
                            parserId = parser.parserId,
                            source = SourceType.NOTIFICATION,
                            success = true,
                            duration = System.currentTimeMillis() - startTime,
                            packageName = packageName
                        )
                        return result
                    }
                }
            } catch (e: Exception) {
                // Parser crashed - log and continue to next parser
                logParseError(
                    parserId = parser.parserId,
                    error = e,
                    rawText = text
                )
            }
        }

        // No parser succeeded - return unparsed event
        logParse(
            parserId = "none",
            source = SourceType.NOTIFICATION,
            success = false,
            duration = System.currentTimeMillis() - startTime,
            packageName = packageName
        )

        return ParsedEvent(
            source = SourceType.NOTIFICATION,
            sourceApp = packageName,
            senderNumber = null,
            rawText = text,
            capturedAt = Date(),
            isParsed = false,
            confidenceScore = 0f
        )
    }

    /**
     * Parse an SMS by trying all registered parsers in priority order.
     */
    fun parseSms(
        sender: String,
        text: String
    ): ParsedEvent {
        val startTime = System.currentTimeMillis()

        for (parser in parsers) {
            try {
                if (parser.canParseSms(sender, text)) {
                    val result = parser.parseSms(sender, text)
                    if (result != null) {
                        logParse(
                            parserId = parser.parserId,
                            source = SourceType.SMS,
                            success = true,
                            duration = System.currentTimeMillis() - startTime,
                            packageName = sender
                        )
                        return result
                    }
                }
            } catch (e: Exception) {
                logParseError(
                    parserId = parser.parserId,
                    error = e,
                    rawText = text
                )
            }
        }

        logParse(
            parserId = "none",
            source = SourceType.SMS,
            success = false,
            duration = System.currentTimeMillis() - startTime,
            packageName = sender
        )

        return ParsedEvent(
            source = SourceType.SMS,
            sourceApp = null,
            senderNumber = sender,
            rawText = text,
            capturedAt = Date(),
            isParsed = false,
            confidenceScore = 0f
        )
    }

    /**
     * Get statistics about parser performance.
     */
    fun getStats(): ParserStats {
        val totalAttempts = parseLog.size
        val successfulParses = parseLog.count { it.success }
        val failedParses = totalAttempts - successfulParses

        val parserUsage = parseLog
            .groupBy { it.parserId }
            .mapValues { (_, logs) -> logs.count { it.success } }

        return ParserStats(
            totalAttempts = totalAttempts,
            successfulParses = successfulParses,
            failedParses = failedParses,
            successRate = if (totalAttempts > 0) successfulParses.toFloat() / totalAttempts else 0f,
            parserUsage = parserUsage,
            registeredParsers = parsers.size
        )
    }

    private fun logParse(
        parserId: String,
        source: SourceType,
        success: Boolean,
        duration: Long,
        packageName: String
    ) {
        parseLog.add(
            ParseLogEntry(
                parserId = parserId,
                source = source,
                success = success,
                durationMs = duration,
                packageName = packageName,
                timestamp = Date()
            )
        )

        // Keep only last 1000 entries to prevent memory growth
        if (parseLog.size > 1000) {
            parseLog.subList(0, parseLog.size - 1000).clear()
        }
    }

    private fun logParseError(parserId: String, error: Exception, rawText: String) {
        android.util.Log.e(
            "ParserRegistry",
            "Parser $parserId crashed: ${error.message}",
            error
        )
    }
}

data class ParseLogEntry(
    val parserId: String,
    val source: SourceType,
    val success: Boolean,
    val durationMs: Long,
    val packageName: String,
    val timestamp: Date
)

data class ParserStats(
    val totalAttempts: Int,
    val successfulParses: Int,
    val failedParses: Int,
    val successRate: Float,
    val parserUsage: Map<String, Int>,
    val registeredParsers: Int
)
