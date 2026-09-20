package com.securevault.passwordmanager.core.security

import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.PersistableBundle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Handles copying credentials to the Android system clipboard securely:
 * 1. Sets ClipDescription.EXTRA_IS_SENSITIVE for Android 13+ to avoid OS clipboard overlay previews.
 * 2. Launches an automatic timer job (e.g. 30 seconds) to clear clipboard content.
 */
class SecureClipboardManager(private val context: Context) {

    private val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    private val scope = CoroutineScope(Dispatchers.Main)
    private var clearJob: Job? = null
    private var lastCopiedText: String? = null

    fun copyToClipboard(label: String, text: String, autoClearSeconds: Int = 30) {
        clearJob?.cancel()
        lastCopiedText = text

        val clip = ClipData.newPlainText(label, text)

        // Android 13+ (API 33) Privacy Flag: Do not show sensitive passwords in system clipboard preview
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            clip.description.extras = PersistableBundle().apply {
                putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true)
            }
        }

        clipboard.setPrimaryClip(clip)

        if (autoClearSeconds > 0) {
            clearJob = scope.launch {
                delay(autoClearSeconds * 1000L)
                clearIfMatching(text)
            }
        }
    }

    private fun clearIfMatching(originalText: String) {
        val currentClip = clipboard.primaryClip
        if (currentClip != null && currentClip.itemCount > 0) {
            val text = currentClip.getItemAt(0).text?.toString()
            if (text == originalText || text == lastCopiedText) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    clipboard.clearPrimaryClip()
                } else {
                    clipboard.setPrimaryClip(ClipData.newPlainText("", ""))
                }
                lastCopiedText = null
            }
        }
    }
}
