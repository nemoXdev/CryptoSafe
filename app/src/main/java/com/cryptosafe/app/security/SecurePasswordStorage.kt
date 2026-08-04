package com.cryptosafe.app.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.cryptosafe.app.CryptoEngine
import java.security.SecureRandom

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
            return
        } catch (e: Exception) {
            // EncryptedSharedPreferences فشلت (جهاز به مشكلة نظام).
            // نعود لملف عادي ولا نمسح أي شيء — القراءة من raw قد تعيد
            // المفتاح الصحيح إذا كان قد حُفظ صريحاً من قبل.
            prefs = rawPrefs

            // علامة "fallback_plain" تعني أن المفتاح الموجود في raw كتبه كودنا
            // كنص صريح، فهو موثوق. بدونها قد يكون الملف مكتوباً بواسطة ESP
            // (قيمة مشفرة) فلا نثق به ونلجأ للنسخة الاحتياطية.
            if (rawPrefs.getBoolean("fallback_plain", false)) {
                val recovered = rawPrefs.getString("db_passphrase", null)
                if (!recovered.isNullOrEmpty()) return
            }

            val encryptedPass = backupPrefs?.getString("db_passphrase_enc", null)
            if (!encryptedPass.isNullOrEmpty()) {
                isBackupAvailable = true
                isPassphraseLost = true
                return
            }

            val recovered = rawPrefs.getString("db_passphrase", null)
            if (!recovered.isNullOrEmpty()) return

            isPassphraseLost = true
        }
    }

    // النسخة الاحتياطية تُنشأ من المفتاح الواضح + PIN النصي مباشرة،
    // دون قراءة أي شيء من ESP — فهكذا تنجح حتى لو ESP معطوبة بالكامل.
    fun savePin(pin: String) {
        getPrefs().edit().putString("pin_hash", CryptoEngine.hashPin(pin)).apply()
        syncBackupWithPin(pin)
    }

    private fun syncBackupWithPin(pin: String) {
        val passphrase = prefs?.getString("db_passphrase", null) ?: return
        try {
            val encrypted = CryptoEngine.encrypt(passphrase, pin.toCharArray())
            backupPrefs?.edit()?.putString("db_passphrase_enc", encrypted)?.apply()
        } catch (_: Exception) {}
    }

    // استرجاع المفتاح من النسخة الاحتياطية عبر PIN. ينجح حتى لو ESP معطوبة.
    fun recoverFromBackup(pin: String): Boolean {
        val encryptedPass = backupPrefs?.getString("db_passphrase_enc", null) ?: return false
        return try {
            val decrypted = CryptoEngine.decrypt(encryptedPass, pin.toCharArray())
            getPrefs().edit().putString("db_passphrase", decrypted).apply()
            markPlainFallback()
            isPassphraseLost = false
            isBackupAvailable = false
            true
        } catch (_: Exception) {
            false
        }
    }

    // تحقق فقط من صحة PIN مقابل الاحتياطي، دون حفظ أي شيء
    // (تُستخدم لتأكيد الحذف النهائي قبل تنفيذه).
    fun isBackupPinValid(pin: String): Boolean {
        val encryptedPass = backupPrefs?.getString("db_passphrase_enc", null) ?: return false
        return try {
            CryptoEngine.decrypt(encryptedPass, pin.toCharArray())
            true
        } catch (_: Exception) {
            false
        }
    }

    fun hasPin(): Boolean {
        return !getPrefs().getString("pin_hash", null).isNullOrEmpty()
    }

    fun removePinHash() {
        getPrefs().edit().remove("pin_hash").apply()
        backupPrefs?.edit()?.remove("db_passphrase_enc")?.apply()
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
        markPlainFallback()
    }

    // تُكتب العلامة فقط عندما نكتب المفتاح بأنفسنا في وضع الـ fallback،
    // لتأكيد أن قيمة db_passphrase في raw هي نص صريح موثوق.
    private fun markPlainFallback() {
        if (prefs == null) return
        getPrefs().edit().putBoolean("fallback_plain", true).apply()
    }

    fun clearDatabasePassphrase() {
        getPrefs().edit().remove("db_passphrase").apply()
        backupPrefs?.edit()?.remove("db_passphrase_enc")?.apply()
    }

    fun getOrCreateDatabasePassphrase(): ByteArray {
        val existing = getDatabasePassphrase()
        if (existing != null && existing.isNotEmpty()) return existing
        // يوجد احتياطي بانتظار الاسترجاع — لا ننشئ مفتاحاً جديداً أبداً
        // وإلا تعذّر فتح القاعدة القديمة وتضيع البيانات للأبد.
        if (isBackupAvailable) {
            throw IllegalStateException("db_passphrase_lost_recovery_required")
        }
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

    private fun getPrefs(): SharedPreferences {
        return prefs ?: throw IllegalStateException("SecurePasswordStorage not initialized")
    }
}
