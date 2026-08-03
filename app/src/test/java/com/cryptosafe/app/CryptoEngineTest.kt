package com.cryptosafe.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * اختبارات JVM نقية (لا تحتاج Robolectric) للمنطق الخالي من Android:
 * مولّد كلمات المرور وقياس قوة كلمة المرور.
 */
class CryptoEngineTest {

    @Test
    fun `generatePassword returns correct length`() {
        assertEquals(8, CryptoEngine.generatePassword(8).length)
        assertEquals(16, CryptoEngine.generatePassword(16).length)
        assertEquals(32, CryptoEngine.generatePassword(32).length)
        assertEquals(64, CryptoEngine.generatePassword(64).length)
    }

    @Test
    fun `generatePassword contains uppercase`() {
        val password = CryptoEngine.generatePassword(100)
        assertTrue(password.any { it.isUpperCase() })
    }

    @Test
    fun `generatePassword contains lowercase`() {
        val password = CryptoEngine.generatePassword(100)
        assertTrue(password.any { it.isLowerCase() })
    }

    @Test
    fun `generatePassword contains digit`() {
        val password = CryptoEngine.generatePassword(100)
        assertTrue(password.any { it.isDigit() })
    }

    @Test
    fun `generatePassword contains symbol`() {
        val password = CryptoEngine.generatePassword(100)
        assertTrue(password.any { !it.isLetterOrDigit() })
    }

    @Test
    fun `generatePassword different each time`() {
        val p1 = CryptoEngine.generatePassword(32)
        val p2 = CryptoEngine.generatePassword(32)
        assertNotEquals(p1, p2)
    }

    @Test
    fun `generatePassword never contains ambiguous whitespace`() {
        val password = CryptoEngine.generatePassword(200)
        assertTrue(password.none { it.isWhitespace() })
    }

    @Test
    fun `short password is weak`() {
        val (score, level) = CryptoEngine.checkPasswordStrength("abc".toCharArray())
        assertEquals(1, score)
        assertEquals("weak", level)
    }

    @Test
    fun `long complex password is strong`() {
        val (score, level) = CryptoEngine.checkPasswordStrength("MyStr0ng!Passw0rd#2024".toCharArray())
        assertEquals(4, score)
        assertEquals("strong", level)
    }

    @Test
    fun `8 chars or less is always weak`() {
        val (score, _) = CryptoEngine.checkPasswordStrength("Abc1234!".toCharArray())
        assertEquals(1, score)
    }

    @Test
    fun `long without all types is weak`() {
        val (score, level) = CryptoEngine.checkPasswordStrength("aaaaaaaaaaaa".toCharArray())
        assertEquals(1, score)
        assertEquals("weak", level)
    }

    @Test
    fun `15 chars with all types is medium`() {
        val (score, level) = CryptoEngine.checkPasswordStrength("Aaaa1bbb!cccc9".toCharArray())
        assertEquals(2, score)
        assertEquals("medium", level)
    }

    @Test
    fun `empty password is weak`() {
        val (score, level) = CryptoEngine.checkPasswordStrength("".toCharArray())
        assertEquals(1, score)
        assertEquals("weak", level)
    }
}
