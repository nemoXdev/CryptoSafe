package com.cryptosafe.app.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

object SecurePasswordStorage {
    private var prefs: SharedPreferences? = null
    private var backupPrefs: SharedPreferences? = null
    var isPassphraseLost = false
        private set
    var isBackupAvailable = false
        private set

    fun initialize(context: Context) {
        backupPrefs = context.getSharedPreferences("cryptosafe_db_backup", Context.MODE_PRIVATE)
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
            syncBackupEncrypted()
            return
        } catch (e: Exception) {
            prefs = rawPrefs

            val recovered = rawPrefs.getString("db_passphrase", null)
            if (!recovered.isNullOrEmpty()) return

            val encryptedPass = backupPrefs?.getString("db_passphrase_enc", null)
            if (!encryptedPass.isNullOrEmpty()) {
                val rawPinHash = rawPrefs.getString("pin_hash", null)
                if (!rawPinHash.isNullOrEmpty()) {
                    try {
                        val decrypted = decryptWithPin(encryptedPass, rawPinHash)
                        rawPrefs.edit().putString("db_passphrase", decrypted).apply()
                        return
                    } catch (_: Exception) {}
                }
                isBackupAvailable = true
            }

            isPassphraseLost = true
        }
    }

    private fun syncBackupEncrypted() {
        val passphrase = prefs?.getString("db_passphrase", null) ?: return
        val pinHash = prefs?.getString("pin_hash", null) ?: return
        try {
            val encrypted = encryptWithPin(passphrase, pinHash)
            backupPrefs?.edit()?.putString("db_passphrase_enc", encrypted)?.apply()
        } catch (_: Exception) {}
    }

    fun recoverFromBackup(pin: String): Boolean {
        val encryptedPass = backupPrefs?.getString("db_passphrase_enc", null) ?: return false
        return try {
            val pinHash = hashPin(pin)
            val decrypted = decryptWithPin(encryptedPass, pinHash)
            getPrefs().edit().putString("db_passphrase", decrypted).apply()
            isPassphraseLost = false
            isBackupAvailable = false
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun hashPin(pin: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        return md.digest(pin.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
    }

    private fun getPrefs(): SharedPreferences {
        return prefs ?: throw IllegalStateException("SecurePasswordStorage not initialized")
    }

    private fun encryptWithPin(plaintext: String, pinHash: String): String {
        val keyBytes = pinHash.toByteArray(Charsets.UTF_8).copyOf(32)
        val key = SecretKeySpec(keyBytes, "AES")
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val iv = ByteArray(12)
        SecureRandom().nextBytes(iv)
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(128, iv))
        val encrypted = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
        return android.util.Base64.encodeToString(iv + encrypted, android.util.Base64.NO_WRAP)
    }

    private fun decryptWithPin(encryptedBase64: String, pinHash: String): String {
        val keyBytes = pinHash.toByteArray(Charsets.UTF_8).copyOf(32)
        val key = SecretKeySpec(keyBytes, "AES")
        val combined = android.util.Base64.decode(encryptedBase64, android.util.Base64.NO_WRAP)
        val iv = combined.copyOfRange(0, 12)
        val encrypted = combined.copyOfRange(12, combined.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, iv))
        return String(cipher.doFinal(encrypted), Charsets.UTF_8)
    }

    fun hasPin(): Boolean {
        return !getPrefs().getString("pin_hash", null).isNullOrEmpty()
    }

    fun removePinHash() {
        getPrefs().edit().remove("pin_hash").apply()
    }

    fun savePinHash(pinHash: String) {
        getPrefs().edit().putString("pin_hash", pinHash).apply()
        syncBackupEncrypted()
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
        return getPrefs().getString("db_passphrase", null)?.toByteArray()
    }

    fun saveDatabasePassphrase(passphrase: String) {
        getPrefs().edit().putString("db_passphrase", passphrase).apply()
        syncBackupEncrypted()
    }

    fun clearDatabasePassphrase() {
        getPrefs().edit().remove("db_passphrase").apply()
        backupPrefs?.edit()?.remove("db_passphrase_enc")?.apply()
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
}
