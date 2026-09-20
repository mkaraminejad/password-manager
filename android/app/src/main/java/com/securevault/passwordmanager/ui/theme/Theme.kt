package com.securevault.passwordmanager.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Sky500,
    onPrimary = Color.White,
    secondary = Emerald500,
    background = Slate900,
    surface = Slate800,
    onBackground = Slate50,
    onSurface = Slate100,
    surfaceVariant = Slate700,
    onSurfaceVariant = Slate300
)

private val LightColorScheme = lightColorScheme(
    primary = Slate800,
    onPrimary = Color.White,
    secondary = Sky600,
    background = Slate50,
    surface = Color.White,
    onBackground = Slate900,
    onSurface = Slate800,
    surfaceVariant = Slate100,
    onSurfaceVariant = Slate600
)

@Composable
fun SecureVaultTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
