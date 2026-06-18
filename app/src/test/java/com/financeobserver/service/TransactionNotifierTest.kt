package com.financeobserver.service

import com.financeobserver.model.NotificationStatus
import com.financeobserver.model.SourceType
import com.financeobserver.model.TransactionNotification
import com.financeobserver.model.ParsedEvent
import org.junit.Assert.*
import org.junit.Test
import java.util.Date

class TransactionNotifierTest {

    @Test
    fun `notification record has correct defaults`() {
        val record = TransactionNotification(
            transactionId = 42L
        )
        assertEquals(42L, record.transactionId)
        assertEquals("device_user", record.recipientId)
        assertEquals("in_app", record.channel)
        assertEquals(NotificationStatus.PENDING, record.status)
        assertEquals(0, record.attemptCount)
        assertNull(record.sentAt)
        assertNull(record.errorMessage)
        assertNull(record.lastAttemptAt)
    }

    @Test
    fun `notification record stores failure details`() {
        val record = TransactionNotification(
            transactionId = 7L,
            channel = "in_app",
            status = NotificationStatus.FAILED,
            errorMessage = "Network error",
            attemptCount = 3,
            lastAttemptAt = 1000L
        )
        assertEquals(NotificationStatus.FAILED, record.status)
        assertEquals("Network error", record.errorMessage)
        assertEquals(3, record.attemptCount)
        assertEquals(1000L, record.lastAttemptAt)
    }

    @Test
    fun `different statuses are distinct`() {
        assertNotEquals(NotificationStatus.PENDING, NotificationStatus.SENT)
        assertNotEquals(NotificationStatus.SENT, NotificationStatus.FAILED)
        assertNotEquals(NotificationStatus.FAILED, NotificationStatus.SUPPRESSED)
    }

    @Test
    fun `notifier dedup key prevents duplicates`() {
        val event1 = ParsedEvent(
            source = SourceType.NOTIFICATION,
            sourceApp = "com.chase.mobile",
            rawText = "Spent \$47.32 at TARGET",
            capturedAt = Date(),
            merchant = "TARGET",
            amount = 47.32,
            currency = "USD",
            date = Date(),
            isParsed = true
        )
        val event2 = ParsedEvent(
            source = SourceType.SMS,
            sourceApp = null,
            senderNumber = "Chase",
            rawText = "Spent \$47.32 at TARGET",
            capturedAt = Date(),
            merchant = "TARGET",
            amount = 47.32,
            currency = "USD",
            date = Date(),
            isParsed = true
        )
        assertEquals(
            "Same merchant+amount+date produce same dedup key",
            event1.computeDedupKey(),
            event2.computeDedupKey()
        )
    }

    @Test
    fun `different events have different dedup keys`() {
        val event1 = ParsedEvent(
            source = SourceType.NOTIFICATION,
            sourceApp = null,
            rawText = "",
            capturedAt = Date(),
            merchant = "NETFLIX",
            amount = 15.99,
            date = Date(0),
            isParsed = true
        )
        val event2 = ParsedEvent(
            source = SourceType.NOTIFICATION,
            sourceApp = null,
            rawText = "",
            capturedAt = Date(),
            merchant = "SPOTIFY",
            amount = 9.99,
            date = Date(0),
            isParsed = true
        )
        assertNotEquals(
            "Different merchants and amounts should have different keys",
            event1.computeDedupKey(),
            event2.computeDedupKey()
        )
    }

    @Test
    fun `notification can be created for any valid transaction id`() {
        val records = listOf(
            TransactionNotification(transactionId = 1L),
            TransactionNotification(transactionId = Long.MAX_VALUE),
            TransactionNotification(transactionId = 999999L)
        )
        records.forEach { record ->
            assertTrue("Transaction ID must be positive", record.transactionId > 0)
        }
    }

    @Test
    fun `notification records with same transaction id are distinguished by id`() {
        val record1 = TransactionNotification(id = 1, transactionId = 42L)
        val record2 = TransactionNotification(id = 2, transactionId = 42L)
        assertNotEquals("Same transaction can have multiple notification attempts", record1.id, record2.id)
    }
}
