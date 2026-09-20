package com.securevault.passwordmanager.core.timeout

import com.securevault.passwordmanager.core.security.VaultKeyHolder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Monitors user interaction and app state to enforce automatic lock after an idle timeout.
 */
class IdleTimeoutTracker(
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main)
) {

    private var timeoutMinutes: Int = 5
    private var lockJob: Job? = null

    private val _isLocked = MutableStateFlow(true)
    val isLocked: StateFlow<Boolean> = _isLocked.asStateFlow()

    fun updateTimeoutSetting(minutes: Int) {
        timeoutMinutes = minutes
        if (!_isLocked.value) {
            resetTimer()
        }
    }

    fun onUserActivity() {
        if (!_isLocked.value) {
            resetTimer()
        }
    }

    fun onAppForeground() {
        // Kept for lifecycle hook
    }

    fun onAppBackground() {
        // If timeout is 0 (Immediate lock on exit), lock right away
        if (timeoutMinutes == 0) {
            lockVault()
        }
    }

    fun unlock() {
        _isLocked.value = false
        resetTimer()
    }

    fun lockVault() {
        lockJob?.cancel()
        VaultKeyHolder.clearKey()
        _isLocked.value = true
    }

    private fun resetTimer() {
        lockJob?.cancel()
        if (timeoutMinutes > 0) {
            lockJob = scope.launch {
                delay(timeoutMinutes * 60 * 1000L)
                lockVault()
            }
        }
    }
}
