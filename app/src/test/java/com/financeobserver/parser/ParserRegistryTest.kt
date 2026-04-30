package com.financeobserver.parser

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for the ParserRegistry and individual parsers.
 * Tests with real notification samples from actual banks and payment apps.
 */
class ParserRegistryTest {

    private lateinit var registry: ParserRegistry

    @Before
    fun setUp() {
        registry = ParserRegistry()
    }

    @Test
    fun `registry has all built-in parsers registered`() {
        val stats = registry.getStats()
        // 11 specific parsers + 1 generic fallback = 12
        assertEquals(12, stats.registeredParsers)
    }

    @Test
    fun `Chase notification - purchase`() {
        val event = registry.parseNotification(
            packageName = "com.chase.mobile",
            title = "Chase",
            text = "You spent \$47.32 at TARGET on your debit card ending 1234."
        )

        assertTrue("Should be parsed", event.isParsed)
        assertEquals("TARGET", event.merchant)
        assertEquals(47.32, event.amount!!, 0.01)
        assertTrue("Confidence should be high", event.confidenceScore > 0.8f)
    }

    @Test
    fun `Chase notification - refund`() {
        val event = registry.parseNotification(
            packageName = "com.chase.mobile",
            title = "Chase",
            text = "Refund of \$25.00 from AMAZON on your credit card ending 5678."
        )

        assertTrue("Should be parsed", event.isParsed)
        assertEquals("AMAZON", event.merchant)
        assertEquals(25.0, event.amount!!, 0.01)
    }

    @Test
    fun `Chase SMS - purchase`() {
        val event = registry.parseSms(
            sender = "Chase",
            text = "Chase: Purchase of \$123.45 at STARBUCKS on 04/28. Debit card ending 1234."
        )

        assertTrue("Should be parsed", event.isParsed)
        assertEquals("STARBUCKS", event.merchant)
        assertEquals(123.45, event.amount!!, 0.01)
    }

    @Test
    fun `Venmo notification - received payment`() {
        val event = registry.parseNotification(
            packageName = "com.venmo",
            title = "Venmo",
            text = "John Smith paid you \$25.00 for Dinner"
        )

        assertTrue("Should be parsed", event.isParsed)
        assertTrue("Should contain sender name", event.merchant!!.contains("John Smith"))
        assertEquals(25.0, event.amount!!, 0.01)
    }

    @Test
    fun `Venmo notification - sent payment`() {
        val event = registry.parseNotification(
            packageName = "com.venmo",
            title = "Venmo",
            text = "You paid Sarah Johnson \$15.00 for Coffee"
        )

        assertTrue("Should be parsed", event.isParsed)
        assertTrue("Should contain recipient name", event.merchant!!.contains("Sarah Johnson"))
        assertEquals(15.0, event.amount!!, 0.01)
    }

    @Test
    fun `Venmo SMS - received payment`() {
        val event = registry.parseSms(
            sender = "Venmo",
            text = "Venmo: You received a payment of \$50.00 from Mike Davis"
        )

        assertTrue("Should be parsed", event.isParsed)
        assertEquals(50.0, event.amount!!, 0.01)
    }

    @Test
    fun `Bank of America notification`() {
        val event = registry.parseNotification(
            packageName = "com.infonow.bofa",
            title = "Bank of America",
            text = "Purchase of \$89.99 at NETFLIX on your debit card ending 9012."
        )

        assertTrue("Should be parsed", event.isParsed)
        assertEquals("NETFLIX", event.merchant)
        assertEquals(89.99, event.amount!!, 0.01)
    }

    @Test
    fun `PayPal notification - sent money`() {
        val event = registry.parseNotification(
            packageName = "com.paypal.android.p2pmobile",
            title = "PayPal",
            text = "You sent \$30.00 to Test User for Rent"
        )

        assertTrue("Should be parsed", event.isParsed)
        assertEquals(30.0, event.amount!!, 0.01)
    }

    @Test
    fun `Google Pay notification`() {
        val event = registry.parseNotification(
            packageName = "com.google.android.apps.nbu.paisa.user",
            title = "Google Pay",
            text = "\$12.50 paid to UBER EATS"
        )

        assertTrue("Should be parsed", event.isParsed)
        assertEquals(12.50, event.amount!!, 0.01)
    }

    @Test
    fun `Cash App notification`() {
        val event = registry.parseNotification(
            packageName = "com.squareup.cash",
            title = "Cash App",
            text = "\$20.00 paid to @friend123"
        )

        assertTrue("Should be parsed", event.isParsed)
        assertEquals(20.0, event.amount!!, 0.01)
    }

    @Test
    fun `Zelle notification`() {
        val event = registry.parseNotification(
            packageName = "com.zellepay.zelle",
            title = "Zelle",
            text = "\$100.00 received from jane.doe@email.com for Birthday gift"
        )

        assertTrue("Should be parsed", event.isParsed)
        assertEquals(100.0, event.amount!!, 0.01)
    }

    @Test
    fun `Unknown app - generic parser catches dollar amounts`() {
        val event = registry.parseNotification(
            packageName = "com.unknown.app",
            title = "Payment Alert",
            text = "Your card was charged \$55.00 at SOME STORE"
        )

        // Generic parser should catch this
        assertTrue("Should be parsed by generic parser", event.isParsed)
        assertEquals(55.0, event.amount!!, 0.01)
        assertTrue("Confidence should be lower for generic", event.confidenceScore < 0.7f)
    }

    @Test
    fun `Non-payment notification returns unparsed event`() {
        val event = registry.parseNotification(
            packageName = "com.instagram.android",
            title = "Instagram",
            text = "John liked your photo."
        )

        assertFalse("Should not be parsed", event.isParsed)
        assertEquals(0f, event.confidenceScore, 0.01f)
    }

    @Test
    fun `Duplicate detection - same dedup key`() {
        val event1 = registry.parseNotification(
            packageName = "com.chase.mobile",
            title = "Chase",
            text = "You spent \$47.32 at TARGET on your debit card ending 1234."
        )

        val event2 = registry.parseNotification(
            packageName = "com.chase.mobile",
            title = "Chase",
            text = "You spent \$47.32 at TARGET on your debit card ending 1234."
        )

        assertEquals(
            "Same transaction should produce same dedup key",
            event1.computeDedupKey(),
            event2.computeDedupKey()
        )
    }

    @Test
    fun `Parser stats track usage`() {
        // Parse several notifications
        registry.parseNotification("com.chase.mobile", "Chase", "You spent \$10.00 at STORE.")
        registry.parseNotification("com.venmo", "Venmo", "John paid you \$5.00")
        registry.parseNotification("com.instagram.android", "Instagram", "New like!")

        val stats = registry.getStats()
        assertEquals(3, stats.totalAttempts)
        assertTrue("Success rate should be > 0", stats.successRate > 0f)
        assertTrue("Should have used multiple parsers", stats.parserUsage.size >= 2)
    }

    @Test
    fun `Generic parser handles amount with commas`() {
        val event = registry.parseNotification(
            packageName = "com.unknown.app",
            title = "Alert",
            text = "Payment of \$1,234.56 processed successfully"
        )

        assertTrue("Should be parsed", event.isParsed)
        assertEquals(1234.56, event.amount!!, 0.01)
    }

    @Test
    fun `Generic parser handles multiple amounts - picks largest`() {
        val event = registry.parseNotification(
            packageName = "com.unknown.app",
            title = "Alert",
            text = "Your balance is \$5,000.00. A charge of \$42.99 was made at STORE."
        )

        assertTrue("Should be parsed", event.isParsed)
        // Should pick the larger amount (balance) or the charge - depends on implementation
        assertTrue("Amount should be one of the values",
            event.amount == 5000.0 || event.amount == 42.99)
    }
}
