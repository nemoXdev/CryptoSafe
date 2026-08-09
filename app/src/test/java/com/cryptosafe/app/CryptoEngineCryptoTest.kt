package com.cryptosafe.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec


@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CryptoEngineCryptoTest {

    private val fakeDeriver = object : KeyDeriver {
        override fun derive(
            password: ByteArray,
            salt: ByteArray,
            mCostInKibibyte: Int,
            hashLengthInBytes: Int
        ): ByteArray {
            val md = MessageDigest.getInstance("SHA-256")
            md.update(password)
            md.update(salt)
            val full = md.digest()
            return full.copyOf(hashLengthInBytes)
        }
    }

    @Before
    fun setUp() {
        CryptoEngine.keyDeriver = fakeDeriver
    }

    @Test
    fun `encrypt then decrypt returns original`() {
        val password = "StrongP@ssw0rd!".toCharArray()
        val plain = "Top secret message to protect"
        val encrypted = CryptoEngine.encrypt(plain, password)
        assertNotEquals(plain, encrypted)
        assertEquals(plain, CryptoEngine.decrypt(encrypted, password))
    }

    @Test
    fun `encrypt supports unicode`() {
        val password = "Hard secret 123!".toCharArray()
        val plain = "Hello, secure world! 🌍"
        val encrypted = CryptoEngine.encrypt(plain, password)
        assertEquals(plain, CryptoEngine.decrypt(encrypted, password))
    }

    @Test
    fun `wrong password fails to decrypt`() {
        val encrypted = CryptoEngine.encrypt("secret", "correct-password".toCharArray())
        assertThrows { CryptoEngine.decrypt(encrypted, "wrong-password".toCharArray()) }
    }

    @Test
    fun `same plaintext produces different ciphertext`() {
        val password = "pw".toCharArray()
        val c1 = CryptoEngine.encrypt("hello", password)
        val c2 = CryptoEngine.encrypt("hello", password)
        assertNotEquals(c1, c2)
    }

    @Test
    fun `tampered ciphertext fails GCM auth`() {
        val encrypted = CryptoEngine.encrypt("authenticated-data", "pw".toCharArray())
        val decoded = android.util.Base64.decode(encrypted, android.util.Base64.NO_WRAP)
        decoded[decoded.size - 1] = (decoded[decoded.size - 1].toInt() xor 0x01).toByte()
        val tampered = android.util.Base64.encodeToString(decoded, android.util.Base64.NO_WRAP)
        assertThrows { CryptoEngine.decrypt(tampered, "pw".toCharArray()) }
    }

    @Test
    fun `legacy format without version byte still decrypts`() {
        
        
        val password = "legacy-pass".toCharArray()
        val salt = "1234567890123456".toByteArray(Charsets.UTF_8)
        val iv = ByteArray(12) { it.toByte() }
        val passwordBytes = password.joinToString("").toByteArray(Charsets.UTF_8)
        val key = CryptoEngine.keyDeriver.derive(passwordBytes, salt, 131072, 32)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(128, iv))
        val ciphertext = cipher.doFinal("old message".toByteArray(Charsets.UTF_8))

        val legacy = android.util.Base64.encodeToString(
            salt + iv + ciphertext,
            android.util.Base64.NO_WRAP
        )
        assertEquals("old message", CryptoEngine.decrypt(legacy, password))
    }

    @Test
    fun `legacy ciphertext whose first salt byte equals version still decrypts`() {
        
        
        val password = "legacy-pass".toCharArray()
        val salt = byteArrayOf(0x01) + ByteArray(15) { it.toByte() }
        val iv = ByteArray(12) { it.toByte() }
        val passwordBytes = password.joinToString("").toByteArray(Charsets.UTF_8)
        val key = CryptoEngine.keyDeriver.derive(passwordBytes, salt, 131072, 32)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(128, iv))
        val ciphertext = cipher.doFinal("ambiguous".toByteArray(Charsets.UTF_8))

        val legacy = android.util.Base64.encodeToString(
            salt + iv + ciphertext,
            android.util.Base64.NO_WRAP
        )
        assertEquals("ambiguous", CryptoEngine.decrypt(legacy, password))
    }

    @Test
    fun `corrupted base64 fails`() {
        assertThrows { CryptoEngine.decrypt("!!!not-valid-base64!!!", "p".toCharArray()) }
    }

    @Test
    fun `empty string fails`() {
        assertThrows { CryptoEngine.decrypt("", "p".toCharArray()) }
    }

    @Test
    fun `random garbage fails`() {
        assertThrows { CryptoEngine.decrypt("dGhpcyBpcyBub3QgZW5jb2RlZA==", "p".toCharArray()) }
    }

    @Test
    fun `too short data fails`() {
        assertThrows { CryptoEngine.decrypt("AAAA", "p".toCharArray()) }
    }

    @Test
    fun `pin hash verifies correctly`() {
        val hash = CryptoEngine.hashPin("MyP@ss123")
        assertTrue(CryptoEngine.verifyPin("MyP@ss123", hash))
        assertFalse(CryptoEngine.verifyPin("WrongP@ss", hash))
    }

    @Test
    fun `pin hash different each time`() {
        assertNotEquals(CryptoEngine.hashPin("SamePin1"), CryptoEngine.hashPin("SamePin1"))
    }

    @Test
    fun `pin verify with garbage hash returns false`() {
        assertFalse(CryptoEngine.verifyPin("AnyPin", "garbage-not-base64!!"))
    }

    @Test
    fun `box password hash verifies correctly`() {
        val hash = CryptoEngine.hashPasswordForStorage("BoxP@ssw0rd")
        assertTrue(CryptoEngine.verifyPasswordForStorage("BoxP@ssw0rd", hash))
        assertFalse(CryptoEngine.verifyPasswordForStorage("WrongPass", hash))
    }

    @Test
    fun `box password hash different each time`() {
        assertNotEquals(
            CryptoEngine.hashPasswordForStorage("SameBox1"),
            CryptoEngine.hashPasswordForStorage("SameBox1")
        )
    }

    @Test
    fun `box password verify with garbage hash returns false`() {
        assertFalse(CryptoEngine.verifyPasswordForStorage("AnyPass", "garbage-not-base64!!"))
    }

    @Test
    fun `legacy unversioned pin hash still verifies`() {
        val pin = "LegacyPin!1"
        val salt = "1234567890123456".toByteArray(Charsets.UTF_8)
        val derived = CryptoEngine.keyDeriver.derive(
            pin.toByteArray(Charsets.UTF_8), salt, 65536, 32
        )
        val legacy = android.util.Base64.encodeToString(
            salt + derived, android.util.Base64.NO_WRAP
        )
        assertTrue(CryptoEngine.verifyPin(pin, legacy))
        assertFalse(CryptoEngine.verifyPin("OtherPin1!", legacy))
    }

    @Test
    fun `legacy unversioned box password hash still verifies`() {
        val password = "LegacyBoxPass!"
        val salt = "fedcba9876543210".toByteArray(Charsets.UTF_8)
        val derived = CryptoEngine.keyDeriver.derive(
            password.toByteArray(Charsets.UTF_8), salt, 65536, 32
        )
        val legacy = android.util.Base64.encodeToString(
            salt + derived, android.util.Base64.NO_WRAP
        )
        assertTrue(CryptoEngine.verifyPasswordForStorage(password, legacy))
        assertFalse(CryptoEngine.verifyPasswordForStorage("WrongBoxPass", legacy))
    }

    @Test
    fun `versioned hashes carry a version byte and verify`() {
        val pinHash = CryptoEngine.hashPin("NewPin!1")
        val decoded = android.util.Base64.decode(pinHash, android.util.Base64.NO_WRAP)
        assertEquals(0x01.toByte(), decoded[0])
        assertTrue(CryptoEngine.verifyPin("NewPin!1", pinHash))
        assertFalse(CryptoEngine.verifyPin("WrongPin!", pinHash))
    }

    @Test
    fun `hash verify with wrong length returns false`() {
        val tooShort = android.util.Base64.encodeToString(
            ByteArray(8), android.util.Base64.NO_WRAP
        )
        assertFalse(CryptoEngine.verifyPin("AnyPin", tooShort))
        assertFalse(CryptoEngine.verifyPasswordForStorage("AnyPass", tooShort))
    }

    @Test
    fun `versioned-looking ciphertext too short fails cleanly`() {
        
        val short = android.util.Base64.encodeToString(
            byteArrayOf(0x01, 0x02, 0x03), android.util.Base64.NO_WRAP
        )
        assertThrows { CryptoEngine.decrypt(short, "p".toCharArray()) }
    }

    @Test
    fun `versioned ciphertext with wrong password fails cleanly`() {
        val encrypted = CryptoEngine.encrypt("data", "correct-password".toCharArray())
        assertThrows { CryptoEngine.decrypt(encrypted, "wrong-password".toCharArray()) }
    }

    @Test
    fun `deriveBoxKey is deterministic for same password and salt`() {
        val salt = CryptoEngine.generateSalt()
        val k1 = CryptoEngine.deriveBoxKey("BoxPass!1".toCharArray(), salt)
        val k2 = CryptoEngine.deriveBoxKey("BoxPass!1".toCharArray(), salt)
        assertTrue(k1.contentEquals(k2))
        assertEquals(32, k1.size)
        k1.fill(0)
        k2.fill(0)
    }

    @Test
    fun `deriveBoxKey differs with a different salt`() {
        val salt1 = CryptoEngine.generateSalt()
        val salt2 = CryptoEngine.generateSalt()
        val k1 = CryptoEngine.deriveBoxKey("BoxPass!1".toCharArray(), salt1)
        val k2 = CryptoEngine.deriveBoxKey("BoxPass!1".toCharArray(), salt2)
        assertFalse(k1.contentEquals(k2))
        k1.fill(0)
        k2.fill(0)
    }

    @Test
    fun `encryptWithKey and decryptWithKey roundtrip`() {
        val salt = CryptoEngine.generateSalt()
        val key = CryptoEngine.deriveBoxKey("BoxPass!1".toCharArray(), salt)
        try {
            val cipherText = CryptoEngine.encryptWithKey("fast-path message", key, salt)
            assertNotEquals("fast-path message", cipherText)
            assertEquals("fast-path message", CryptoEngine.decryptWithKey(cipherText, key, salt))
        } finally {
            key.fill(0)
        }
    }

    @Test
    fun `decryptWithKey returns null when header salt differs (legacy message)`() {
        
        val boxSalt = CryptoEngine.generateSalt()
        val legacy = CryptoEngine.encrypt("old message", "pass!1".toCharArray())
        val key = CryptoEngine.deriveBoxKey("pass!1".toCharArray(), boxSalt)
        try {
            assertNull(CryptoEngine.decryptWithKey(legacy, key, boxSalt))
            
            assertEquals("old message", CryptoEngine.decrypt(legacy, "pass!1".toCharArray()))
        } finally {
            key.fill(0)
        }
    }

    @Test
    fun `decryptWithKey returns null for tampered ciphertext`() {
        val salt = CryptoEngine.generateSalt()
        val key = CryptoEngine.deriveBoxKey("pass!1".toCharArray(), salt)
        try {
            val cipherText = CryptoEngine.encryptWithKey("authenticated", key, salt)
            val decoded = android.util.Base64.decode(cipherText, android.util.Base64.NO_WRAP)
            decoded[decoded.size - 1] = (decoded[decoded.size - 1].toInt() xor 0x01).toByte()
            val tampered = android.util.Base64.encodeToString(decoded, android.util.Base64.NO_WRAP)
            assertNull(CryptoEngine.decryptWithKey(tampered, key, salt))
        } finally {
            key.fill(0)
        }
    }

    @Test
    fun `decryptWithKey returns null for wrong password key`() {
        val salt = CryptoEngine.generateSalt()
        val rightKey = CryptoEngine.deriveBoxKey("right!1".toCharArray(), salt)
        val wrongKey = CryptoEngine.deriveBoxKey("wrong!1".toCharArray(), salt)
        try {
            val cipherText = CryptoEngine.encryptWithKey("secret", rightKey, salt)
            assertNull(CryptoEngine.decryptWithKey(cipherText, wrongKey, salt))
        } finally {
            rightKey.fill(0)
            wrongKey.fill(0)
        }
    }

    private fun assertThrows(block: () -> Unit) {
        try {
            block()
            fail("Should have thrown an exception")
        } catch (e: Exception) {
            assertTrue(true)
        }
    }
}
