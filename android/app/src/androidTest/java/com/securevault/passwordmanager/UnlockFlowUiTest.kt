package com.securevault.passwordmanager

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.securevault.passwordmanager.core.security.CryptoManager
import com.securevault.passwordmanager.core.timeout.IdleTimeoutTracker
import com.securevault.passwordmanager.data.preferences.AppPreferences
import com.securevault.passwordmanager.ui.theme.SecureVaultTheme
import com.securevault.passwordmanager.ui.unlock.UnlockScreen
import com.securevault.passwordmanager.ui.unlock.UnlockViewModel
import io.mockk.every
import io.mockk.mockk
import org.junit.Rule
import org.junit.Test

class UnlockFlowUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testUnlockScreenDisplaysInputAndBiometricButtonWhenEnabled() {
        val prefs = mockk<AppPreferences>(relaxed = true)
        every { prefs.isBiometricEnabled } returns true

        val crypto = mockk<CryptoManager>(relaxed = true)
        val tracker = IdleTimeoutTracker()
        val viewModel = UnlockViewModel(prefs, crypto, tracker)

        composeTestRule.setContent {
            SecureVaultTheme {
                UnlockScreen(
                    viewModel = viewModel,
                    onTriggerBiometric = {},
                    onUnlockSuccess = {}
                )
            }
        }

        // Verify password field and unlock button
        composeTestRule.onNodeWithTag("unlock_password_input").assertIsDisplayed()
        composeTestRule.onNodeWithTag("unlock_submit_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("unlock_biometric_button").assertIsDisplayed()
    }
}
