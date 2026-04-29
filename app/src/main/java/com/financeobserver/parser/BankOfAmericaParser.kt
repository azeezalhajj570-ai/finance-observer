package com.financeobserver.parser

import com.financeobserver.model.ParsedEvent
import com.financeobserver.model.SourceType
import java.util.Date

class BankOfAmericaParser : PaymentParser {
    override val parserId = "bank_of_america"
    override val supportedPackages = listOf("com.infonow.bofa", "com.bankofamerica.digitalwallet")
    override val supportedSenders = listOf("Bank of America", "BofA", "BOFA")
    override val priority = 88

    private val pattern = Regex("(?:purchase|charge|payment)\\s+of\\s+\\$?([\\d,]+\\.\\d{2})\\s+(?:at|to|from)\\s+([\\w\\s&]+?)(?:\\s+on|\\s+\\.|$)", RegexOption.IGNORE_CASE)

    override fun canParseNotification(packageName: String, title: String?, text: String) =
        packageName in supportedPackages || text.contains("Bank of America", ignoreCase = true) || text.contains("BofA", ignoreCase = true)

    override fun canParseSms(sender: String, text: String) = sender in supportedSenders

    override fun parseNotification(packageName: String, title: String?, text: String): ParsedEvent? {
        val match = pattern.find("${title ?: ""} $text") ?: return null
        val amount = match.groupValues[1].replace(",", "").toDoubleOrNull() ?: return null
        return ParsedEvent(SourceType.NOTIFICATION, packageName, null, text, Date(), match.groupValues[2].trim(), amount, "USD", Date(), parserId, 0.9f, true)
    }

    override fun parseSms(sender: String, text: String): ParsedEvent? {
        val match = pattern.find(text) ?: return null
        val amount = match.groupValues[1].replace(",", "").toDoubleOrNull() ?: return null
        return ParsedEvent(SourceType.SMS, null, sender, text, Date(), match.groupValues[2].trim(), amount, "USD", Date(), parserId, 0.9f, true)
    }
}
