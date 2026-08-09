package com.cryptosafe.app

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.IOException

object LocalizationManager {
    private var prefs: SharedPreferences? = null
    private var appContext: Context? = null
    private val translations = mutableMapOf<String, JSONObject>()
    private val backgroundScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val allLocales = listOf(
        "en", "ar", "fr", "es", "de", "zh", "pt", "fa", "ku", "hi", "tr", "ru", "id", "ko", "ja"
    )
    var currentLocale by mutableStateOf("en")
        private set
    val isRtl: Boolean
        get() = currentLocale in listOf("ar", "fa", "ku")

    fun initialize(context: Context) {
        prefs = context.getSharedPreferences("cryptosafe_prefs", Context.MODE_PRIVATE)
        appContext = context.applicationContext

        val saved = prefs?.getString("locale", null)
        val deviceLang = context.resources.configuration.locales[0].language
        val active = when {
            saved != null && saved in allLocales -> saved
            deviceLang in allLocales -> deviceLang
            else -> "en"
        }

        
        
        loadLocale(context, active)
        currentLocale = active

        val remaining = allLocales - active
        backgroundScope.launch(Dispatchers.IO) {
            remaining.forEach { loadLocale(context, it) }
        }
    }

    @Synchronized
    private fun loadLocale(context: Context, locale: String) {
        try {
            if (translations.containsKey(locale)) return
            val combined = JSONObject()
            val files = context.assets.list("locales/$locale") ?: return
            for (file in files) {
                if (!file.endsWith(".json")) continue
                val inputStream = context.assets.open("locales/$locale/$file")
                val json = inputStream.bufferedReader().use { it.readText() }
                val obj = JSONObject(json)
                val keys = obj.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    combined.put(key, obj.get(key))
                }
            }
            translations[locale] = combined
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun setLocale(locale: String) {
        if (locale in allLocales) {
            if (!translations.containsKey(locale)) {
                appContext?.let { loadLocale(it, locale) }
            }
            currentLocale = locale
            prefs?.edit()?.putString("locale", locale)?.apply()
        }
    }

    @Synchronized
    fun getString(key: String): String {
        return try {
            translations[currentLocale]?.getString(key) ?: key
        } catch (e: Exception) {
            key
        }
    }

    fun getAvailableLocales(): List<String> = allLocales

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
        "tr" -> "Türkçe"
        "ru" -> "Русский"
        "id" -> "Bahasa Indonesia"
        "ko" -> "한국어"
        "ja" -> "日本語"
        else -> code
    }
}
