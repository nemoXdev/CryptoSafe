package com.cryptosafe.app

import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * Handles AES-256-GCM encryption/decryption with PBKDF2 key derivation.
 * All operations run locally on the device hardware. No data leaves the device.
 */
object CryptoEngine {

    private const val SALT_LENGTH = 16
    private const val IV_LENGTH = 12
    private const val PBKDF2_ITERATIONS = 310_000
    private const val KEY_LENGTH = 256

    /**
     * Encrypts plaintext using a password.
     * Returns a single Base64 string containing: [Salt][IV][Ciphertext+AuthTag]
     */
    fun encrypt(plainText: String, password: CharArray): String {
        val salt = ByteArray(SALT_LENGTH).also { SecureRandom().nextBytes(it) }
        val iv = ByteArray(IV_LENGTH).also { SecureRandom().nextBytes(it) }
        val key = deriveKey(password, salt)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(128, iv))
        val ciphertext = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))

        val combined = salt + iv + ciphertext
        return android.util.Base64.encodeToString(combined, android.util.Base64.NO_WRAP)
    }

    /**
     * Decrypts a Base64 encoded string using the same password.
     * Fails if password is incorrect or data is tampered.
     */
    fun decrypt(encoded: String, password: CharArray): String {
        val combined = android.util.Base64.decode(encoded, android.util.Base64.NO_WRAP)
        if (combined.size < SALT_LENGTH + IV_LENGTH + 16) {
            throw IllegalArgumentException("Invalid encrypted data format")
        }

        val salt = combined.sliceArray(0 until SALT_LENGTH)
        val iv = combined.sliceArray(SALT_LENGTH until SALT_LENGTH + IV_LENGTH)
        val ciphertext = combined.sliceArray(SALT_LENGTH + IV_LENGTH until combined.size)

        val key = deriveKey(password, salt)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(128, iv))

        return cipher.doFinal(ciphertext).toString(Charsets.UTF_8)
    }

    private fun deriveKey(password: CharArray, salt: ByteArray): ByteArray {
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(password, salt, PBKDF2_ITERATIONS, KEY_LENGTH)
        val key = factory.generateSecret(spec).encoded
        // Clear the password spec for security
        spec.clearPassword()
        return key
    }

    /**
     * Evaluates password strength. Returns score (0-4) and level text.
     */
    fun checkPasswordStrength(password: CharArray): Pair<Int, String> {
        var score = 0
        val passStr = password.concatToString()
        if (passStr.length >= 10) score++
        if (passStr.any { it.isLetter() }) score++
        if (passStr.any { it.isDigit() }) score++
        if (passStr.any { !it.isLetterOrDigit() }) score++

        val level = when (score) {
            0, 1 -> "Weak / ضعيف"
            2, 3 -> "Medium / متوسط"
            4 -> "Strong / قوي"
            else -> "Weak / ضعيف"
        }
        return Pair(score, level)
    }
}
