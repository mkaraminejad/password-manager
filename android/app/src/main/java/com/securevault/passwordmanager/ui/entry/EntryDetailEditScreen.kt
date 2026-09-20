package com.securevault.passwordmanager.ui.entry

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.securevault.passwordmanager.core.security.PasswordStrengthAnalyzer
import com.securevault.passwordmanager.ui.generator.PasswordGeneratorSheet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntryDetailEditScreen(
    viewModel: EntryViewModel,
    onNavigateBack: () -> Unit
) {
    val formState by viewModel.formState.collectAsState()
    var showPassword by remember { mutableStateOf(false) }
    var showGenerator by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    val strength = remember(formState.password) {
        PasswordStrengthAnalyzer.evaluate(formState.password)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (formState.isNew) "Add Credential" else "Edit Credential", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.onToggleFavorite() }) {
                        Icon(
                            imageVector = if (formState.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = "Favorite",
                            tint = if (formState.isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                        )
                    }
                    if (!formState.isNew) {
                        IconButton(onClick = { showDeleteConfirmation = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // Title
            OutlinedTextField(
                value = formState.title,
                onValueChange = { viewModel.onTitleChanged(it) },
                label = { Text("Title *") },
                placeholder = { Text("e.g. Google, GitHub, Bank") },
                leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("entry_title_input")
            )

            // Username / Email
            OutlinedTextField(
                value = formState.username,
                onValueChange = { viewModel.onUsernameChanged(it) },
                label = { Text("Username or Email") },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("entry_username_input")
            )

            // Password with generator and strength
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = formState.password,
                    onValueChange = { viewModel.onPasswordChanged(it) },
                    label = { Text("Password *") },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                    trailingIcon = {
                        Row {
                            IconButton(onClick = { showGenerator = true }) {
                                Icon(Icons.Default.AutoFixHigh, contentDescription = "Generate password")
                            }
                            IconButton(onClick = { showPassword = !showPassword }) {
                                Icon(
                                    imageVector = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = "Toggle password"
                                )
                            }
                        }
                    },
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("entry_password_input")
                )

                if (formState.password.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Strength: ${strength.label}",
                            style = MaterialTheme.typography.labelSmall
                        )
                        Text(
                            text = "${formState.password.length} chars",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                    LinearProgressIndicator(
                        progress = { strength.score / 5f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp),
                        color = when (strength.score) {
                            1, 2 -> MaterialTheme.colorScheme.error
                            3 -> MaterialTheme.colorScheme.tertiary
                            else -> MaterialTheme.colorScheme.secondary
                        }
                    )
                }
            }

            // Website / URL
            OutlinedTextField(
                value = formState.url,
                onValueChange = { viewModel.onUrlChanged(it) },
                label = { Text("Website or App URL") },
                placeholder = { Text("https://example.com") },
                leadingIcon = { Icon(Icons.Default.Language, contentDescription = null) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Next),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("entry_url_input")
            )

            // Folder
            OutlinedTextField(
                value = formState.folder,
                onValueChange = { viewModel.onFolderChanged(it) },
                label = { Text("Folder (optional)") },
                placeholder = { Text("e.g. Work, Personal, Finance") },
                leadingIcon = { Icon(Icons.Default.Folder, contentDescription = null) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("entry_folder_input")
            )

            // Tags
            OutlinedTextField(
                value = formState.tagsText,
                onValueChange = { viewModel.onTagsChanged(it) },
                label = { Text("Tags (comma separated)") },
                placeholder = { Text("security, 2fa, personal") },
                leadingIcon = { Icon(Icons.Default.Tag, contentDescription = null) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("entry_tags_input")
            )

            // Notes
            OutlinedTextField(
                value = formState.notes,
                onValueChange = { viewModel.onNotesChanged(it) },
                label = { Text("Notes (encrypted)") },
                placeholder = { Text("Additional confidential details...") },
                leadingIcon = { Icon(Icons.Default.Notes, contentDescription = null) },
                minLines = 3,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("entry_notes_input")
            )

            formState.errorMessage?.let { error ->
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = { viewModel.saveEntry(onNavigateBack) },
                enabled = !formState.isSaving && formState.title.isNotBlank() && formState.password.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("entry_save_button")
            ) {
                if (formState.isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text(if (formState.isNew) "Save Credential" else "Update Credential", fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        if (showGenerator) {
            PasswordGeneratorSheet(
                onPasswordGenerated = { generated ->
                    viewModel.onPasswordChanged(generated)
                },
                onDismiss = { showGenerator = false }
            )
        }

        if (showDeleteConfirmation) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirmation = false },
                title = { Text("Delete Credential?") },
                text = { Text("Are you sure you want to permanently delete '${formState.title}'? This action cannot be undone.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showDeleteConfirmation = false
                            viewModel.deleteEntry(onNavigateBack)
                        }
                    ) {
                        Text("Delete", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirmation = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}
