package com.securevault.passwordmanager

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.securevault.passwordmanager.core.timeout.IdleTimeoutTracker
import com.securevault.passwordmanager.domain.repository.VaultRepository
import com.securevault.passwordmanager.ui.entry.EntryDetailEditScreen
import com.securevault.passwordmanager.ui.entry.EntryViewModel
import com.securevault.passwordmanager.ui.theme.SecureVaultTheme
import io.mockk.coVerify
import io.mockk.mockk
import org.junit.Rule
import org.junit.Test

class VaultEntryUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testAddEntryValidatesRequiredFieldsAndSaves() {
        val repo = mockk<VaultRepository>(relaxed = true)
        val tracker = IdleTimeoutTracker()
        val viewModel = EntryViewModel(repo, tracker)
        viewModel.loadEntry(null) // New entry

        composeTestRule.setContent {
            SecureVaultTheme {
                EntryDetailEditScreen(
                    viewModel = viewModel,
                    onNavigateBack = {}
                )
            }
        }

        // Initially disabled because title & password are empty
        composeTestRule.onNodeWithTag("entry_save_button").assertIsNotEnabled()

        // Fill fields
        composeTestRule.onNodeWithTag("entry_title_input").performTextInput("ProtonMail")
        composeTestRule.onNodeWithTag("entry_username_input").performTextInput("user@proton.me")
        composeTestRule.onNodeWithTag("entry_password_input").performTextInput("SuperSecret12345!")

        // Save button becomes enabled
        composeTestRule.onNodeWithTag("entry_save_button").assertIsEnabled()
        composeTestRule.onNodeWithTag("entry_save_button").performClick()

        coVerify {
            repo.insertEntry(match { it.title == "ProtonMail" && it.username == "user@proton.me" })
        }
    }
}
