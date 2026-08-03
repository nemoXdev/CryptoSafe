package com.cryptosafe.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.security.MessageDigest

/**
 * اختبارات محرك التشفير الحقيقية تعمل على JVM عبر Robolectric (بدون محاكي).
 * تُستبدل خوارزمية Argon2 (JNI أندرويد) بدالة SHA-256 حتمية لاختبار منطق
 * التشفير/فك التشفير نفسه. سلوك Argon2 نفسه يبقى غير قابل للاختبار على JVM.
 */
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
        val password = "كلمة سر صعبة 123!".toCharArray()
        val plain = "مرحباً بالعالم 🌍"
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

    private fun assertThrows(block: () -> Unit) {
        try {
            block()
            fail("Should have thrown an exception")
        } catch (e: Exception) {
            assertTrue(true)
        }
    }
}
