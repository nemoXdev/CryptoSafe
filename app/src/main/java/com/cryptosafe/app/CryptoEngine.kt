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

    const val SALT_LENGTH = 16
    private const val IV_LENGTH = 12
    private const val KEY_LENGTH = 256

    
    
    private const val FORMAT_VERSION: Byte = 0x01
    private const val AAD_CONTEXT = "cryptosafe.box.v1"
    
    private const val MIN_VERSIONED_SIZE = 1 + SALT_LENGTH + IV_LENGTH + 16

    
    @Volatile
    internal var keyDeriver: KeyDeriver = Argon2Deriver

    
    fun generateSalt(): ByteArray = ByteArray(SALT_LENGTH).also { SecureRandom().nextBytes(it) }

    
    fun deriveBoxKey(password: CharArray, salt: ByteArray): ByteArray {
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

    
    fun encryptWithKey(plainText: String, key: ByteArray, salt: ByteArray): String {
        val iv = ByteArray(IV_LENGTH).also { SecureRandom().nextBytes(it) }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(128, iv))
        cipher.updateAAD(AAD_CONTEXT.toByteArray(Charsets.UTF_8))
        val ciphertext = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))

        val combined = byteArrayOf(FORMAT_VERSION) + salt + iv + ciphertext
        return android.util.Base64.encodeToString(combined, android.util.Base64.NO_WRAP)
    }

    
    fun decryptWithKey(encoded: String, key: ByteArray, expectedSalt: ByteArray): String? {
        return try {
            val combined = android.util.Base64.decode(encoded, android.util.Base64.NO_WRAP)
            if (combined.size < MIN_VERSIONED_SIZE) return null

            val headerSalt = if (combined[0] == FORMAT_VERSION) {
                combined.sliceArray(1 until 1 + SALT_LENGTH)
            } else {
                combined.sliceArray(0 until SALT_LENGTH)
            }
            if (!headerSalt.contentEquals(expectedSalt)) return null

            val offset = if (combined[0] == FORMAT_VERSION) 1 else 0
            val iv = combined.sliceArray(offset + SALT_LENGTH until offset + SALT_LENGTH + IV_LENGTH)
            val ciphertext = combined.sliceArray(offset + SALT_LENGTH + IV_LENGTH until combined.size)

            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(128, iv))
            if (combined[0] == FORMAT_VERSION) {
                cipher.updateAAD(AAD_CONTEXT.toByteArray(Charsets.UTF_8))
            }
            cipher.doFinal(ciphertext).toString(Charsets.UTF_8)
        } catch (e: Exception) {
            null
        }
    }

    fun encrypt(plainText: String, password: CharArray): String {
        val salt = ByteArray(SALT_LENGTH).also { SecureRandom().nextBytes(it) }
        val iv = ByteArray(IV_LENGTH).also { SecureRandom().nextBytes(it) }
        val key = deriveKey(password, salt)

        try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(128, iv))
            cipher.updateAAD(AAD_CONTEXT.toByteArray(Charsets.UTF_8))
            val ciphertext = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))

            val combined = byteArrayOf(FORMAT_VERSION) + salt + iv + ciphertext
            return android.util.Base64.encodeToString(combined, android.util.Base64.NO_WRAP)
        } finally {
            key.fill(0)
        }
    }

    fun decrypt(encoded: String, password: CharArray): String {
        val combined = android.util.Base64.decode(encoded, android.util.Base64.NO_WRAP)

        
        
        
        if (combined.size >= MIN_VERSIONED_SIZE && combined[0] == FORMAT_VERSION) {
            return try {
                decryptLayout(combined, offset = 1, password, withAad = true)
            } catch (_: Exception) {
                decryptLayout(combined, offset = 0, password, withAad = false)
            }
        }

        return decryptLayout(combined, offset = 0, password, withAad = false)
    }

    private fun decryptLayout(
        combined: ByteArray,
        offset: Int,
        password: CharArray,
        withAad: Boolean
    ): String {
        val required = offset + SALT_LENGTH + IV_LENGTH + 16
        if (combined.size < required) {
            throw IllegalArgumentException("Invalid encrypted data format")
        }

        val salt = combined.sliceArray(offset until offset + SALT_LENGTH)
        val iv = combined.sliceArray(offset + SALT_LENGTH until offset + SALT_LENGTH + IV_LENGTH)
        val ciphertext = combined.sliceArray(offset + SALT_LENGTH + IV_LENGTH until combined.size)

        val key = deriveKey(password, salt)
        try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(128, iv))
            if (withAad) cipher.updateAAD(AAD_CONTEXT.toByteArray(Charsets.UTF_8))

            return cipher.doFinal(ciphertext).toString(Charsets.UTF_8)
        } finally {
            key.fill(0)
        }
    }

    private fun deriveKey(password: CharArray, salt: ByteArray): ByteArray =
        deriveBoxKey(password, salt)

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
        
        val combined = byteArrayOf(FORMAT_VERSION) + salt + result
        return android.util.Base64.encodeToString(combined, android.util.Base64.NO_WRAP)
    }

    fun verifyPin(pin: String, storedHash: String): Boolean {
        return try {
            val (salt, expectedHash) = splitStoredHash(storedHash)
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
        val combined = byteArrayOf(FORMAT_VERSION) + salt + result
        return android.util.Base64.encodeToString(combined, android.util.Base64.NO_WRAP)
    }

    
    fun verifyPasswordForStorage(password: String, storedHash: String): Boolean {
        return try {
            val (salt, expectedHash) = splitStoredHash(storedHash)
            val passwordBytes = password.toByteArray(Charsets.UTF_8)
            keyDeriver.derive(passwordBytes, salt, 65536, 32).contentEquals(expectedHash)
        } catch (e: Exception) {
            false
        }
    }

    
    private fun splitStoredHash(decodedBase64: String): Pair<ByteArray, ByteArray> {
        val decoded = android.util.Base64.decode(decodedBase64, android.util.Base64.NO_WRAP)
        return if (decoded.size >= 1 + SALT_LENGTH + 32 && decoded[0] == FORMAT_VERSION) {
            decoded.copyOfRange(1, 1 + SALT_LENGTH) to
                decoded.copyOfRange(1 + SALT_LENGTH, 1 + SALT_LENGTH + 32)
        } else if (decoded.size == SALT_LENGTH + 32) {
            decoded.copyOfRange(0, SALT_LENGTH) to
                decoded.copyOfRange(SALT_LENGTH, SALT_LENGTH + 32)
        } else {
            throw IllegalArgumentException("Invalid stored hash format")
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