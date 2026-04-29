package com.financeobserver.model

import java.util.Date

/**
 * Raw event captured from a notification or SMS before normalization.
 * This is the intermediate step between capture and transaction storage.
 */
data class ParsedEvent(
    val source: SourceType,
    val sourceApp: String?,
    val senderNumber: String?,       // For SMS only
    val rawText: String,
    val capturedAt: Date = Date(),

    // Extracted fields (may be partial)
    val merchant: String? = null,
    val amount: Double? = null,
    val currency: String? = null,
    val date: Date? = null,

    // Parser metadata
    val parserName: String? = null,  // Which parser handled this
    val confidenceScore: Float = 0f,
    val isParsed: Boolean = false,

    // Dedup key (computed from merchant + amount + approximate time)
    val dedupKey: String? = null
) {
    /**
     * Generate a deduplication key for matching across sources.
     * Uses merchant name (normalized), amount, and date (rounded to hour).
     */
    fun computeDedupKey(): String {
        val merchantNorm = merchant?.lowercase()?.trim() ?: "unknown"
        val amountNorm = amount?.let { String.format("%.2f", it) } ?: "0.00"
        val dateNorm = date?.let { (it.time / 3600000).toString() } ?: "0"
        return "$merchantNorm|$amountNorm|$dateNorm"
    }
}
