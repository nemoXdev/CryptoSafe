package com.cryptosafe.app

import com.lambdapioneer.argon2kt.Argon2Kt
import com.lambdapioneer.argon2kt.Argon2Mode
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

object CryptoEngine {

    private const val SALT_LENGTH = 16
    private const val IV_LENGTH = 12
    private const val KEY_LENGTH = 256

    private val argon2 by lazy { Argon2Kt() }

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
        val passwordBytes = password.concatToString().toByteArray(Charsets.UTF_8)
        val result = argon2.hash(
            mode = Argon2Mode.ARGON2_ID,
            password = passwordBytes,
            salt = salt,
            tCostInIterations = 4,
            mCostInKibibyte = 131072,
            parallelism = 4,
            hashLengthInBytes = KEY_LENGTH / 8
        )
        passwordBytes.fill(0)
        return result.rawHashAsByteArray()
    }

    fun checkPasswordStrength(password: CharArray): Pair<Int, String> {
        var score = 0
        if (password.size >= 10) score++
        if (password.any { it.isLetter() }) score++
        if (password.any { it.isDigit() }) score++
        if (password.any { !it.isLetterOrDigit() }) score++

        val levelKey = when (score) {
            0, 1 -> "weak"
            2, 3 -> "medium"
            4 -> "strong"
            else -> "weak"
        }
        return Pair(score, levelKey)
    }
}