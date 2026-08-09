package com.cryptosafe.app.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.cryptosafe.app.CryptoEngine
import java.security.SecureRandom

object SecurePasswordStorage {
    private var prefs: SharedPreferences? = null
    private var appContext: Context? = null

    
    fun isStorageDegraded(): Boolean = storageDegraded

    private var storageDegraded = false

    fun initialize(context: Context) {
        appContext = context.applicationContext
        val rawPrefs = context.getSharedPreferences("cryptosafe_secure_prefs", Context.MODE_PRIVATE)

        try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            prefs = EncryptedSharedPreferences.create(
                context, "cryptosafe_secure_prefs", masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
            storageDegraded = false
            return
        } catch (e: Exception) {
            com.cryptosafe.app.DiagnosticsLogger.logEvent(
                "WARN",
                "esp_init_failed_using_keystore_encrypted_fallback"
            )
        }

        
        val fallback = KeystoreEncryptedPrefs.create(
            context,
            "cryptosafe_secure_prefs_fallback"
        )
        if (fallback != null) {
            prefs = fallback
            storageDegraded = true
            return
        }

        
        
        com.cryptosafe.app.DiagnosticsLogger.logEvent(
            "CRITICAL",
            "secure_storage_unavailable_writes_disabled"
        )
        prefs = NoopPrefs
        storageDegraded = true
    }

    fun savePin(pin: String) {
        getPrefs().edit().putString("pin_hash", CryptoEngine.hashPin(pin)).apply()
    }

    fun hasPin(): Boolean {
        return !getPrefs().getString("pin_hash", null).isNullOrEmpty()
    }

    fun removePinHash() {
        getPrefs().edit().remove("pin_hash").apply()
    }

    fun getPinHash(): String? {
        return getPrefs().getString("pin_hash", null)
    }

    fun getPinAttempts(): Int {
        return getPrefs().getInt("pin_attempts", 0)
    }

    fun setPinAttempts(attempts: Int) {
        getPrefs().edit().putInt("pin_attempts", attempts).apply()
    }

    fun getPinLockoutTime(): Long {
        return getPrefs().getLong("pin_lockout_time", 0L)
    }

    fun setPinLockoutTime(time: Long) {
        getPrefs().edit().putLong("pin_lockout_time", time).apply()
    }

    fun isScreenshotProtectionEnabled(): Boolean {
        return getPrefs().getBoolean("screenshot_protection", true)
    }

    fun setScreenshotProtectionEnabled(enabled: Boolean) {
        getPrefs().edit().putBoolean("screenshot_protection", enabled).apply()
    }

    fun isBiometricEnabled(): Boolean {
        return getPrefs().getBoolean("biometric_enabled", true)
    }

    fun setBiometricEnabled(enabled: Boolean) {
        getPrefs().edit().putBoolean("biometric_enabled", enabled).apply()
    }

    
    fun getDatabasePassphrase(): ByteArray? {
        val context = appContext ?: return null
        KeystoreBox.decrypt(context)?.let { return it.toByteArray() }
        return getPrefs().getString("db_passphrase", null)?.toByteArray()
    }

    
    
    fun saveDatabasePassphrase(passphrase: String) {
        val context = appContext ?: return
        if (KeystoreBox.encrypt(context, passphrase)) {
            KeystorePrefs.setMode(context, KeystorePrefs.MODE_KEYSTORE)
            KeystorePrefs.setPassphraseStored(context, true)
            return
        }
        KeystorePrefs.setMode(context, KeystorePrefs.MODE_ESP)
        getPrefs().edit().putString("db_passphrase", passphrase).apply()
    }

    fun clearDatabasePassphrase() {
        appContext?.let { KeystoreBox.delete(it) }
        getPrefs().edit().remove("db_passphrase").apply()
    }

    fun getStorageMode(): String {
        val context = appContext ?: return KeystorePrefs.MODE_ESP
        return KeystorePrefs.getMode(context)
    }

    fun getOrCreateDatabasePassphrase(): ByteArray {
        val existing = getDatabasePassphrase()
        if (existing != null && existing.isNotEmpty()) return existing
        
        
        val bytes = ByteArray(32)
        SecureRandom().nextBytes(bytes)
        val passphrase = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
        saveDatabasePassphrase(passphrase)
        return passphrase.toByteArray()
    }

    
    fun canSafelyOpenExistingDb(context: Context): Boolean {
        val dbFile = context.getDatabasePath("cryptosafe.db")
        return !(dbFile.exists() && dbFile.length() > 0 && getDatabasePassphrase() == null)
    }

    fun saveBoxPassword(boxId: Long, password: String) {
        getPrefs().edit().putString("box_perm_$boxId", password).apply()
    }

    fun getBoxPassword(boxId: Long): String? {
        return getPrefs().getString("box_perm_$boxId", null)
    }

    fun removeBoxPassword(boxId: Long) {
        getPrefs().edit().remove("box_perm_$boxId").apply()
    }

    fun hasPermanentBoxPassword(boxId: Long): Boolean {
        return getPrefs().contains("box_perm_$boxId")
    }

    fun getAutoLockTimer(): Int {
        return getPrefs().getInt("auto_lock_timer", 0)
    }

    fun setAutoLockTimer(seconds: Int) {
        getPrefs().edit().putInt("auto_lock_timer", seconds).apply()
    }

    
    fun getBoxAttempts(boxId: Long): Int {
        return getPrefs().getInt("box_attempts_$boxId", 0)
    }

    fun setBoxAttempts(boxId: Long, attempts: Int) {
        getPrefs().edit().putInt("box_attempts_$boxId", attempts).apply()
    }

    fun getBoxLockoutTime(boxId: Long): Long {
        return getPrefs().getLong("box_lockout_$boxId", 0L)
    }

    fun setBoxLockoutTime(boxId: Long, time: Long) {
        getPrefs().edit().putLong("box_lockout_$boxId", time).apply()
    }

    fun getLastStopTime(): Long {
        return getPrefs().getLong("last_stop_time", 0L)
    }

    fun setLastStopTime(time: Long) {
        getPrefs().edit().putLong("last_stop_time", time).apply()
    }

    fun isLockedOnExit(): Boolean {
        return getPrefs().getBoolean("locked_on_exit", false)
    }

    fun setLockedOnExit(locked: Boolean) {
        getPrefs().edit().putBoolean("locked_on_exit", locked).apply()
    }

    private fun getPrefs(): SharedPreferences {
        return prefs ?: throw IllegalStateException("SecurePasswordStorage not initialized")
    }
}


private object NoopPrefs : SharedPreferences {
    override fun getAll(): MutableMap<String, Any?> = HashMap()
    override fun getString(key: String, defValue: String?): String? = defValue
    override fun getStringSet(key: String, defValue: Set<String>?): Set<String>? = defValue
    override fun getInt(key: String, defValue: Int): Int = defValue
    override fun getLong(key: String, defValue: Long): Long = defValue
    override fun getFloat(key: String, defValue: Float): Float = defValue
    override fun getBoolean(key: String, defValue: Boolean): Boolean = defValue
    override fun contains(key: String): Boolean = false

    override fun edit(): SharedPreferences.Editor = object : SharedPreferences.Editor {
        override fun putString(key: String, value: String?): SharedPreferences.Editor = this
        override fun putStringSet(key: String, values: Set<String>?): SharedPreferences.Editor = this
        override fun putInt(key: String, value: Int): SharedPreferences.Editor = this
        override fun putLong(key: String, value: Long): SharedPreferences.Editor = this
        override fun putFloat(key: String, value: Float): SharedPreferences.Editor = this
        override fun putBoolean(key: String, value: Boolean): SharedPreferences.Editor = this
        override fun remove(key: String): SharedPreferences.Editor = this
        override fun clear(): SharedPreferences.Editor = this
        override fun commit(): Boolean = true
        override fun apply() {}
    }

    override fun registerOnSharedPreferenceChangeListener(
        listener: SharedPreferences.OnSharedPreferenceChangeListener
    ) {}

    override fun unregisterOnSharedPreferenceChangeListener(
        listener: SharedPreferences.OnSharedPreferenceChangeListener
    ) {}
}
