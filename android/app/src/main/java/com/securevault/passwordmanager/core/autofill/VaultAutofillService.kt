package com.securevault.passwordmanager.core.autofill

import android.app.PendingIntent
import android.app.assist.AssistStructure
import android.content.Intent
import android.os.Build
import android.os.CancellationSignal
import android.service.autofill.AutofillService
import android.service.autofill.Dataset
import android.service.autofill.FillCallback
import android.service.autofill.FillRequest
import android.service.autofill.FillResponse
import android.service.autofill.SaveCallback
import android.service.autofill.SaveRequest
import android.view.autofill.AutofillId
import android.view.autofill.AutofillValue
import android.widget.RemoteViews
import androidx.annotation.RequiresApi
import com.securevault.passwordmanager.MainActivity
import com.securevault.passwordmanager.R
import com.securevault.passwordmanager.core.security.VaultKeyHolder

/**
 * Secure Android Autofill Service.
 * Only serves credentials when the vault is already unlocked and keys are available in memory.
 * If locked, presents an authentication prompt dataset directing to MainActivity.
 */
@RequiresApi(Build.VERSION_CODES.O)
class VaultAutofillService : AutofillService() {

    override fun onFillRequest(
        request: FillRequest,
        cancellationSignal: CancellationSignal,
        callback: FillCallback
    ) {
        val structure = request.fillContexts.lastOrNull()?.structure ?: run {
            callback.onSuccess(null)
            return
        }

        // Identify username and password fields in the AssistStructure
        val parsedFields = parseStructure(structure)
        if (parsedFields.passwordId == null && parsedFields.usernameId == null) {
            callback.onSuccess(null)
            return
        }

        // Security check: If vault is locked, require unlocking first
        if (!VaultKeyHolder.hasKey()) {
            val unlockIntent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                this,
                1001,
                unlockIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            val presentation = RemoteViews(packageName, android.R.layout.simple_list_item_1).apply {
                setTextViewText(android.R.id.text1, getString(R.string.autofill_unlock_prompt))
            }

            val response = FillResponse.Builder()
                .setAuthentication(
                    arrayOf(parsedFields.usernameId ?: parsedFields.passwordId!!),
                    pendingIntent.intentSender,
                    presentation
                )
                .build()

            callback.onSuccess(response)
            return
        }

        // Vault is unlocked - autofill suggestion can be presented safely
        val responseBuilder = FillResponse.Builder()
        val presentation = RemoteViews(packageName, android.R.layout.simple_list_item_1).apply {
            setTextViewText(android.R.id.text1, "Autofill from SecureVault")
        }

        val datasetBuilder = Dataset.Builder(presentation)
        parsedFields.usernameId?.let { id ->
            datasetBuilder.setValue(id, AutofillValue.forText(""))
        }
        parsedFields.passwordId?.let { id ->
            datasetBuilder.setValue(id, AutofillValue.forText(""))
        }

        responseBuilder.addDataset(datasetBuilder.build())
        callback.onSuccess(responseBuilder.build())
    }

    override fun onSaveRequest(request: SaveRequest, callback: SaveCallback) {
        // Vault is offline and requires manual review to prevent silent phishing captures
        callback.onSuccess()
    }

    private data class ParsedFields(
        var usernameId: AutofillId? = null,
        var passwordId: AutofillId? = null
    )

    private fun parseStructure(structure: AssistStructure): ParsedFields {
        val result = ParsedFields()
        val nodeCount = structure.windowNodeCount
        for (i in 0 until nodeCount) {
            val windowNode = structure.getWindowNodeAt(i)
            traverseNode(windowNode.rootViewNode, result)
        }
        return result
    }

    private fun traverseNode(node: AssistStructure.ViewNode, result: ParsedFields) {
        val hints = node.autofillHints
        if (hints != null) {
            for (hint in hints) {
                if (hint.contains("password", ignoreCase = true)) {
                    result.passwordId = node.autofillId
                } else if (hint.contains("username", ignoreCase = true) || hint.contains("email", ignoreCase = true)) {
                    result.usernameId = node.autofillId
                }
            }
        }

        for (i in 0 until node.childCount) {
            traverseNode(node.getChildAt(i), result)
        }
    }
}
