package com.cryptosafe.app.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.SecureRandom

object SecurePasswordStorage {
    private var encryptedPrefs: SharedPreferences? = null
    private var masterKey: MasterKey? = null

    fun initialize(context: Context) {
        masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        encryptedPrefs = EncryptedSharedPreferences.create(
            context,
            "cryptosafe_secure_prefs",
            masterKey!!,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    private fun getPrefs(): SharedPreferences {
        return encryptedPrefs ?: throw IllegalStateException("SecurePasswordStorage not initialized")
    }

    // ملاحظة أمنية: كلمة مرور الصندوق لا تُخزَّن هنا أبداً (لا بنص عادي ولا مشفّرة).
    // يتم التحقق منها فقط عبر CryptoEngine.verifyPasswordForStorage() مقابل Box.passwordHash،
    // وتبقى موجودة بالذاكرة (CharArray) طوال فتح الصندوق فقط ثم تُمسح فوراً.
    // هذا يضمن إن فتح قفل PIN العام للتطبيق لا يعطي وصول تلقائي لأي صندوق.

    fun hasPin(): Boolean {
        return getPrefs().getString("pin_hash", null)?.isNotEmpty() == true
    }

    fun removePinHash() {
        getPrefs().edit().remove("pin_hash").apply()
    }

    fun savePinHash(pinHash: String) {
        getPrefs().edit().putString("pin_hash", pinHash).apply()
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
    }

    fun getOrCreateDatabasePassphrase(): ByteArray {
        val existing = getDatabasePassphrase()
        if (existing != null && existing.isNotEmpty()) return existing

        val random = SecureRandom()
        val bytes = ByteArray(32)
        random.nextBytes(bytes)
        val passphrase = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
        saveDatabasePassphrase(passphrase)
        return passphrase.toByteArray()
    }

    fun isScreenshotProtectionEnabled(): Boolean {
        return getPrefs().getBoolean("screenshot_protection", true)
    }

    fun setScreenshotProtectionEnabled(enabled: Boolean) {
        getPrefs().edit().putBoolean("screenshot_protection", enabled).apply()
    }

    // ---- كلمات مرور الصناديق الدائمة (permanent mode) ----
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

    // ---- مؤقت القفل التلقائي (auto-lock timer) ----
    fun getAutoLockTimer(): Int {
        return getPrefs().getInt("auto_lock_timer", 0) // 0 = فوري
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

    // ---- قفل إلزامي عند إعادة الفتح ----
    // يُفعَّل عند الخروج من التطبيق عبر زر "إلغاء" في شاشة القفل (finish).
    // أي: المستخدم كان مقفولاً ولم ينجح أي فك قفل، فالعودة يجب أن تطلب الرمز دائماً
    // مهما كانت مدة مؤقت القفل التلقائي (التسجيل في last_stop_time في ON_STOP
    // كان يسمح بإعادة الفتح داخل مدة المؤقت بدون رمز).
    fun isLockedOnExit(): Boolean {
        return getPrefs().getBoolean("locked_on_exit", false)
    }

    fun setLockedOnExit(locked: Boolean) {
        getPrefs().edit().putBoolean("locked_on_exit", locked).apply()
    }
}
