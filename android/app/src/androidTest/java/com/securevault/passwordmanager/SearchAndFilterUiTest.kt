package com.securevault.passwordmanager

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.securevault.passwordmanager.core.model.VaultEntry
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
import org.junit.Rule
import org.junit.Test

class SearchAndFilterUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testSearchFiltersEntriesInVaultList() {
        val entry1 = VaultEntry(id = "1", title = "Amazon AWS", username = "dev@cloud.com", password = "pass1")
        val entry2 = VaultEntry(id = "2", title = "Spotify Music", username = "listener@audio.com", password = "pass2")

        val repo = mockk<VaultRepository>(relaxed = true)
        every { repo.getAllEntries() } returns flowOf(listOf(entry1, entry2))

        val clipboard = mockk<SecureClipboardManager>(relaxed = true)
        val prefs = mockk<AppPreferences>(relaxed = true)
        val tracker = IdleTimeoutTracker()

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

        // Both items displayed initially
        composeTestRule.onNodeWithText("Amazon AWS").assertIsDisplayed()
        composeTestRule.onNodeWithText("Spotify Music").assertIsDisplayed()

        // Type search filter
        composeTestRule.onNodeWithTag("vault_search_input").performTextInput("Spotify")

        // Only Spotify should remain
        composeTestRule.onNodeWithText("Spotify Music").assertIsDisplayed()
        composeTestRule.onNodeWithText("Amazon AWS").assertDoesNotExist()
    }
}
