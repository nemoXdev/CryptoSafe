package com.cryptosafe.app

import com.lambdapioneer.argon2kt.Argon2Kt
import com.lambdapioneer.argon2kt.Argon2Mode
import java.nio.ByteBuffer
import java.nio.CharBuffer
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
        val charBuffer = CharBuffer.wrap(password)
        val byteBuffer = Charsets.UTF_8.newEncoder().encode(charBuffer)
        val passwordBytes = ByteArray(byteBuffer.remaining())
        byteBuffer.get(passwordBytes)
        byteBuffer.clear()
        charBuffer.clear()

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

    fun generatePassword(length: Int = 16): String {
        val upper = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
        val lower = "abcdefghijklmnopqrstuvwxyz"
        val digits = "0123456789"
        val symbols = "!@#\$%^&*()-_=+"
        val random = SecureRandom()
        val perType = length / 4
        val remainder = length % 4
        val result = mutableListOf<Char>()
        repeat(perType + if (remainder > 0) 1 else 0) { result.add(upper[random.nextInt(upper.length)]) }
        repeat(perType + if (remainder > 1) 1 else 0) { result.add(lower[random.nextInt(lower.length)]) }
        repeat(perType + if (remainder > 2) 1 else 0) { result.add(digits[random.nextInt(digits.length)]) }
        repeat(perType) { result.add(symbols[random.nextInt(symbols.length)]) }
        result.shuffle(random)
        return result.joinToString("")
    }

    fun checkPasswordStrength(password: CharArray): Pair<Int, String> {
        val length = password.size
        val str = String(password)
        val hasUpper = str.any { it.isUpperCase() }
        val hasLower = str.any { it.isLowerCase() }
        val hasDigit = str.any { it.isDigit() }
        val hasSymbol = str.any { !it.isLetterOrDigit() }
        val hasAll = hasUpper && hasLower && hasDigit && hasSymbol

        val (score, levelKey) = when {
            length <= 8 -> 1 to "weak"
            length <= 15 && hasAll -> 2 to "medium"
            length > 15 && hasAll -> 4 to "strong"
            else -> 1 to "weak"
        }
        return Pair(score, levelKey)
    }
}