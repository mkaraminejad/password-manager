package com.securevault.passwordmanager.ui.entry

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.securevault.passwordmanager.core.model.VaultEntry
import com.securevault.passwordmanager.core.timeout.IdleTimeoutTracker
import com.securevault.passwordmanager.domain.repository.VaultRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

data class EntryFormState(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "",
    val username: String = "",
    val password: String = "",
    val url: String = "",
    val notes: String = "",
    val folder: String = "",
    val tagsText: String = "",
    val isFavorite: Boolean = false,
    val isNew: Boolean = true,
    val isSaving: Boolean = false,
    val errorMessage: String? = null
)

class EntryViewModel(
    private val repository: VaultRepository,
    private val idleTimeoutTracker: IdleTimeoutTracker
) : ViewModel() {

    private val _formState = MutableStateFlow(EntryFormState())
    val formState: StateFlow<EntryFormState> = _formState.asStateFlow()

    fun loadEntry(entryId: String?) {
        if (entryId.isNullOrBlank()) {
            _formState.value = EntryFormState(isNew = true)
            return
        }

        viewModelScope.launch {
            val entry = repository.getEntryById(entryId)
            if (entry != null) {
                _formState.value = EntryFormState(
                    id = entry.id,
                    title = entry.title,
                    username = entry.username,
                    password = entry.password,
                    url = entry.url,
                    notes = entry.notes,
                    folder = entry.folder,
                    tagsText = entry.tags.joinToString(", "),
                    isFavorite = entry.isFavorite,
                    isNew = false
                )
            }
        }
    }

    fun onTitleChanged(v: String) {
        _formState.value = _formState.value.copy(title = v, errorMessage = null)
        idleTimeoutTracker.onUserActivity()
    }

    fun onUsernameChanged(v: String) {
        _formState.value = _formState.value.copy(username = v)
        idleTimeoutTracker.onUserActivity()
    }

    fun onPasswordChanged(v: String) {
        _formState.value = _formState.value.copy(password = v, errorMessage = null)
        idleTimeoutTracker.onUserActivity()
    }

    fun onUrlChanged(v: String) {
        _formState.value = _formState.value.copy(url = v)
        idleTimeoutTracker.onUserActivity()
    }

    fun onNotesChanged(v: String) {
        _formState.value = _formState.value.copy(notes = v)
        idleTimeoutTracker.onUserActivity()
    }

    fun onFolderChanged(v: String) {
        _formState.value = _formState.value.copy(folder = v)
        idleTimeoutTracker.onUserActivity()
    }

    fun onTagsChanged(v: String) {
        _formState.value = _formState.value.copy(tagsText = v)
        idleTimeoutTracker.onUserActivity()
    }

    fun onToggleFavorite() {
        _formState.value = _formState.value.copy(isFavorite = !_formState.value.isFavorite)
        idleTimeoutTracker.onUserActivity()
    }

    fun saveEntry(onSuccess: () -> Unit) {
        val state = _formState.value
        if (state.title.isBlank()) {
            _formState.value = state.copy(errorMessage = "Title is required")
            return
        }
        if (state.password.isBlank()) {
            _formState.value = state.copy(errorMessage = "Password is required")
            return
        }

        idleTimeoutTracker.onUserActivity()
        _formState.value = state.copy(isSaving = true)

        viewModelScope.launch {
            val tags = state.tagsText.split(",")
                .map { it.trim() }
                .filter { it.isNotBlank() }

            val entry = VaultEntry(
                id = state.id,
                title = state.title.trim(),
                username = state.username.trim(),
                password = state.password,
                url = state.url.trim(),
                notes = state.notes.trim(),
                folder = state.folder.trim(),
                tags = tags,
                isFavorite = state.isFavorite,
                updatedAt = System.currentTimeMillis()
            )

            if (state.isNew) {
                repository.insertEntry(entry)
            } else {
                repository.updateEntry(entry)
            }

            _formState.value = _formState.value.copy(isSaving = false)
            onSuccess()
        }
    }

    fun deleteEntry(onSuccess: () -> Unit) {
        val state = _formState.value
        if (state.isNew) return

        viewModelScope.launch {
            repository.deleteById(state.id)
            onSuccess()
        }
    }
}
