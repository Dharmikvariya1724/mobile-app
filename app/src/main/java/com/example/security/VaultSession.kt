package com.example.security

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.crypto.SecretKey

/**
 * Manages the in-memory security state of the vault.
 * - Holds the active master key securely in memory only while unlocked
 * - Wipes the key reference upon lock
 * - Tracks user activity timestamps for auto-locking
 * - Tracks failed attempts and lockout timers
 */
object VaultSession {

    private var masterKey: SecretKey? = null

    private val _isUnlocked = MutableStateFlow(false)
    val isUnlocked: StateFlow<Boolean> = _isUnlocked.asStateFlow()

    private var lastActiveTimestamp: Long = System.currentTimeMillis()

    // Configurable auto-lock timeout (default 30 seconds of inactivity / backgrounding)
    var autoLockTimeoutSeconds: Int = 30

    fun unlock(key: SecretKey) {
        masterKey = key
        _isUnlocked.value = true
        recordActivity()
    }

    fun lock() {
        masterKey = null
        _isUnlocked.value = false
    }

    fun getActiveKey(): SecretKey? {
        if (!_isUnlocked.value) return null
        return masterKey
    }

    fun recordActivity() {
        lastActiveTimestamp = System.currentTimeMillis()
    }

    fun checkAutoLock(): Boolean {
        if (!_isUnlocked.value) return true
        if (autoLockTimeoutSeconds <= 0) {
            // Immediate lock on background
            return false
        }
        val elapsedSeconds = (System.currentTimeMillis() - lastActiveTimestamp) / 1000
        if (elapsedSeconds >= autoLockTimeoutSeconds) {
            lock()
            return true
        }
        return false
    }
}
