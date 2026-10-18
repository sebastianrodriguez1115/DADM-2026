package com.edu.unal.directorio.ui.detail

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.edu.unal.directorio.R

const val DELETE_DIALOG_CONFIRM_TAG = "delete_dialog_confirm"
const val DELETE_DIALOG_CANCEL_TAG = "delete_dialog_cancel"

/**
 * Confirmation before deleting [companyName]. Back and a tap outside call [onDismiss], which must
 * only close the dialog; nothing is deleted until [onConfirm].
 */
@Composable
fun DeleteCompanyDialog(
    companyName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.delete_dialog_title)) },
        text = { Text(stringResource(R.string.delete_dialog_message, companyName)) },
        confirmButton = {
            TextButton(onClick = onConfirm, modifier = Modifier.testTag(DELETE_DIALOG_CONFIRM_TAG)) {
                Text(stringResource(R.string.delete_dialog_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.testTag(DELETE_DIALOG_CANCEL_TAG)) {
                Text(stringResource(R.string.delete_dialog_cancel))
            }
        },
    )
}
