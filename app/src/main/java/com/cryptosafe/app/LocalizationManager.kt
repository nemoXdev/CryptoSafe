package com.cryptosafe.app

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONObject
import java.io.IOException

object LocalizationManager {
    private var prefs: SharedPreferences? = null
    private var translations = mutableMapOf<String, JSONObject>()
    var currentLocale by mutableStateOf("en")
        private set
    val isRtl: Boolean
        get() = currentLocale in listOf("ar", "fa", "ku")

    fun initialize(context: Context) {
        prefs = context.getSharedPreferences("cryptosafe_prefs", Context.MODE_PRIVATE)
        loadLocale(context, "en")
        loadLocale(context, "ar")
        loadLocale(context, "fr")
        loadLocale(context, "es")
        loadLocale(context, "de")
        loadLocale(context, "zh")
        loadLocale(context, "pt")
        loadLocale(context, "fa")
        loadLocale(context, "ku")
        loadLocale(context, "hi")

        val saved = prefs?.getString("locale", null)
        if (saved != null && translations.containsKey(saved)) {
            currentLocale = saved
        } else {
            val deviceLang = context.resources.configuration.locales[0].language
            currentLocale = if (translations.containsKey(deviceLang)) deviceLang else "en"
        }
    }

    private fun loadLocale(context: Context, locale: String) {
        try {
            val inputStream = context.assets.open("locales/$locale.json")
            val json = inputStream.bufferedReader().use { it.readText() }
            translations[locale] = JSONObject(json)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun setLocale(locale: String) {
        if (translations.containsKey(locale)) {
            currentLocale = locale
            prefs?.edit()?.putString("locale", locale)?.apply()
        }
    }

    fun getString(key: String): String {
        return try {
            translations[currentLocale]?.getString(key) ?: key
        } catch (e: Exception) {
            key
        }
    }

    fun getAvailableLocales(): List<String> = translations.keys.toList()

    fun getLocaleDisplayName(code: String): String = when (code) {
        "en" -> "English"
        "ar" -> "العربية"
        "fr" -> "Français"
        "es" -> "Español"
        "de" -> "Deutsch"
        "zh" -> "简体中文"
        "pt" -> "Português"
        "fa" -> "فارسی"
        "ku" -> "کوردی"
        "hi" -> "हिन्दी"
        else -> code
    }
}
