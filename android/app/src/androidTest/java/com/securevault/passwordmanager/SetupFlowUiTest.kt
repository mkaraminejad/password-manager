package com.securevault.passwordmanager

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.securevault.passwordmanager.core.security.CryptoManager
import com.securevault.passwordmanager.data.preferences.AppPreferences
import com.securevault.passwordmanager.ui.setup.SetupScreen
import com.securevault.passwordmanager.ui.setup.SetupViewModel
import com.securevault.passwordmanager.ui.theme.SecureVaultTheme
import io.mockk.mockk
import org.junit.Rule
import org.junit.Test

class SetupFlowUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testSetupEnforcesMinimum12CharactersForMasterPassword() {
        val prefs = mockk<AppPreferences>(relaxed = true)
        val crypto = mockk<CryptoManager>(relaxed = true)
        val viewModel = SetupViewModel(prefs, crypto)

        composeTestRule.setContent {
            SecureVaultTheme {
                SetupScreen(viewModel = viewModel, onSetupComplete = {})
            }
        }

        // Initially submit button should be disabled
        composeTestRule.onNodeWithTag("setup_submit_button").assertIsNotEnabled()

        // Type short password
        composeTestRule.onNodeWithTag("setup_master_password_input").performTextInput("short123")
        composeTestRule.onNodeWithTag("setup_confirm_password_input").performTextInput("short123")

        // Still disabled because length < 12
        composeTestRule.onNodeWithTag("setup_submit_button").assertIsNotEnabled()

        // Type valid 12+ character password
        composeTestRule.onNodeWithTag("setup_master_password_input").performTextClearance()
        composeTestRule.onNodeWithTag("setup_confirm_password_input").performTextClearance()
        composeTestRule.onNodeWithTag("setup_master_password_input").performTextInput("MasterPassword2026!#")
        composeTestRule.onNodeWithTag("setup_confirm_password_input").performTextInput("MasterPassword2026!#")

        // Submit button should become enabled
        composeTestRule.onNodeWithTag("setup_submit_button").assertIsEnabled()
    }
}
