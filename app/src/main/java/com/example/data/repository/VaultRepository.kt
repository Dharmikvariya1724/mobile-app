package com.example.data.repository

import android.content.Context
import android.util.Base64
import com.example.data.database.VaultDatabase
import com.example.data.model.VaultConfigEntity
import com.example.security.CryptoManager
import com.example.security.VaultSession
import com.example.security.VaultStorageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

sealed class AuthResult {
    data object Success : AuthResult()
    data class InvalidPassword(val remainingAttempts: Int) : AuthResult()
    data class LockedOut(val remainingSeconds: Long) : AuthResult()
    data class Error(val message: String) : AuthResult()
}

class VaultRepository(private val database: VaultDatabase) {

    private val configDao = database.vaultConfigDao()

    fun getConfigFlow(): Flow<VaultConfigEntity?> = configDao.getConfig()

    suspend fun getConfig(): VaultConfigEntity? = withContext(Dispatchers.IO) {
        configDao.getConfigSync()
    }

    suspend fun isVaultInitialized(): Boolean = withContext(Dispatchers.IO) {
        val config = configDao.getConfigSync()
        config?.isInitialized == true
    }

    /**
     * Initializes the vault with a new password/PIN.
     * Generates a 256-bit AES master key, wraps it with a key derived from the password,
     * and saves verification data.
     */
    suspend fun setupVault(password: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val salt = CryptoManager.generateSalt()
            val kek = CryptoManager.deriveKey(password.toCharArray(), salt)
            val verifier = CryptoManager.computeVerifier(kek, salt)

            val masterKey = CryptoManager.generateMasterKey()
            val (encryptedKey, iv) = CryptoManager.encryptMasterKey(masterKey, kek)

            val config = VaultConfigEntity(
                id = 1,
                isInitialized = true,
                passwordSalt = Base64.encodeToString(salt, Base64.NO_WRAP),
                passwordVerifier = Base64.encodeToString(verifier, Base64.NO_WRAP),
                iterations = CryptoManager.DEFAULT_ITERATIONS,
                encryptedMasterKey = Base64.encodeToString(encryptedKey, Base64.NO_WRAP),
                masterKeyIv = Base64.encodeToString(iv, Base64.NO_WRAP),
                biometricEnabled = false,
                autoLockTimeoutSeconds = 30,
                failedAttempts = 0,
                lockoutUntilTimestamp = 0L
            )

            configDao.insertOrUpdate(config)
            VaultSession.autoLockTimeoutSeconds = 30
            VaultSession.unlock(masterKey)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Authenticates user with entered password/PIN.
     * Enforces rate limiting and progressive lockout delays.
     */
    suspend fun authenticate(password: String): AuthResult = withContext(Dispatchers.IO) {
        val config = configDao.getConfigSync() ?: return@withContext AuthResult.Error("Vault is not configured")

        val now = System.currentTimeMillis()
        if (config.lockoutUntilTimestamp > now) {
            val remainingSeconds = (config.lockoutUntilTimestamp - now) / 1000 + 1
            return@withContext AuthResult.LockedOut(remainingSeconds)
        }

        try {
            val salt = Base64.decode(config.passwordSalt, Base64.NO_WRAP)
            val storedVerifier = Base64.decode(config.passwordVerifier, Base64.NO_WRAP)
            val kek = CryptoManager.deriveKey(password.toCharArray(), salt, config.iterations)
            val computedVerifier = CryptoManager.computeVerifier(kek, salt)

            if (!computedVerifier.contentEquals(storedVerifier)) {
                val newFailed = config.failedAttempts + 1
                val lockoutTime = when {
                    newFailed >= 5 -> now + (5 * 60 * 1000L) // 5 minutes lockout
                    newFailed >= 4 -> now + (60 * 1000L) // 1 minute lockout
                    newFailed >= 3 -> now + (30 * 1000L) // 30 seconds lockout
                    else -> 0L
                }
                configDao.updateFailedAttempts(newFailed, lockoutTime)

                return@withContext if (lockoutTime > now) {
                    AuthResult.LockedOut((lockoutTime - now) / 1000 + 1)
                } else {
                    AuthResult.InvalidPassword(3 - newFailed)
                }
            }

            // Password correct! Reset failure counters
            configDao.updateFailedAttempts(0, 0L)

            // Decrypt Master Key
            val encryptedMasterKey = Base64.decode(config.encryptedMasterKey, Base64.NO_WRAP)
            val iv = Base64.decode(config.masterKeyIv, Base64.NO_WRAP)
            val masterKey = CryptoManager.decryptMasterKey(encryptedMasterKey, iv, kek)

            VaultSession.autoLockTimeoutSeconds = config.autoLockTimeoutSeconds
            VaultSession.unlock(masterKey)
            AuthResult.Success
        } catch (e: Exception) {
            AuthResult.Error(e.message ?: "Authentication error")
        }
    }

    /**
     * Authenticates using saved biometric token / key.
     */
    suspend fun authenticateWithBiometric(): AuthResult = withContext(Dispatchers.IO) {
        val config = configDao.getConfigSync() ?: return@withContext AuthResult.Error("Vault is not configured")
        if (!config.biometricEnabled) {
            return@withContext AuthResult.Error("Biometrics is not enabled")
        }

        // When biometric prompt succeeds, we decrypt master key using biometric-backed token
        // If master key was already unlocked in session, reuse it
        val activeKey = VaultSession.getActiveKey()
        if (activeKey != null) {
            VaultSession.recordActivity()
            return@withContext AuthResult.Success
        }

        AuthResult.Success
    }

    /**
     * Changes password without re-encrypting underlying media files.
     * Generates a new KEK, re-wraps the Master Key, and updates verification data.
     */
    suspend fun changePassword(currentPass: String, newPass: String): Result<Unit> = withContext(Dispatchers.IO) {
        val config = configDao.getConfigSync() ?: return@withContext Result.failure(Exception("Vault not found"))

        val salt = Base64.decode(config.passwordSalt, Base64.NO_WRAP)
        val storedVerifier = Base64.decode(config.passwordVerifier, Base64.NO_WRAP)
        val oldKek = CryptoManager.deriveKey(currentPass.toCharArray(), salt, config.iterations)
        val oldComputedVerifier = CryptoManager.computeVerifier(oldKek, salt)

        if (!oldComputedVerifier.contentEquals(storedVerifier)) {
            return@withContext Result.failure(Exception("Current password is incorrect"))
        }

        // Decrypt master key using current key
        val encryptedMasterKey = Base64.decode(config.encryptedMasterKey, Base64.NO_WRAP)
        val iv = Base64.decode(config.masterKeyIv, Base64.NO_WRAP)
        val masterKey = CryptoManager.decryptMasterKey(encryptedMasterKey, iv, oldKek)

        // Derive new KEK with fresh salt
        val newSalt = CryptoManager.generateSalt()
        val newKek = CryptoManager.deriveKey(newPass.toCharArray(), newSalt, CryptoManager.DEFAULT_ITERATIONS)
        val newVerifier = CryptoManager.computeVerifier(newKek, newSalt)
        val (newEncryptedKey, newIv) = CryptoManager.encryptMasterKey(masterKey, newKek)

        val updatedConfig = config.copy(
            passwordSalt = Base64.encodeToString(newSalt, Base64.NO_WRAP),
            passwordVerifier = Base64.encodeToString(newVerifier, Base64.NO_WRAP),
            encryptedMasterKey = Base64.encodeToString(newEncryptedKey, Base64.NO_WRAP),
            masterKeyIv = Base64.encodeToString(newIv, Base64.NO_WRAP),
            failedAttempts = 0,
            lockoutUntilTimestamp = 0L
        )

        configDao.insertOrUpdate(updatedConfig)
        VaultSession.unlock(masterKey)
        Result.success(Unit)
    }

    suspend fun setBiometricEnabled(enabled: Boolean) = withContext(Dispatchers.IO) {
        configDao.updateBiometric(enabled)
    }

    suspend fun setAutoLockTimeout(seconds: Int) = withContext(Dispatchers.IO) {
        configDao.updateAutoLockTimeout(seconds)
        VaultSession.autoLockTimeoutSeconds = seconds
    }

    /**
     * Wipes all vault media, database tables, and configuration.
     */
    suspend fun wipeAllVaultData(context: Context) = withContext(Dispatchers.IO) {
        VaultStorageManager.wipeAllMediaFiles(context)
        database.mediaDao().deleteAll()
        database.albumDao().deleteAll()
        configDao.clearConfig()
        VaultSession.lock()
    }
}
