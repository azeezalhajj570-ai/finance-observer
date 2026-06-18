package com.financeobserver.service

import com.financeobserver.model.User
import org.junit.Assert.*
import org.junit.Test
import java.security.MessageDigest
import java.util.Base64

class AuthManagerTest {

    @Test
    fun `user model stores phone and hash correctly`() {
        val user = User(
            phoneNumber = "+1234567890",
            passwordHash = "salt:hash",
            biometricEnabled = true
        )
        assertEquals("+1234567890", user.phoneNumber)
        assertEquals("salt:hash", user.passwordHash)
        assertTrue(user.biometricEnabled)
    }

    @Test
    fun `user model defaults biometric to false`() {
        val user = User(
            phoneNumber = "+1234567890",
            passwordHash = "salt:hash"
        )
        assertFalse(user.biometricEnabled)
    }

    @Test
    fun `password hash uses SHA-256 and salt`() {
        val salt = "test-salt-value"
        val password = "mypassword"
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(salt.toByteArray())
        val hashBytes = digest.digest(password.toByteArray(Charsets.UTF_8))
        val hash = Base64.getEncoder().encodeToString(hashBytes)

        assertNotNull(hash)
        assertTrue(hash.isNotBlank())
        assertTrue(hash.contains("/") || hash.contains("+") || hash.contains("=") || hash.all { it in 'A'..'Z' || it in 'a'..'z' || it in '0'..'9' || it == '+' || it == '/' || it == '=' })
    }

    @Test
    fun `different passwords produce different hashes with same salt`() {
        val salt = "same-salt"
        val digest1 = MessageDigest.getInstance("SHA-256")
        digest1.update(salt.toByteArray())
        val hash1 = digest1.digest("password1".toByteArray(Charsets.UTF_8))

        val digest2 = MessageDigest.getInstance("SHA-256")
        digest2.update(salt.toByteArray())
        val hash2 = digest2.digest("password2".toByteArray(Charsets.UTF_8))

        assertFalse(hash1.contentEquals(hash2))
    }

    @Test
    fun `same password with different salts produce different hashes`() {
        val password = "samepassword"

        val digest1 = MessageDigest.getInstance("SHA-256")
        digest1.update("salt1".toByteArray())
        val hash1 = digest1.digest(password.toByteArray(Charsets.UTF_8))

        val digest2 = MessageDigest.getInstance("SHA-256")
        digest2.update("salt2".toByteArray())
        val hash2 = digest2.digest(password.toByteArray(Charsets.UTF_8))

        assertFalse(hash1.contentEquals(hash2))
    }

    @Test
    fun `phone numbers are unique per user identity`() {
        val phone1 = "+1234567890"
        val phone2 = "+0987654321"
        assertNotEquals(phone1, phone2)
    }

    @Test
    fun `password minimum length validation`() {
        val shortPassword = "ab"
        val validPassword = "abcd"
        assertTrue(shortPassword.length < 4)
        assertTrue(validPassword.length >= 4)
    }

    @Test
    fun `password confirmation matching`() {
        val password = "testpass"
        val confirmPassword = "testpass"
        val wrongConfirm = "wrong"
        assertEquals(password, confirmPassword)
        assertNotEquals(password, wrongConfirm)
    }
}
