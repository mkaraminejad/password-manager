package com.securevault.passwordmanager

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.securevault.passwordmanager.core.security.SecureClipboardManager
import com.securevault.passwordmanager.core.timeout.IdleTimeoutTracker
import com.securevault.passwordmanager.data.preferences.AppPreferences
import com.securevault.passwordmanager.domain.repository.VaultRepository
import com.securevault.passwordmanager.ui.theme.SecureVaultTheme
import com.securevault.passwordmanager.ui.vault.VaultScreen
import com.securevault.passwordmanager.ui.vault.VaultViewModel
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class LockTimeoutUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testExplicitLockVaultButtonTriggersLock() {
        val repo = mockk<VaultRepository>(relaxed = true)
        every { repo.getAllEntries() } returns flowOf(emptyList())

        val clipboard = mockk<SecureClipboardManager>(relaxed = true)
        val prefs = mockk<AppPreferences>(relaxed = true)
        val tracker = IdleTimeoutTracker()
        tracker.unlock()

        val viewModel = VaultViewModel(repo, clipboard, prefs, tracker)

        composeTestRule.setContent {
            SecureVaultTheme {
                VaultScreen(
                    viewModel = viewModel,
                    onNavigateToAddEntry = {},
                    onNavigateToEditEntry = {},
                    onNavigateToAudit = {},
                    onNavigateToSettings = {}
                )
            }
        }

        // Tap Lock Vault icon button
        composeTestRule.onNodeWithTag("vault_lock_now_button").performClick()

        // Tracker must now be locked
        assertTrue("Tracker should be locked after tapping lock button", tracker.isLocked.value)
    }
}
