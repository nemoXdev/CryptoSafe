package com.cryptosafe.app.security

import android.content.Context


object KeystorePrefs {
    const val MODE_KEYSTORE = "keystore"
    const val MODE_ESP = "esp"
    const val MODE_PLAIN = "plain"

    private const val PREFS_NAME = "cryptosafe_keystore_meta"
    private const val KEY_MODE = "storage_mode"
    private const val KEY_STORED = "passphrase_stored"

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getMode(context: Context): String =
        prefs(context).getString(KEY_MODE, MODE_KEYSTORE) ?: MODE_KEYSTORE

    fun setMode(context: Context, mode: String) {
        prefs(context).edit().putString(KEY_MODE, mode).apply()
    }

    fun isPassphraseStored(context: Context): Boolean =
        prefs(context).getBoolean(KEY_STORED, false)

    fun setPassphraseStored(context: Context, stored: Boolean) {
        prefs(context).edit().putBoolean(KEY_STORED, stored).apply()
    }
}
