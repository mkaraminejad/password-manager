package com.securevault.passwordmanager

import com.securevault.passwordmanager.core.security.VaultKeyHolder
import com.securevault.passwordmanager.core.timeout.IdleTimeoutTracker
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class IdleTimeoutTrackerTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)
    private lateinit var tracker: IdleTimeoutTracker

    @Before
    fun setup() {
        tracker = IdleTimeoutTracker(testScope)
    }

    @Test
    fun testUnlockSetsIsLockedFalse() {
        assertTrue(tracker.isLocked.value)
        tracker.unlock()
        assertFalse(tracker.isLocked.value)
    }

    @Test
    fun testLockVaultZeroizesVaultKey() {
        val dummyKey = byteArrayOf(1, 2, 3, 4)
        VaultKeyHolder.setKey(dummyKey)
        assertTrue(VaultKeyHolder.hasKey())

        tracker.lockVault()

        assertTrue(tracker.isLocked.value)
        assertFalse(VaultKeyHolder.hasKey())
        assertNull(VaultKeyHolder.getKey())
    }

    @Test
    fun testIdleTimeoutTriggersAutomaticLock() = runTest(testDispatcher) {
        val key = byteArrayOf(9, 9, 9, 9)
        VaultKeyHolder.setKey(key)

        tracker.updateTimeoutSetting(1) // 1 minute
        tracker.unlock()
        assertFalse(tracker.isLocked.value)

        // Advance 30 seconds: still unlocked
        advanceTimeBy(30 * 1000L)
        assertFalse(tracker.isLocked.value)

        // Advance remaining 35 seconds: timeout passed, vault should lock
        advanceTimeBy(35 * 1000L)
        assertTrue("Vault must automatically lock after timeout", tracker.isLocked.value)
        assertFalse("Key must be cleared on auto-lock", VaultKeyHolder.hasKey())
    }
}
