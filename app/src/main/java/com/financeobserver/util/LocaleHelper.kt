package com.financeobserver.util

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

object LocaleHelper {

    const val LOCALE_EN = "en"
    const val LOCALE_AR = "ar"

    fun setLocale(context: Context, languageCode: String): Context {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        return context.createConfigurationContext(config)
    }

    fun getCurrentLocale(context: Context): String {
        val locale = context.resources.configuration.locales[0]
        return if (locale.language == LOCALE_AR) LOCALE_AR else LOCALE_EN
    }

    fun isArabic(context: Context): Boolean {
        return getCurrentLocale(context) == LOCALE_AR
    }

    fun wrapContext(context: Context): Context {
        val savedLocale = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            .getString("app_locale", null)
        return if (savedLocale != null) {
            setLocale(context, savedLocale)
        } else {
            context
        }
    }

    fun saveLocale(context: Context, languageCode: String) {
        context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            .edit()
            .putString("app_locale", languageCode)
            .apply()
    }
}
