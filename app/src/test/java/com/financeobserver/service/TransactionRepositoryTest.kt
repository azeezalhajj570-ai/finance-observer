package com.financeobserver.service

import com.financeobserver.model.ParsedEvent
import com.financeobserver.model.SourceType
import org.junit.Assert.*
import org.junit.Test
import java.util.Date

class TransactionRepositoryTest {

    @Test
    fun `insertParsedEvent maps fields correctly`() {
        val event = ParsedEvent(
            source = SourceType.NOTIFICATION,
            sourceApp = "com.chase.mobile",
            senderNumber = null,
            rawText = "Spent \$47.32 at TARGET",
            capturedAt = Date(),
            merchant = "TARGET",
            amount = 47.32,
            currency = "USD",
            date = Date(),
            parserName = "chase",
            confidenceScore = 0.9f,
            isParsed = true
        )
        assertEquals("TARGET", event.merchant)
        assertEquals(47.32, event.amount!!, 0.01)
        assertEquals("USD", event.currency)
        assertTrue(event.isParsed)
    }

    @Test
    fun `unparsed event has confidenceScore of 0`() {
        val event = ParsedEvent(
            source = SourceType.NOTIFICATION,
            sourceApp = null,
            senderNumber = null,
            rawText = "Some random text",
            capturedAt = Date(),
            isParsed = false,
            confidenceScore = 0f
        )
        assertFalse(event.isParsed)
        assertEquals(0f, event.confidenceScore, 0.01f)
    }

    @Test
    fun `computeDedupKey normalizes merchant and amounts`() {
        val event1 = ParsedEvent(
            source = SourceType.NOTIFICATION,
            sourceApp = "com.chase.mobile",
            rawText = "Spent \$10.00",
            capturedAt = Date(),
            merchant = "NETFLIX",
            amount = 10.00,
            date = Date(0),
            isParsed = true
        )
        val event2 = ParsedEvent(
            source = SourceType.SMS,
            sourceApp = null,
            senderNumber = "Chase",
            rawText = "Spent \$10.00",
            capturedAt = Date(),
            merchant = "Netflix",
            amount = 10.00,
            date = Date(0),
            isParsed = true
        )
        assertEquals(
            "Should produce same dedup key regardless of case",
            event1.computeDedupKey(),
            event2.computeDedupKey()
        )
    }

    @Test
    fun `computeDedupKey differs for different merchants`() {
        val event1 = ParsedEvent(
            source = SourceType.NOTIFICATION,
            sourceApp = "com.chase.mobile",
            rawText = "",
            capturedAt = Date(),
            merchant = "NETFLIX",
            amount = 10.00,
            date = Date(0),
            isParsed = true
        )
        val event2 = ParsedEvent(
            source = SourceType.NOTIFICATION,
            sourceApp = "com.chase.mobile",
            rawText = "",
            capturedAt = Date(),
            merchant = "AMAZON",
            amount = 10.00,
            date = Date(0),
            isParsed = true
        )
        assertNotEquals(
            "Different merchants should have different keys",
            event1.computeDedupKey(),
            event2.computeDedupKey()
        )
    }
}
