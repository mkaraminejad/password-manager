package com.securevault.passwordmanager.core.security

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Arrays

/**
 * Thread-safe transient in-memory holder for the decrypted vault key.
 * This key is never persisted to disk. When the vault is locked (manually, on idle,
 * or backgrounded), this key is explicitly wiped with zeros.
 */
object VaultKeyHolder {

    private var activeKey: ByteArray? = null
    private val _isUnlocked = MutableStateFlow(false)
    val isUnlocked: StateFlow<Boolean> = _isUnlocked.asStateFlow()

    @Synchronized
    fun setKey(key: ByteArray) {
        // Clear any old key first
        clearKey()
        activeKey = key.copyOf()
        _isUnlocked.value = true
    }

    @Synchronized
    fun getKey(): ByteArray? {
        return activeKey?.copyOf()
    }

    @Synchronized
    fun hasKey(): Boolean {
        return activeKey != null && activeKey!!.isNotEmpty()
    }

    @Synchronized
    fun clearKey() {
        activeKey?.let {
            Arrays.fill(it, 0.toByte())
        }
        activeKey = null
        _isUnlocked.value = false
    }
}
