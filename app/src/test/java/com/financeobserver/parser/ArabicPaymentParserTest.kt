package com.financeobserver.parser

import com.financeobserver.model.SourceType
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class ArabicPaymentParserTest {

    private lateinit var registry: ParserRegistry

    @Before
    fun setUp() {
        registry = ParserRegistry()
    }

    @Test
    fun `Arabic SMS - added deposit`() {
        val event = registry.parseSms(
            sender = "YKB",
            text = "اضيف 8000ر.ي تحويل مشترك رص:8009.4ر.ي من محمد"
        )

        assertTrue("Should be parsed", event.isParsed)
        assertEquals(8000.0, event.amount!!, 0.01)
        assertEquals("YER", event.currency)
    }

    @Test
    fun `Arabic SMS - deduction`() {
        val event = registry.parseSms(
            sender = "CAC Bank",
            text = "خصم 39300ر.ي إرسال حوالة شبكة تحويل رص:5809.4ر.ي"
        )

        assertTrue("Should be parsed", event.isParsed)
        assertEquals(39300.0, event.amount!!, 0.01)
    }

    @Test
    fun `Arabic SMS - transfer to account`() {
        val event = registry.parseSms(
            sender = "Bank",
            text = "تم تحويل4,100.00لحساب أحمد رصيدك279.04YER"
        )

        assertTrue("Should be parsed", event.isParsed)
        assertEquals(4100.0, event.amount!!, 0.01)
    }

    @Test
    fun `Arabic SMS - received money`() {
        val event = registry.parseSms(
            sender = "Bank",
            text = "استلمت مبلغ 5000 YER من Ali"
        )

        assertTrue("Should be parsed", event.isParsed)
        assertEquals(5000.0, event.amount!!, 0.01)
    }

    @Test
    fun `Arabic SMS - paid to merchant`() {
        val event = registry.parseSms(
            sender = "Bank",
            text = "تم دفع 150 ر.ي ل متجر الإلكتروني"
        )

        assertTrue("Should be parsed", event.isParsed)
        assertEquals(150.0, event.amount!!, 0.01)
    }

    @Test
    fun `Arabic SMS - USD deduction`() {
        val event = registry.parseSms(
            sender = "Bank",
            text = "تم خصم USD 52.4 مقابل عملية شراء"
        )

        assertTrue("Should be parsed", event.isParsed)
        assertEquals(52.4, event.amount!!, 0.01)
        assertEquals("USD", event.currency)
    }

    @Test
    fun `Arabic SMS - recharge`() {
        val event = registry.parseSms(
            sender = "Bank",
            text = "تم شحن رصيدك بمبلغ 600 ر.ي"
        )

        assertTrue("Should be parsed", event.isParsed)
        assertEquals(600.0, event.amount!!, 0.01)
    }

    @Test
    fun `Arabic SMS - withdrew`() {
        val event = registry.parseSms(
            sender = "Bank",
            text = "سحبت 1000 ر.ي من صراف الأهلي"
        )

        assertTrue("Should be parsed", event.isParsed)
        assertEquals(1000.0, event.amount!!, 0.01)
    }

    @Test
    fun `Arabic SMS - sent money`() {
        val event = registry.parseSms(
            sender = "Bank",
            text = "لقد قمت بإرسال 44700 ريال يمني"
        )

        assertTrue("Should be parsed", event.isParsed)
        assertEquals(44700.0, event.amount!!, 0.01)
    }

    @Test
    fun `Non-Arabic SMS is not parsed by Arabic parser`() {
        val event = registry.parseSms(
            sender = "Chase",
            text = "Purchase of \$45.00 at AMAZON"
        )

        assertTrue("Should be parsed by Chase or generic parser", event.isParsed)
        assertEquals(45.0, event.amount!!, 0.01)
    }

    @Test
    fun `Arabic decimal separator is handled`() {
        val event = registry.parseSms(
            sender = "Bank",
            text = "خصم 1500٫50 ر.ي مشتريات"
        )

        assertTrue("Should be parsed", event.isParsed)
        assertEquals(1500.50, event.amount!!, 0.01)
    }
}
