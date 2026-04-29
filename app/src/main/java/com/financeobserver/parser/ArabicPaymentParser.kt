package com.financeobserver.parser

import com.financeobserver.model.ParsedEvent
import com.financeobserver.model.SourceType
import java.util.Date

/**
 * Parser for Arabic/Yemeni bank SMS messages and notifications.
 * Handles patterns from Yemeni banks (YKB, CAC Bank, etc.) and mobile wallets.
 * 
 * Supported patterns:
 * - اضيف (added/deposited): "اضيف 8000ر.ي تحويل مشترك رص:8009.4ر.ي من [name]"
 * - خصم (deducted): "خصم 39300ر.ي إرسال حوالة شبكة تحويل رص:5809.4ر.ي"
 * - تم تحويل (transferred): "تم تحويل4,100.00لحساب [name] رصيدك279.04YER"
 * - تم خصم (deducted): "تم خصم مبلغ 1,200.00 YER مقابل مشتريات"
 * - تم دفع (paid): "تم دفع 150 ر.ي ل [merchant]"
 * - استلمت (received): "استلمت 10000ر.ي من [name]"
 * - تم إيداع (deposited): "تم إيداع 50000ر.ي عبر الوكيل"
 * - سحبت (withdrew): "سحبت 1000 ر.ي من [bank]"
 * - لقد قمت بإرسال (you sent): "لقد قمت بإرسال 44700 ريال يمني"
 * - استلمت مبلغ (received amount): "استلمت مبلغ 5000 YER من [number]"
 * - تم استلام (received): "تم استلام 29700 YER حوالة رقم [number]"
 * - تم خصم USD (deducted USD): "تم خصم USD 52.4 مقابل عملية شراء"
 * - تم شحن رصيدك (recharged): "تم شحن رصيدك بمبلغ 600 ر.ي"
 */
class ArabicPaymentParser : PaymentParser {
    override val parserId = "arabic_payment"
    override val supportedPackages = listOf("*")
    override val supportedSenders = listOf("*")
    override val priority = 95  // Highest priority - try before US parsers

    // Amount patterns for Yemeni Rial (ر.ي), Saudi Riyal (ر.س), YER, SAR, USD
    // Matches numbers like: 8000, 8,000, 8000.4, 50,000.00, 500
    private val amountPattern = Regex("([\\d,]+(?:\\.\\d{1,2})?)")

    // Pattern: "اضيف XXXXر.ي ... رص:XXXXر.ي من [name]"
    private val addedPattern = Regex(
        "اضيف\\s+([\\d,]+(?:\\.\\d{1,2})?)\\s*(?:ر\\.ي|YER|ر\\.س|SAR)",
        RegexOption.IGNORE_CASE
    )

    // Pattern: "خصم XXXXر.ي ... رص:XXXXر.ي ..."
    private val deductedPattern = Regex(
        "خصم\\s+([\\d,]+(?:\\.\\d{1,2})?)\\s*(?:ر\\.ي|YER|ر\\.س|SAR)",
        RegexOption.IGNORE_CASE
    )

    // Pattern: "تم تحويلX,XXX.XXلحساب [name]"
    private val transferredPattern = Regex(
        "تم\\s*تحويل\\s*([\\d,]+(?:\\.\\d{1,2})?)\\s*(?:ر\\.ي|YER|ر\\.س|SAR)?\\s*لحساب\\s*(.+)",
        RegexOption.IGNORE_CASE
    )

    // Pattern: "تم خصم مبلغ X,XXX.XX YER مقابل ..." or "تم خصم USD XX.X مقابل"
    private val deductedDetailPattern = Regex(
        "تم\\s*خصم\\s*(?:مبلغ\\s*)?(?:USD\\s*)?([\\d,]+(?:\\.\\d{1,2})?)\\s*(?:ر\\.ي|YER|ر\\.س|SAR|USD)?",
        RegexOption.IGNORE_CASE
    )

    // Pattern: "تم دفع XXX ر.ي ل [merchant]"
    private val paidPattern = Regex(
        "تم\\s*دفع\\s+([\\d,]+(?:\\.\\d{1,2})?)\\s*(?:ر\\.ي|YER|ر\\.س|SAR)\\s+ل\\s*(.+)",
        RegexOption.IGNORE_CASE
    )

    // Pattern: "استلمت XXXXر.ي من [name]" or "استلمت مبلغ XXXX YER من [name]"
    private val receivedPattern = Regex(
        "استلمت\\s+(?:مبلغ\\s+)?([\\d,]+(?:\\.\\d{1,2})?)\\s*(?:ر\\.ي|YER|ر\\.س|SAR)\\s+من\\s*(.+)",
        RegexOption.IGNORE_CASE
    )

    // Pattern: "تم إيداع XXXXر.ي عبر [agent]" or "تم إيداع ... من [name]"
    private val depositedPattern = Regex(
        "تم\\s*إيداع\\s+([\\d,]+(?:\\.\\d{1,2})?)\\s*(?:ر\\.ي|YER|ر\\.س|SAR)",
        RegexOption.IGNORE_CASE
    )

    // Pattern: "سحبت XXXX ر.ي من [bank]"
    private val withdrewPattern = Regex(
        "سحبت\\s+([\\d,]+(?:\\.\\d{1,2})?)\\s*(?:ر\\.ي|YER|ر\\.س|SAR)\\s+من\\s*(.+)",
        RegexOption.IGNORE_CASE
    )

    // Pattern: "لقد قمت بإرسال XXXX ريال يمني"
    private val sentPattern = Regex(
        "لقد\\s*قمت\\s*بإرسال\\s+([\\d,]+(?:\\.\\d{1,2})?)\\s*(?:ر\\.ي|YER|ر\\.س|SAR|ريال)",
        RegexOption.IGNORE_CASE
    )

    // Pattern: "تم استلام XXXX YER حوالة رقم [number]"
    private val transferReceivedPattern = Regex(
        "تم\\s*استلام\\s+([\\d,]+(?:\\.\\d{1,2})?)\\s*(?:ر\\.ي|YER|ر\\.س|SAR|ريال)",
        RegexOption.IGNORE_CASE
    )

    // Pattern: "تم شحن رصيدك بمبلغ XXX ر.ي"
    private val rechargePattern = Regex(
        "تم\\s*شحن\\s*(?:رصيدك)?\\s*بمبلغ\\s+([\\d,]+(?:\\.\\d{1,2})?)\\s*(?:ر\\.ي|YER|ر\\.س|SAR)",
        RegexOption.IGNORE_CASE
    )

    // Pattern: "رصيدك هو XXXX.XX" or "رصيدكXXX.XX" - extract balance
    private val balancePattern = Regex(
        "رصيدك(?:\\s*هو)?\\s*([\\d,]+\\.?\\d{0,2})\\s*(?:ر\\.ي|YER|ر\\.س|SAR)?",
        RegexOption.IGNORE_CASE
    )

    // Merchant extraction patterns
    private val merchantFromPattern = Regex(
        "من\\s+(.+)",
        RegexOption.IGNORE_CASE
    )

    private val merchantToPattern = Regex(
        "(?:الى|لـ?)\\s*(.+)",
        RegexOption.IGNORE_CASE
    )

    // Arabic financial keywords
    private val arabicKeywords = listOf(
        "اضيف", "خصم", "تحويل", "دفع", "استلمت", "إيداع",
        "سحبت", "إرسال", "استلام", "حوالة", "رصيد",
        "ر.ي", "YER", "ر.س", "SAR", "ريال", "مبلغ",
        "شحن", "شراء", "مشتريات", "بطاقة", "محفظة",
        "صراف", "بنك", "جوال", "سداد", "اصدار"
    )

    override fun canParseNotification(packageName: String, title: String?, text: String): Boolean {
        return hasArabicFinancialText(text) || hasArabicFinancialText(title ?: "")
    }

    override fun canParseSms(sender: String, text: String): Boolean {
        return hasArabicFinancialText(text)
    }

    override fun parseNotification(packageName: String, title: String?, text: String): ParsedEvent? {
        val combined = "${title ?: ""} $text".trim()
        return parseArabicText(combined, packageName, null)
    }

    override fun parseSms(sender: String, text: String): ParsedEvent? {
        return parseArabicText(text, null, sender)
    }

    private fun hasArabicFinancialText(text: String): Boolean {
        return arabicKeywords.any { text.contains(it, ignoreCase = true) }
    }

    private fun parseArabicText(
        text: String,
        sourceApp: String? = null,
        sender: String? = null
    ): ParsedEvent? {
        // Normalize text - remove extra spaces, normalize numbers
        val normalized = text
            .replace("\\s+".toRegex(), " ")
            .replace("٫".toRegex(), ".")  // Arabic decimal separator
            .trim()

        // Try each pattern in order of specificity
        var match: MatchResult?
        var amount: Double?
        var merchant: String?
        var isIncoming: Boolean
        var currency: String

        // 1. "اضيف XXXXر.ي" - Deposit/Added (incoming)
        match = addedPattern.find(normalized)
        if (match != null) {
            amount = parseArabicNumber(match.groupValues[1])
            if (amount != null) {
                merchant = extractMerchantFrom(normalized) ?: extractMerchantAfterAmount(normalized, match.value)
                currency = detectCurrency(normalized)
                return ParsedEvent(
                    source = if (sourceApp != null) SourceType.NOTIFICATION else SourceType.SMS,
                    sourceApp = sourceApp,
                    senderNumber = sender,
                    rawText = text,
                    capturedAt = Date(),
                    merchant = merchant,
                    amount = amount,
                    currency = currency,
                    date = Date(),
                    parserName = parserId,
                    confidenceScore = 0.9f,
                    isParsed = true
                )
            }
        }

        // 2. "خصم XXXXر.ي" - Deduction (outgoing)
        match = deductedPattern.find(normalized)
        if (match != null) {
            amount = parseArabicNumber(match.groupValues[1])
            if (amount != null) {
                merchant = extractMerchantTo(normalized) ?: extractMerchantFrom(normalized)
                    ?: extractMerchantAfterAmount(normalized, match.value)
                return ParsedEvent(
                    source = if (sourceApp != null) SourceType.NOTIFICATION else SourceType.SMS,
                    sourceApp = sourceApp,
                    senderNumber = sender,
                    rawText = text,
                    capturedAt = Date(),
                    merchant = merchant,
                    amount = amount,
                    currency = detectCurrency(normalized),
                    date = Date(),
                    parserName = parserId,
                    confidenceScore = 0.9f,
                    isParsed = true
                )
            }
        }

        // 3. "تم تحويلX,XXXلحساب [name]" - Transfer to account
        match = transferredPattern.find(normalized)
        if (match != null) {
            amount = parseArabicNumber(match.groupValues[1])
            if (amount != null) {
                merchant = match.groupValues[2].trim().takeIf { it.isNotBlank() }
                    ?: "حساب آخر"
                return ParsedEvent(
                    source = if (sourceApp != null) SourceType.NOTIFICATION else SourceType.SMS,
                    sourceApp = sourceApp,
                    senderNumber = sender,
                    rawText = text,
                    capturedAt = Date(),
                    merchant = merchant,
                    amount = amount,
                    currency = detectCurrency(normalized),
                    date = Date(),
                    parserName = parserId,
                    confidenceScore = 0.85f,
                    isParsed = true
                )
            }
        }

        // 4. "استلمت XXXXر.ي من [name]" - Received from (incoming)
        match = receivedPattern.find(normalized)
        if (match != null) {
            amount = parseArabicNumber(match.groupValues[1])
            if (amount != null) {
                merchant = match.groupValues[2].trim().takeIf { it.isNotBlank() }
                    ?: extractMerchantFrom(normalized)
                    ?: "مرسل"
                return ParsedEvent(
                    source = if (sourceApp != null) SourceType.NOTIFICATION else SourceType.SMS,
                    sourceApp = sourceApp,
                    senderNumber = sender,
                    rawText = text,
                    capturedAt = Date(),
                    merchant = merchant,
                    amount = amount,
                    currency = detectCurrency(normalized),
                    date = Date(),
                    parserName = parserId,
                    confidenceScore = 0.9f,
                    isParsed = true
                )
            }
        }

        // 5. "تم دفع XXX ر.ي ل [merchant]" - Paid to (outgoing)
        match = paidPattern.find(normalized)
        if (match != null) {
            amount = parseArabicNumber(match.groupValues[1])
            if (amount != null) {
                merchant = match.groupValues[2].trim().takeIf { it.isNotBlank() }
                    ?: "تاجر"
                return ParsedEvent(
                    source = if (sourceApp != null) SourceType.NOTIFICATION else SourceType.SMS,
                    sourceApp = sourceApp,
                    senderNumber = sender,
                    rawText = text,
                    capturedAt = Date(),
                    merchant = merchant,
                    amount = amount,
                    currency = detectCurrency(normalized),
                    date = Date(),
                    parserName = parserId,
                    confidenceScore = 0.85f,
                    isParsed = true
                )
            }
        }

        // 6. "تم خصم مبلغ XXXX" - Deduction detailed
        match = deductedDetailPattern.find(normalized)
        if (match != null) {
            amount = parseArabicNumber(match.groupValues[1])
            if (amount != null) {
                merchant = extractDeductionMerchant(normalized)
                return ParsedEvent(
                    source = if (sourceApp != null) SourceType.NOTIFICATION else SourceType.SMS,
                    sourceApp = sourceApp,
                    senderNumber = sender,
                    rawText = text,
                    capturedAt = Date(),
                    merchant = merchant,
                    amount = amount,
                    currency = detectCurrency(normalized),
                    date = Date(),
                    parserName = parserId,
                    confidenceScore = 0.85f,
                    isParsed = true
                )
            }
        }

        // 7. "تم إيداع XXXXر.ي" - Deposited
        match = depositedPattern.find(normalized)
        if (match != null) {
            amount = parseArabicNumber(match.groupValues[1])
            if (amount != null) {
                merchant = extractMerchantFrom(normalized) ?: "إيداع"
                return ParsedEvent(
                    source = if (sourceApp != null) SourceType.NOTIFICATION else SourceType.SMS,
                    sourceApp = sourceApp,
                    senderNumber = sender,
                    rawText = text,
                    capturedAt = Date(),
                    merchant = merchant,
                    amount = amount,
                    currency = detectCurrency(normalized),
                    date = Date(),
                    parserName = parserId,
                    confidenceScore = 0.85f,
                    isParsed = true
                )
            }
        }

        // 8. "سحبت XXXX ر.ي من [bank]" - Withdrew
        match = withdrewPattern.find(normalized)
        if (match != null) {
            amount = parseArabicNumber(match.groupValues[1])
            if (amount != null) {
                merchant = match.groupValues[2].trim().takeIf { it.isNotBlank() } ?: "سحب"
                return ParsedEvent(
                    source = if (sourceApp != null) SourceType.NOTIFICATION else SourceType.SMS,
                    sourceApp = sourceApp,
                    senderNumber = sender,
                    rawText = text,
                    capturedAt = Date(),
                    merchant = merchant,
                    amount = amount,
                    currency = detectCurrency(normalized),
                    date = Date(),
                    parserName = parserId,
                    confidenceScore = 0.85f,
                    isParsed = true
                )
            }
        }

        // 9. "لقد قمت بإرسال XXXX" - Sent (outgoing)
        match = sentPattern.find(normalized)
        if (match != null) {
            amount = parseArabicNumber(match.groupValues[1])
            if (amount != null) {
                merchant = extractMerchantTo(normalized) ?: "تحويل"
                return ParsedEvent(
                    source = if (sourceApp != null) SourceType.NOTIFICATION else SourceType.SMS,
                    sourceApp = sourceApp,
                    senderNumber = sender,
                    rawText = text,
                    capturedAt = Date(),
                    merchant = merchant,
                    amount = amount,
                    currency = detectCurrency(normalized),
                    date = Date(),
                    parserName = parserId,
                    confidenceScore = 0.85f,
                    isParsed = true
                )
            }
        }

        // 10. "تم استلام XXXX YER حوالة" - Transfer received
        match = transferReceivedPattern.find(normalized)
        if (match != null) {
            amount = parseArabicNumber(match.groupValues[1])
            if (amount != null) {
                merchant = "حوالة واردة"
                return ParsedEvent(
                    source = if (sourceApp != null) SourceType.NOTIFICATION else SourceType.SMS,
                    sourceApp = sourceApp,
                    senderNumber = sender,
                    rawText = text,
                    capturedAt = Date(),
                    merchant = merchant,
                    amount = amount,
                    currency = detectCurrency(normalized),
                    date = Date(),
                    parserName = parserId,
                    confidenceScore = 0.85f,
                    isParsed = true
                )
            }
        }

        // 11. "تم شحن رصيدك بمبلغ XXX" - Recharge
        match = rechargePattern.find(normalized)
        if (match != null) {
            amount = parseArabicNumber(match.groupValues[1])
            if (amount != null) {
                merchant = "شحن رصيد"
                return ParsedEvent(
                    source = if (sourceApp != null) SourceType.NOTIFICATION else SourceType.SMS,
                    sourceApp = sourceApp,
                    senderNumber = sender,
                    rawText = text,
                    capturedAt = Date(),
                    merchant = merchant,
                    amount = amount,
                    currency = detectCurrency(normalized),
                    date = Date(),
                    parserName = parserId,
                    confidenceScore = 0.8f,
                    isParsed = true
                )
            }
        }

        return null
    }

    private fun parseArabicNumber(numStr: String): Double? {
        return numStr
            .replace(",", "")
            .replace("٫", ".")
            .toDoubleOrNull()
    }

    private fun detectCurrency(text: String): String {
        return when {
            text.contains("ر.س") || text.contains("SAR", ignoreCase = true) -> "SAR"
            text.contains("USD", ignoreCase = true) || text.contains("دولار") -> "USD"
            text.contains("ر.ي") || text.contains("YER", ignoreCase = true) || text.contains("ريال") -> "YER"
            else -> "YER"  // Default to YER for Arabic messages
        }
    }

    private fun extractMerchantFrom(text: String): String? {
        val match = merchantFromPattern.find(text)
        if (match != null) {
            val name = match.groupValues[1].trim()
            if (name.isNotBlank() && name != "نفسة") {
                // Remove trailing "رص:" or balance info, phone numbers
                val cleaned = name.replace("رص:.*".toRegex(), "")
                    .replace("صرف حوالة الى المحفظة".toRegex(), "")
                    .trim()
                if (cleaned.isNotBlank()) return cleaned.take(60)
            }
        }
        return null
    }

    private fun extractMerchantTo(text: String): String? {
        val match = merchantToPattern.find(text)
        if (match != null) {
            val name = match.groupValues[1].trim()
            if (name.isNotBlank()) {
                val cleaned = name.replace("رص:.*".toRegex(), "")
                    .replace("رقم\\s*\\d+".toRegex(), "")
                    .trim()
                if (cleaned.isNotBlank()) return cleaned.take(60)
            }
        }
        return null
    }

    private fun extractMerchantAfterAmount(text: String, matchText: String): String {
        val afterMatch = text.substringAfter(matchText).trim()
        // Remove balance info
        val withoutBalance = afterMatch.replace("رص:.*".toRegex(), "").trim()
        
        // Try to extract descriptive text
        val descriptiveWords = listOf(
            "تحويل مشترك", "إيداع نقدي", "صرف حوالة", "إرسال حوالة",
            "تحويل لمحفظة", "سحب من الصراف", "سداد", "عملية شراء",
            "مقابل مشتريات", "تغذية بطاقة", "اصدار بطاقة"
        )
        
        for (word in descriptiveWords) {
            if (text.contains(word)) return word
        }

        if (withoutBalance.isNotBlank() && withoutBalance.length in 2..60) {
            return withoutBalance
        }
        return "معاملة"
    }

    private fun extractDeductionMerchant(text: String): String {
        // Look for "مقابل [something]"
        val againstPattern = Regex("مقابل\\s+(.+)", RegexOption.IGNORE_CASE)
        val match = againstPattern.find(text)
        if (match != null) {
            val desc = match.groupValues[1].trim()
            if (desc.isNotBlank()) return desc.take(60)
        }

        // Look for "من [something]"
        return extractMerchantFrom(text) ?: "خصم"
    }
}
