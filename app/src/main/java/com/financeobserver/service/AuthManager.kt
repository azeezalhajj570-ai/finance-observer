package com.financeobserver.service

import android.content.Context
import androidx.biometric.BiometricManager
import com.financeobserver.database.UserDao
import com.financeobserver.model.User
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

class AuthManager(
    private val context: Context,
    private val userDao: UserDao
) {
    private val prefs = context.getSharedPreferences("finance_observer_auth", Context.MODE_PRIVATE)
    private val saltCache = mutableMapOf<String, String>()

    fun isLoggedIn(): Boolean = prefs.contains(KEY_SESSION_PHONE)

    fun getCurrentUserPhone(): String? = prefs.getString(KEY_SESSION_PHONE, null)

    fun logout() {
        prefs.edit().remove(KEY_SESSION_PHONE).apply()
    }

    fun isBiometricAvailable(): Boolean {
        val manager = BiometricManager.from(context)
        return manager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG
        ) == BiometricManager.BIOMETRIC_SUCCESS
    }

    suspend fun signUp(phone: String, password: String): Result<User> {
        if (phone.isBlank()) {
            return Result.failure(Exception("Phone number is required"))
        }
        if (password.length < 4) {
            return Result.failure(Exception("Password must be at least 4 characters"))
        }
        val existing = userDao.findByPhone(phone)
        if (existing != null) {
            return Result.failure(Exception("Phone number already registered"))
        }
        val salt = generateSalt()
        val hash = hashPassword(password, salt)
        val user = User(
            phoneNumber = phone,
            passwordHash = "$salt:$hash"
        )
        userDao.insert(user)
        createSession(phone)
        return Result.success(user)
    }

    suspend fun signIn(phone: String, password: String): Result<User> {
        val user = userDao.findByPhone(phone)
            ?: return Result.failure(Exception("Phone number not found"))
        val parts = user.passwordHash.split(":", limit = 2)
        if (parts.size != 2) return Result.failure(Exception("Invalid credentials"))
        val salt = parts[0]
        val storedHash = parts[1]
        val computedHash = hashPassword(password, salt)
        if (storedHash != computedHash) {
            return Result.failure(Exception("Invalid password"))
        }
        createSession(phone)
        return Result.success(user)
    }

    suspend fun getCurrentUser(): User? {
        val phone = getCurrentUserPhone() ?: return null
        return userDao.findByPhone(phone)
    }

    suspend fun setBiometricEnabled(enabled: Boolean) {
        val phone = getCurrentUserPhone() ?: return
        userDao.setBiometricEnabled(phone, enabled)
    }

    suspend fun isBiometricEnabled(): Boolean {
        val phone = getCurrentUserPhone() ?: return false
        return userDao.findByPhone(phone)?.biometricEnabled ?: false
    }

    private fun createSession(phone: String) {
        prefs.edit().putString(KEY_SESSION_PHONE, phone).apply()
    }

    private fun generateSalt(): String {
        val random = SecureRandom()
        val bytes = ByteArray(16)
        random.nextBytes(bytes)
        return Base64.getEncoder().encodeToString(bytes)
    }

    private fun hashPassword(password: String, salt: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(salt.toByteArray())
        val hashBytes = digest.digest(password.toByteArray(Charsets.UTF_8))
        return Base64.getEncoder().encodeToString(hashBytes)
    }

    companion object {
        private const val KEY_SESSION_PHONE = "session_phone"
    }
}
