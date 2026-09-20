package com.securevault.passwordmanager.ui.generator

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.securevault.passwordmanager.core.security.PasswordGenerator

enum class GeneratorMode {
    RANDOM_CHARACTERS,
    PASSPHRASE
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PasswordGeneratorSheet(
    onPasswordGenerated: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var mode by remember { mutableStateOf(GeneratorMode.RANDOM_CHARACTERS) }

    // Random Characters Config
    var length by remember { mutableFloatStateOf(20f) }
    var incUpper by remember { mutableStateOf(true) }
    var incLower by remember { mutableStateOf(true) }
    var incNumbers by remember { mutableStateOf(true) }
    var incSymbols by remember { mutableStateOf(true) }
    var avoidAmbiguous by remember { mutableStateOf(false) }

    // Passphrase Config
    var wordCount by remember { mutableFloatStateOf(4f) }
    var separator by remember { mutableStateOf("-") }
    var capitalize by remember { mutableStateOf(true) }
    var incPassphraseNumber by remember { mutableStateOf(true) }

    var currentPassword by remember { mutableStateOf("") }

    fun refreshPassword() {
        currentPassword = when (mode) {
            GeneratorMode.RANDOM_CHARACTERS -> {
                PasswordGenerator.generatePassword(
                    PasswordGenerator.GeneratorConfig(
                        length = length.toInt(),
                        includeUppercase = incUpper,
                        includeLowercase = incLower,
                        includeNumbers = incNumbers,
                        includeSymbols = incSymbols,
                        excludeAmbiguous = avoidAmbiguous
                    )
                )
            }
            GeneratorMode.PASSPHRASE -> {
                PasswordGenerator.generatePassphrase(
                    PasswordGenerator.PassphraseConfig(
                        wordCount = wordCount.toInt(),
                        separator = separator,
                        capitalizeWords = capitalize,
                        includeNumber = incPassphraseNumber
                    )
                )
            }
        }
    }

    LaunchedEffect(mode, length, incUpper, incLower, incNumbers, incSymbols, avoidAmbiguous, wordCount, separator, capitalize, incPassphraseNumber) {
        refreshPassword()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Password Generator",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            // Mode Toggle
            TabRow(selectedTabIndex = mode.ordinal) {
                Tab(
                    selected = mode == GeneratorMode.RANDOM_CHARACTERS,
                    onClick = { mode = GeneratorMode.RANDOM_CHARACTERS },
                    text = { Text("Random Chars") }
                )
                Tab(
                    selected = mode == GeneratorMode.PASSPHRASE,
                    onClick = { mode = GeneratorMode.PASSPHRASE },
                    text = { Text("Passphrase") }
                )
            }

            // Output Display Card
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = currentPassword,
                        style = MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.Monospace),
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { refreshPassword() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Regenerate")
                    }
                }
            }

            if (mode == GeneratorMode.RANDOM_CHARACTERS) {
                // Length Slider
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Length: ${length.toInt()}")
                    }
                    Slider(
                        value = length,
                        onValueChange = { length = it },
                        valueRange = 8f..48f,
                        steps = 39
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Uppercase (A-Z)")
                    Switch(checked = incUpper, onCheckedChange = { incUpper = it })
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Lowercase (a-z)")
                    Switch(checked = incLower, onCheckedChange = { incLower = it })
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Numbers (0-9)")
                    Switch(checked = incNumbers, onCheckedChange = { incNumbers = it })
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Symbols (!@#$)")
                    Switch(checked = incSymbols, onCheckedChange = { incSymbols = it })
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Exclude Ambiguous (0, O, 1, l, I)")
                    Switch(checked = avoidAmbiguous, onCheckedChange = { avoidAmbiguous = it })
                }
            } else {
                // Passphrase settings
                Column {
                    Text("Words: ${wordCount.toInt()}")
                    Slider(
                        value = wordCount,
                        onValueChange = { wordCount = it },
                        valueRange = 3f..8f,
                        steps = 4
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Capitalize Words")
                    Switch(checked = capitalize, onCheckedChange = { capitalize = it })
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Include Number")
                    Switch(checked = incPassphraseNumber, onCheckedChange = { incPassphraseNumber = it })
                }
            }

            Button(
                onClick = {
                    onPasswordGenerated(currentPassword)
                    onDismiss()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("generator_use_password_button")
            ) {
                Text("Use This Password", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
