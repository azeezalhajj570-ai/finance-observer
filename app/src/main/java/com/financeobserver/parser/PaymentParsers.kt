package com.financeobserver.parser

import com.financeobserver.model.ParsedEvent
import com.financeobserver.model.SourceType
import java.util.Date

class PayPalParser : PaymentParser {
    override val parserId = "paypal"
    override val supportedPackages = listOf("com.paypal.android.p2pmobile")
    override val supportedSenders = listOf("PayPal", "PAYPAL", "service@paypal.com")
    override val priority = 85
    private val pattern = Regex("You\\s+(?:received|sent)\\s+\\$?([\\d,]+\\.\\d{2})", RegexOption.IGNORE_CASE)
    private val merchantPattern = Regex("(?:to|from)\\s+([\\w\\s&]+?)(?:\\s+for|\\s+\\.|$)", RegexOption.IGNORE_CASE)

    override fun canParseNotification(packageName: String, title: String?, text: String) =
        packageName in supportedPackages || text.contains("PayPal", ignoreCase = true)
    override fun canParseSms(sender: String, text: String) = sender in supportedSenders

    override fun parseNotification(packageName: String, title: String?, text: String): ParsedEvent? {
        val combined = "${title ?: ""} $text"
        val amountMatch = pattern.find(combined) ?: return null
        val amount = amountMatch.groupValues[1].replace(",", "").toDoubleOrNull() ?: return null
        val merchant = merchantPattern.find(combined)?.groupValues?.get(1)?.trim() ?: "PayPal"
        return ParsedEvent(SourceType.NOTIFICATION, packageName, null, text, Date(), merchant, amount, "USD", Date(), parserId, 0.9f, true)
    }
    override fun parseSms(sender: String, text: String): ParsedEvent? {
        val amountMatch = pattern.find(text) ?: return null
        val amount = amountMatch.groupValues[1].replace(",", "").toDoubleOrNull() ?: return null
        val merchant = merchantPattern.find(text)?.groupValues?.get(1)?.trim() ?: "PayPal"
        return ParsedEvent(SourceType.SMS, null, sender, text, Date(), merchant, amount, "USD", Date(), parserId, 0.9f, true)
    }
}

class GooglePayParser : PaymentParser {
    override val parserId = "google_pay"
    override val supportedPackages = listOf("com.google.android.apps.nbu.paisa.user", "com.google.android.apps.walletnfcrel")
    override val supportedSenders = listOf("Google Pay", "GPay")
    override val priority = 85
    private val pattern = Regex("\\$?([\\d,]+\\.\\d{2})\\s+(?:paid to|received from|sent to)\\s+([\\w\\s&]+?)", RegexOption.IGNORE_CASE)

    override fun canParseNotification(packageName: String, title: String?, text: String) =
        packageName in supportedPackages || text.contains("Google Pay", ignoreCase = true) || text.contains("GPay", ignoreCase = true)
    override fun canParseSms(sender: String, text: String) = sender in supportedSenders

    override fun parseNotification(packageName: String, title: String?, text: String): ParsedEvent? {
        val combined = "${title ?: ""} $text"
        val match = pattern.find(combined) ?: return null
        val amount = match.groupValues[1].replace(",", "").toDoubleOrNull() ?: return null
        return ParsedEvent(SourceType.NOTIFICATION, packageName, null, text, Date(), match.groupValues[2].trim(), amount, "USD", Date(), parserId, 0.9f, true)
    }
    override fun parseSms(sender: String, text: String): ParsedEvent? {
        val match = pattern.find(text) ?: return null
        val amount = match.groupValues[1].replace(",", "").toDoubleOrNull() ?: return null
        return ParsedEvent(SourceType.SMS, null, sender, text, Date(), match.groupValues[2].trim(), amount, "USD", Date(), parserId, 0.9f, true)
    }
}

class ApplePayParser : PaymentParser {
    override val parserId = "apple_pay"
    override val supportedPackages = listOf("com.apple.passbook")
    override val supportedSenders = listOf("Apple Pay", "Apple")
    override val priority = 80
    private val pattern = Regex("\\$?([\\d,]+\\.\\d{2})\\s+(?:at|to)\\s+([\\w\\s&]+?)", RegexOption.IGNORE_CASE)

    override fun canParseNotification(packageName: String, title: String?, text: String) =
        packageName in supportedPackages || text.contains("Apple Pay", ignoreCase = true)
    override fun canParseSms(sender: String, text: String) = sender in supportedSenders

    override fun parseNotification(packageName: String, title: String?, text: String): ParsedEvent? {
        val combined = "${title ?: ""} $text"
        val match = pattern.find(combined) ?: return null
        val amount = match.groupValues[1].replace(",", "").toDoubleOrNull() ?: return null
        return ParsedEvent(SourceType.NOTIFICATION, packageName, null, text, Date(), match.groupValues[2].trim(), amount, "USD", Date(), parserId, 0.85f, true)
    }
    override fun parseSms(sender: String, text: String): ParsedEvent? {
        val match = pattern.find(text) ?: return null
        val amount = match.groupValues[1].replace(",", "").toDoubleOrNull() ?: return null
        return ParsedEvent(SourceType.SMS, null, sender, text, Date(), match.groupValues[2].trim(), amount, "USD", Date(), parserId, 0.85f, true)
    }
}

class CashAppParser : PaymentParser {
    override val parserId = "cash_app"
    override val supportedPackages = listOf("com.squareup.cash")
    override val supportedSenders = listOf("Cash App", "CashApp")
    override val priority = 85
    private val pattern = Regex("\\$?([\\d,]+\\.\\d{2})\\s+(?:from|to|paid by|paid to)\\s+([\\w$]+)", RegexOption.IGNORE_CASE)

    override fun canParseNotification(packageName: String, title: String?, text: String) =
        packageName in supportedPackages || text.contains("Cash App", ignoreCase = true)
    override fun canParseSms(sender: String, text: String) = sender in supportedSenders

    override fun parseNotification(packageName: String, title: String?, text: String): ParsedEvent? {
        val combined = "${title ?: ""} $text"
        val match = pattern.find(combined) ?: return null
        val amount = match.groupValues[1].replace(",", "").toDoubleOrNull() ?: return null
        return ParsedEvent(SourceType.NOTIFICATION, packageName, null, text, Date(), "Cash App ${match.groupValues[2]}", amount, "USD", Date(), parserId, 0.9f, true)
    }
    override fun parseSms(sender: String, text: String): ParsedEvent? {
        val match = pattern.find(text) ?: return null
        val amount = match.groupValues[1].replace(",", "").toDoubleOrNull() ?: return null
        return ParsedEvent(SourceType.SMS, null, sender, text, Date(), "Cash App ${match.groupValues[2]}", amount, "USD", Date(), parserId, 0.9f, true)
    }
}

class ZelleParser : PaymentParser {
    override val parserId = "zelle"
    override val supportedPackages = listOf("com.zellepay.zelle")
    override val supportedSenders = listOf("Zelle", "ZELLE")
    override val priority = 80
    private val pattern = Regex("\\$?([\\d,]+\\.\\d{2})\\s+(?:from|to|received from|sent to)\\s+([\\w\\s.@]+?)(?:\\s+for|\\s+\\.|$)", RegexOption.IGNORE_CASE)

    override fun canParseNotification(packageName: String, title: String?, text: String) =
        packageName in supportedPackages || text.contains("Zelle", ignoreCase = true)
    override fun canParseSms(sender: String, text: String) = sender in supportedSenders

    override fun parseNotification(packageName: String, title: String?, text: String): ParsedEvent? {
        val combined = "${title ?: ""} $text"
        val match = pattern.find(combined) ?: return null
        val amount = match.groupValues[1].replace(",", "").toDoubleOrNull() ?: return null
        return ParsedEvent(SourceType.NOTIFICATION, packageName, null, text, Date(), "Zelle ${match.groupValues[2].trim()}", amount, "USD", Date(), parserId, 0.9f, true)
    }
    override fun parseSms(sender: String, text: String): ParsedEvent? {
        val match = pattern.find(text) ?: return null
        val amount = match.groupValues[1].replace(",", "").toDoubleOrNull() ?: return null
        return ParsedEvent(SourceType.SMS, null, sender, text, Date(), "Zelle ${match.groupValues[2].trim()}", amount, "USD", Date(), parserId, 0.9f, true)
    }
}

class StripeParser : PaymentParser {
    override val parserId = "stripe"
    override val supportedPackages = listOf("com.stripe.android")
    override val supportedSenders = listOf("Stripe", "STRIPE")
    override val priority = 75
    private val pattern = Regex("\\$?([\\d,]+\\.\\d{2})\\s+(?:charge|payment)\\s+(?:at|from|to)\\s+([\\w\\s&]+?)", RegexOption.IGNORE_CASE)

    override fun canParseNotification(packageName: String, title: String?, text: String) =
        packageName in supportedPackages || text.contains("Stripe", ignoreCase = true)
    override fun canParseSms(sender: String, text: String) = sender in supportedSenders

    override fun parseNotification(packageName: String, title: String?, text: String): ParsedEvent? {
        val combined = "${title ?: ""} $text"
        val match = pattern.find(combined) ?: return null
        val amount = match.groupValues[1].replace(",", "").toDoubleOrNull() ?: return null
        return ParsedEvent(SourceType.NOTIFICATION, packageName, null, text, Date(), match.groupValues[2].trim(), amount, "USD", Date(), parserId, 0.85f, true)
    }
    override fun parseSms(sender: String, text: String): ParsedEvent? {
        val match = pattern.find(text) ?: return null
        val amount = match.groupValues[1].replace(",", "").toDoubleOrNull() ?: return null
        return ParsedEvent(SourceType.SMS, null, sender, text, Date(), match.groupValues[2].trim(), amount, "USD", Date(), parserId, 0.85f, true)
    }
}

class SquareParser : PaymentParser {
    override val parserId = "square"
    override val supportedPackages = listOf("com.squareup")
    override val supportedSenders = listOf("Square", "SQUARE")
    override val priority = 75
    private val pattern = Regex("\\$?([\\d,]+\\.\\d{2})\\s+(?:payment|charge)\\s+(?:from|at)\\s+([\\w\\s&]+?)", RegexOption.IGNORE_CASE)

    override fun canParseNotification(packageName: String, title: String?, text: String) =
        packageName in supportedPackages || text.contains("Square", ignoreCase = true)
    override fun canParseSms(sender: String, text: String) = sender in supportedSenders

    override fun parseNotification(packageName: String, title: String?, text: String): ParsedEvent? {
        val combined = "${title ?: ""} $text"
        val match = pattern.find(combined) ?: return null
        val amount = match.groupValues[1].replace(",", "").toDoubleOrNull() ?: return null
        return ParsedEvent(SourceType.NOTIFICATION, packageName, null, text, Date(), match.groupValues[2].trim(), amount, "USD", Date(), parserId, 0.85f, true)
    }
    override fun parseSms(sender: String, text: String): ParsedEvent? {
        val match = pattern.find(text) ?: return null
        val amount = match.groupValues[1].replace(",", "").toDoubleOrNull() ?: return null
        return ParsedEvent(SourceType.SMS, null, sender, text, Date(), match.groupValues[2].trim(), amount, "USD", Date(), parserId, 0.85f, true)
    }
}
