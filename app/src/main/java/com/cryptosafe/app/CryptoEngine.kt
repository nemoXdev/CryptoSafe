package com.cryptosafe.app

import com.lambdapioneer.argon2kt.Argon2Kt
import com.lambdapioneer.argon2kt.Argon2Mode
import java.nio.ByteBuffer
import java.nio.CharBuffer
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

interface KeyDeriver {
    fun derive(
        password: ByteArray,
        salt: ByteArray,
        mCostInKibibyte: Int,
        hashLengthInBytes: Int
    ): ByteArray
}

object CryptoEngine {

    private const val SALT_LENGTH = 16
    private const val IV_LENGTH = 12
    private const val KEY_LENGTH = 256

    /**
     * حقن دالة اشتقاق المفتاح لأغراض الاختبار على JVM (Robolectric).
     * الافتراضي هو Argon2id (Argon2Deriver). لا تعدّلها خارج الاختبارات.
     */
    @Volatile
    internal var keyDeriver: KeyDeriver = Argon2Deriver

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
        return try {
            keyDeriver.derive(passwordBytes, salt, 131072, KEY_LENGTH / 8)
        } finally {
            passwordBytes.fill(0)
        }
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
        var hasUpper = false
        var hasLower = false
        var hasDigit = false
        var hasSymbol = false
        for (c in password) {
            when {
                c.isUpperCase() -> hasUpper = true
                c.isLowerCase() -> hasLower = true
                c.isDigit() -> hasDigit = true
                else -> hasSymbol = true
            }
        }
        val hasAll = hasUpper && hasLower && hasDigit && hasSymbol

        val (score, levelKey) = when {
            length <= 8 -> 1 to "weak"
            length <= 15 && hasAll -> 2 to "medium"
            length > 15 && hasAll -> 4 to "strong"
            else -> 1 to "weak"
        }
        return Pair(score, levelKey)
    }

    fun hashPin(pin: String): String {
        val salt = ByteArray(SALT_LENGTH).also { SecureRandom().nextBytes(it) }
        val pinBytes = pin.toByteArray(Charsets.UTF_8)
        val result = keyDeriver.derive(pinBytes, salt, 65536, 32)
        val combined = salt + result
        return android.util.Base64.encodeToString(combined, android.util.Base64.NO_WRAP)
    }

    fun verifyPin(pin: String, storedHash: String): Boolean {
        return try {
            val decoded = android.util.Base64.decode(storedHash, android.util.Base64.NO_WRAP)
            val salt = decoded.sliceArray(0 until SALT_LENGTH)
            val expectedHash = decoded.sliceArray(SALT_LENGTH until decoded.size)

            val pinBytes = pin.toByteArray(Charsets.UTF_8)
            keyDeriver.derive(pinBytes, salt, 65536, 32).contentEquals(expectedHash)
        } catch (e: Exception) {
            false
        }
    }

    fun hashPasswordForStorage(password: String): String {
        val salt = ByteArray(SALT_LENGTH).also { SecureRandom().nextBytes(it) }
        val passwordBytes = password.toByteArray(Charsets.UTF_8)
        val result = keyDeriver.derive(passwordBytes, salt, 65536, 32)
        val combined = salt + result
        return android.util.Base64.encodeToString(combined, android.util.Base64.NO_WRAP)
    }

    /**
     * يتحقق من كلمة مرور صندوق مقابل الهاش المخزّن (نفس صيغة hashPasswordForStorage).
     * لا يخزن ولا يعيد كلمة المرور نفسها أبداً — فقط true/false.
     */
    fun verifyPasswordForStorage(password: String, storedHash: String): Boolean {
        return try {
            val decoded = android.util.Base64.decode(storedHash, android.util.Base64.NO_WRAP)
            val salt = decoded.sliceArray(0 until SALT_LENGTH)
            val expectedHash = decoded.sliceArray(SALT_LENGTH until decoded.size)

            val passwordBytes = password.toByteArray(Charsets.UTF_8)
            keyDeriver.derive(passwordBytes, salt, 65536, 32).contentEquals(expectedHash)
        } catch (e: Exception) {
            false
        }
    }
}

private object Argon2Deriver : KeyDeriver {
    private val argon2 by lazy { Argon2Kt() }

    override fun derive(
        password: ByteArray,
        salt: ByteArray,
        mCostInKibibyte: Int,
        hashLengthInBytes: Int
    ): ByteArray {
        val result = argon2.hash(
            mode = Argon2Mode.ARGON2_ID,
            password = password,
            salt = salt,
            tCostInIterations = 4,
            mCostInKibibyte = mCostInKibibyte,
            parallelism = 4,
            hashLengthInBytes = hashLengthInBytes
        )
        return result.rawHashAsByteArray()
    }
}