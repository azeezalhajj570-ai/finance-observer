package com.financeobserver.util

import android.content.Context
import android.content.SharedPreferences
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

object CurrencyHelper {
    private const val PREFS_NAME = "currency_prefs"
    private const val KEY_CURRENCY = "selected_currency"
    private const val DEFAULT_CURRENCY = "USD"

    val supportedCurrencies = listOf(
        CurrencyInfo("USD", "$", "US Dollar"),
        CurrencyInfo("EUR", "€", "Euro"),
        CurrencyInfo("GBP", "£", "British Pound"),
        CurrencyInfo("SAR", "﷼", "Saudi Riyal"),
        CurrencyInfo("AED", "د.إ", "UAE Dirham"),
        CurrencyInfo("EGP", "E£", "Egyptian Pound"),
        CurrencyInfo("JPY", "¥", "Japanese Yen"),
        CurrencyInfo("INR", "₹", "Indian Rupee"),
        CurrencyInfo("CAD", "C$", "Canadian Dollar"),
        CurrencyInfo("AUD", "A$", "Australian Dollar")
    )

    data class CurrencyInfo(
        val code: String,
        val symbol: String,
        val name: String
    )

    fun getSelectedCurrency(context: Context): CurrencyInfo {
        val prefs = getPrefs(context)
        val code = prefs.getString(KEY_CURRENCY, DEFAULT_CURRENCY) ?: DEFAULT_CURRENCY
        return supportedCurrencies.find { it.code == code } ?: supportedCurrencies.first()
    }

    fun setSelectedCurrency(context: Context, code: String) {
        getPrefs(context).edit().putString(KEY_CURRENCY, code).apply()
    }

    fun formatAmount(amount: Double, currency: CurrencyInfo): String {
        return try {
            val format = NumberFormat.getCurrencyInstance(Locale.US)
            format.currency = Currency.getInstance(currency.code)
            format.format(amount)
        } catch (e: Exception) {
            "${currency.symbol}%.2f".format(amount)
        }
    }

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
}
