package com.financeobserver.parser

import com.financeobserver.model.ParsedEvent
import com.financeobserver.model.SourceType

/**
 * Interface for all notification/SMS parsers.
 * Each parser handles a specific app's notification format.
 * 
 * New parsers can be added without modifying existing code (Open/Closed Principle).
 * Parsers are registered in the ParserRegistry and tried in priority order.
 */
interface PaymentParser {
    /**
     * Unique identifier for this parser.
     * Used for logging, analytics, and remote parser updates.
     */
    val parserId: String

    /**
     * App package names this parser handles.
     * e.g., listOf("com.chase.mobile", "com.chase.sig.android")
     */
    val supportedPackages: List<String>

    /**
     * SMS sender patterns this parser handles.
     * e.g., listOf("Chase", "CHASE")
     */
    val supportedSenders: List<String>

    /**
     * Priority for parser ordering. Higher = tried first.
     * Use 1-100 for app-specific parsers, 0 for fallback.
     */
    val priority: Int

    /**
     * Check if this parser can handle the given notification.
     * Quick check before attempting full parse.
     */
    fun canParseNotification(
        packageName: String,
        title: String?,
        text: String
    ): Boolean

    /**
     * Check if this parser can handle the given SMS.
     */
    fun canParseSms(
        sender: String,
        text: String
    ): Boolean

    /**
     * Parse a notification into a ParsedEvent.
     * Returns null if parsing fails.
     */
    fun parseNotification(
        packageName: String,
        title: String?,
        text: String
    ): ParsedEvent?

    /**
     * Parse an SMS into a ParsedEvent.
     * Returns null if parsing fails.
     */
    fun parseSms(
        sender: String,
        text: String
    ): ParsedEvent?
}

/**
 * Result of attempting to parse an event.
 */
sealed class ParseResult {
    data class Success(val event: ParsedEvent) : ParseResult()
    data class Failure(val reason: String, val rawText: String) : ParseResult()
    object NoParserFound : ParseResult()
}
